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
- **Establishment:** Uses `VpnService.Builder`: address `10.0.0.1/32`, default route `0.0.0.0/0`, DNS server `10.0.0.1`, MTU 1500. **Excludes the Pause app** via `addDisallowedApplication(packageName)` to avoid recursive filtering.
- **Foreground:** Runs as a foreground service with a notification (“Web filtering active”) so the system is less likely to kill it.
- **DNS loop:** Runs on a coroutine; reads and writes via `FileInputStream` and `FileOutputStream` on the same `vpnInterface.fileDescriptor`. TUN delivers **IPv4 packets**; the loop uses `DNSPacketParser.extractDnsInfo(packet)` to locate the UDP DNS payload, then `parseQuery(dnsPayload)` to get the queried domain. The decision:
  1. **WhitelistMatcher.isWhitelisted(domain)** → forward to upstream DNS; wrap the response with `wrapResponse()` and write back.
  2. **BlocklistMatcher.isBlocked(domain)** → build NXDOMAIN with `buildNXDomainResponse(query)`, wrap, and write back.
  3. Otherwise → forward to upstream DNS, wrap, and write back.

Upstream DNS is from `WebFilterConfig.upstreamDns` (default `8.8.8.8`). Config is re-checked every 100 packets; if `vpnEnabled` becomes false, the loop stops. VPN only runs when `config.vpnEnabled` is true.

**Block page:** The current implementation returns NXDOMAIN for blocked domains; the browser shows its own “site can’t be reached” page. A `block_page.html` asset exists for future use (e.g. a local HTTP server); it is not currently served. `buildRedirectResponse()` exists for a potential 127.0.0.1 redirect path.

---

## 3. DNSPacketParser

- **extractDnsInfo(ipPacket)** — Parses an IPv4 packet; returns `IpUdpInfo` (src/dst IP, src port, DNS offset, DNS length) if it is a UDP packet to port 53. IPv6 packets are ignored.
- **wrapResponse(dnsResponse, info)** — Wraps a bare DNS response in IPv4+UDP headers, swapping src/dst for the reply, for writing back to the TUN interface.
- **parseQuery(packet)** — Parses the DNS payload; reads transaction ID, flags, question count; parses the question section (including compression pointers) to extract the domain. Returns `DNSQuery(transactionId, question, rawPacket)` or null.
- **buildNXDomainResponse(query)** — Builds a DNS response with the same transaction ID and question, response code NXDOMAIN (0x8183), 0 answers.
- **buildRedirectResponse(query, ip)** — Builds an A record response pointing the queried name to the given IPv4 address (e.g. for a future block page at 127.0.0.1).

---

## 4. BlocklistMatcher and WhitelistMatcher

- **BlocklistMatcher** — Holds an in-memory `snapshot: Set<String>?` (with `Mutex`) loaded from `BlacklistRepository.getActiveDomainsAsList()`. `isBlocked(domain)` normalizes (lowercase, strip `www.`), checks exact match and wildcard match (`*.domain.com`). Reloads from DB if the snapshot is null or older than 60 seconds. `reloadFromDB()` is also called by `AutoBlacklistEngine` when a keyword match adds a domain.
- **WhitelistMatcher** — Same pattern using `WhitelistRepository.getAllAsList()`; supports exact and wildcard (`*.domain`) matches. Whitelist is checked **before** blacklist in the VPN loop so whitelisted domains always resolve.

Both are singletons provided by Hilt and used by `PauseVpnService` via `VpnEntryPoint`.

---

## 5. URL Capture (Accessibility Side)

### 5.1 BrowserURLReader

- **isKnownBrowser(packageName)** — True for: `com.android.chrome`, `org.mozilla.firefox`, `org.mozilla.fennec_aurora`, `com.brave.browser`, `com.sec.android.app.sbrowser`, `com.microsoft.emmx`, `com.duckduckgo.mobile.android`, `com.opera.browser`, `org.mozilla.focus`. Only these trigger URL extraction.
- **extractURL(rootNode, packageName)** — First tries to find the URL bar by **view ID** (e.g. `com.android.chrome:id/url_bar`). If not found, falls back to **findUrlInEditTexts**: any `EditText` whose text looks like a URL (starts with `http://`/`https://` or matches a URL-like pattern). Returns the trimmed URL string or null.

