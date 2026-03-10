# Web Filter Implementation

The Web Filter adds DNS-level blocking and optional URL-bar capture for parental control. Enforcement is done by a **local VPN**; URL bar text is read only for classification and visit logging. There is no HTTPS inspection and no page content reading.

---

## 1. Architecture Overview

```
Browser / any app
       │
       │  DNS query (UDP port 53)
       ▼
PauseVpnService (TUN interface)
       │
       ├─ WhitelistMatcher.isWhitelisted(domain) → true  → forward to upstream DNS
       ├─ BlocklistMatcher.isBlocked(domain)    → true  → return NXDOMAIN
       └─ else                                   → forward to upstream DNS

In parallel (when URL capture is enabled):
PauseAccessibilityService (TYPE_WINDOW_CONTENT_CHANGED)
       │
       ▼  BrowserURLReader.extractURL(rootNode, packageName)
       ▼  URLClassifier.classify(url, domain)
       ▼  URLCaptureQueue.enqueue(...)  → UrlVisitLogRepository
       ▼  Auto-blacklist on keyword match (if configured)
```

- **VPN** — Single point of enforcement for all apps and browsers; blocks at domain level.
- **URL reader** — Only used to capture visited URLs for the parent’s log and to trigger keyword-based auto-blacklist; the VPN then blocks those domains on subsequent DNS lookups.

---

## 2. PauseVpnService

- **Extends** `VpnService`. Started via explicit intent with action `ACTION_START` or `ACTION_STOP` (e.g. from Parent Dashboard when Web Filter is toggled).
- **Establishment:** Uses `VpnService.Builder`: address `10.0.0.1/32`, default route `0.0.0.0/0`, DNS server `10.0.0.1`, MTU 1500. No `addDisallowedApplication` in the snippet; the PRD suggests excluding the Pause app to avoid recursive filtering — confirm in actual code if present.
- **Foreground:** Runs as a foreground service with a notification (“Web Filter active”) so the system is less likely to kill it.
- **DNS loop:** Runs on a coroutine; reads from `vpnInterface.fileDescriptor` into a buffer. Each packet is parsed with `DNSPacketParser.parseQuery()`. The queried domain is then:
  1. Checked against **WhitelistMatcher** — if whitelisted, the packet is forwarded to upstream DNS and the response is returned (conceptually; the current loop uses `resolveUpstream` for non-blocked).
  2. Checked against **BlocklistMatcher** — if blocked, the response is `DNSPacketParser.buildNXDomainResponse(query)` (NXDOMAIN).
  3. Otherwise forwarded to upstream DNS (`resolveUpstream(upstream, packet)`).

Upstream DNS is taken from `WebFilterConfigRepository.getConfig().upstreamDns` (default `8.8.8.8`). VPN is only run when `config.vpnEnabled` is true.

**Note:** The current implementation returns NXDOMAIN for blocked domains. The PRD also describes a local HTTP server on `127.0.0.1:8080` to serve a block page; that would require building a redirect response (e.g. `buildRedirectResponse(query, "127.0.0.1")`) and a separate `LocalHTTPServer`. The codebase may contain or plan that; enforcement today is NXDOMAIN-only.

---

## 3. DNSPacketParser

- **parseQuery(packet)** — Reads transaction ID, flags, and question count; parses the question section to extract the domain name (label sequence). Returns `DNSQuery(transactionId, question, rawPacket)` or null.
- **buildNXDomainResponse(query)** — Builds a DNS response with the same transaction ID and question, response code NXDOMAIN (0x8183), 0 answers.
- **buildRedirectResponse(query, ip)** — Builds an A record response pointing the queried name to the given IPv4 address (e.g. for a future block page at 127.0.0.1).

---

## 4. BlocklistMatcher and WhitelistMatcher

- **BlocklistMatcher** — Holds an in-memory set of domains loaded from `BlacklistRepository.getActiveDomainsAsList()`. `isBlocked(domain)` normalizes (lowercase, strip `www.`), checks exact match and wildcard match (`*.domain.com`). Reloads from DB if the set is empty or older than 60 seconds.
- **WhitelistMatcher** — Same pattern using `WhitelistRepository.getAllAsList()`. Whitelist is checked **before** blacklist in the VPN loop so whitelisted domains always resolve.

Both are singletons provided by Hilt and used by `PauseVpnService` via `VpnEntryPoint`.

---

## 5. URL Capture (Accessibility Side)

### 5.1 BrowserURLReader

- **isKnownBrowser(packageName)** — True for a fixed set of packages (Chrome, Firefox, Brave, Samsung Internet, Edge, DuckDuckGo, Opera, Firefox Focus). Only these trigger URL extraction.
- **extractURL(rootNode, packageName)** — First tries to find the URL bar by **view ID** (e.g. `com.android.chrome:id/url_bar`). If not found, falls back to **findUrlInEditTexts**: any `EditText` whose text looks like a URL (starts with `http://`/`https://` or matches a URL-like pattern). Returns the trimmed URL string or null.

Browser view IDs can change with updates; the fallback reduces fragility.

### 5.2 URL Classification and URLCaptureQueue

- In `handleWindowContentChanged`, when the app is a known browser and URL capture is enabled, the raw URL is normalized and the domain is extracted (`URLNormalizer.extractDomain`). **URLClassifier.classify(rawUrl, domain)** returns a classification (e.g. CLEAN, KEYWORD_MATCH, BLACKLISTED).
- **URLCaptureQueue.enqueue(url, domain, browserPackage, classification, wasBlocked)**:
  - Deduplicates by `domain:browserPackage` within a short window (e.g. 5 seconds) to avoid log spam.
  - Appends a `UrlVisitLog` (fullUrl truncated to 500 chars) to an in-memory list.
  - When the list size reaches a threshold (e.g. 10), it **flush**es: inserts all pending entries into `UrlVisitLogRepository` and clears the list.

So URL visit log and auto-blacklist (via URLClassifier/AutoBlacklistEngine) are decoupled from the VPN packet path; the VPN only sees DNS and the in-memory block/whitelist sets.

---

## 6. Block Page and Unblock Request

- **Block page:** When the VPN returns NXDOMAIN, the browser shows its own “site can’t be reached” / “ERR_NAME_NOT_RESOLVED” page. If a local block page is implemented (PRD: `127.0.0.1:8080`), the VPN would return an A record for 127.0.0.1 and a local HTTP server would serve HTML (e.g. `block_page.html`) with a “Request Review” button.
- **Unblock request:** The block page (or app) can open `pause://unblock-request?domain={domain}`. The app’s deep link opens **UnblockRequestScreen** with that domain; the child can add a note and submit. The app creates a **PendingReview** and notifies the parent. Parent can approve (e.g. add domain to whitelist) or deny from the Parent Dashboard.

---

## 7. Configuration

- **WebFilterConfig** (singleton row in Room, or similar) holds: `vpnEnabled`, `urlReaderEnabled`, `keywordFilterEnabled`, `autoBlacklistOnKeywordMatch`, `notifyParentOnAutoBlock`, upstream DNS, etc.
- **WebFilterConfigRepository** provides this config to the VPN and to the Accessibility Service so URL capture and VPN can be toggled independently.

---

## 8. Boot and Lifecycle

- **BootReceiver** can re-start `PauseVpnService` after device boot if Web Filter was active before shutdown (see BootEntryPoint and receiver implementation).
- **onRevoke** — When the user revokes VPN permission, `PauseVpnService.onRevoke()` stops the VPN and releases the TUN interface.

For full feature specs, data flows, and sequence diagrams, see [PRDv3.md](PRDv3.md). For repository and DB schema, see [data-layer.md](data-layer.md).