Browser view IDs can change with updates; the EditText fallback reduces fragility.

### 5.2 URL Classification, AutoBlacklistEngine, and URLCaptureQueue

- In `handleWindowContentChanged`, when the app is a known browser and URL capture is enabled, the raw URL is normalized and the domain is extracted (`URLNormalizer.extractDomain`). **URLClassifier.classify(rawUrl, domain)** returns `Pair<URLClassification, KeywordMatch?>` (CLEAN, KEYWORD_MATCH, BLACKLISTED, WHITELISTED). Classification uses whitelist, blacklist, and `KeywordMatcher` (domain, path, query).
- **URLCaptureQueue.enqueue(...)** — Deduplicates by `domain:browserPackage` within 5 seconds; appends `UrlVisitLog` (fullUrl truncated to 500 chars) to a pending list; flushes to `UrlVisitLogRepository` when the list reaches 10 entries.
- **AutoBlacklistEngine.onKeywordMatch(domain, keyword, url)** — Called when classification is `KEYWORD_MATCH`. If `autoBlacklistOnKeywordMatch` is enabled in config, adds the domain to `BlacklistRepository` (source `AUTO_KEYWORD`), calls `BlocklistMatcher.reloadFromDB()`, and inserts a `PendingReview` record. The VPN will block the domain on subsequent DNS lookups.

URL visit log and auto-blacklist are decoupled from the VPN packet path; the VPN only sees DNS and the in-memory block/whitelist sets.

---

## 6. Block Page and Unblock Request

- **Block page:** The VPN returns NXDOMAIN for blocked domains; the browser shows its own “site can’t be reached” / “ERR_NAME_NOT_RESOLVED” page. A `block_page.html` asset exists (`app/src/main/assets/block_page.html`) with “This site is blocked by Pause” and a “Request Review” link that deep-links to `pause://unblock-request?domain={domain}`. This asset is **not currently served** by a local HTTP server; a future implementation could use `buildRedirectResponse()` and a LocalHTTPServer to show the block page for HTTP requests.
- **Unblock request:** The app’s deep link `pause://unblock-request?domain={domain}` opens **UnblockRequestScreen** with that domain. The child can add a note and submit; the app creates a **PendingReview**. The parent can approve (e.g. add domain to whitelist) or deny from the Parent Dashboard.

---

## 7. Configuration

- **WebFilterConfig** (Room entity, singleton row `id=1`) holds: `vpnEnabled`, `urlReaderEnabled`, `keywordFilterEnabled`, `autoBlacklistOnKeywordMatch`, `notifyParentOnAutoBlock`, `safeSearchEnforcement`, `youtubeRestrictedMode`, `blockIncognito`, `dailyBrowsingBudgetMinutes`, `upstreamDns` (default `8.8.8.8`).
- **WebFilterConfigRepository** provides this config to the VPN and to the Accessibility Service so URL capture and VPN can be toggled independently.

---

## 8. Boot and Lifecycle

- **BootReceiver** — Handles `ACTION_BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED`. Uses `BootEntryPoint` to: (1) resume Strict session via `StrictSessionManager.resumeSessionOnBootSync()`, (2) resume Parental Control via `ParentalControlManager.resumeOnBootSync()`, (3) start `PauseVpnService` with `ACTION_START` if `WebFilterConfig.vpnEnabled` is true. Uses `goAsync()` so work completes before the receiver process is killed. On failure, posts a “Pause needs attention” notification.
- **onRevoke** — When the user revokes VPN permission, `PauseVpnService.onRevoke()` stops the VPN and releases the TUN interface.

For full feature specs, data flows, and sequence diagrams, see [PRDv3.md](PRDv3.md). For repository and DB schema, see [data-layer.md](data-layer.md).
