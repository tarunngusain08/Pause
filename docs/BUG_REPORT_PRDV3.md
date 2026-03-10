# Bug Report: PRDv3 Implementation Review

**Review Date:** Based on plan at `prdv3_web_filter_implementation_55415cdd.plan.md`  
**Scope:** Codebase audit against plan specification

---

## 1. Critical Bugs (Blocking / Data Loss / Security)

### BUG-001: VPN DNS Pipeline Non-Functional — TUN Packet Format Mismatch

**Severity:** Critical  
**Component:** `PauseVpnService`, `DNSPacketParser`  
**Plan Reference:** C.2.1, C.2.2

**Description:**  
The VPN reads raw bytes from the TUN interface and passes them directly to `DNSPacketParser.parseQuery()`. However, TUN delivers **IP packets** (layer 3), not raw DNS/UDP payloads. Each packet has:
- IP header (typically 20 bytes)
- UDP header (8 bytes)
- DNS payload

The parser assumes the first 12 bytes are the DNS header (transaction ID, flags, etc.). In reality, the first bytes are the IP header (version, TTL, etc.), so:
- `parseQuery()` parses garbage
- Extracted "domain" is invalid
- Blocklist/whitelist checks operate on wrong data

**Impact:** Web filtering does not work. Blocked domains are not actually blocked.

**Fix Plan:**
1. Add IP/UDP packet parsing before DNS:
   - Parse IP header, verify protocol = UDP (17)
   - Parse UDP header, verify dest port = 53
   - Extract UDP payload as DNS packet
2. After building DNS response, wrap it in UDP + IP headers and write back to TUN `FileOutputStream`
3. Use correct source/dest IP and port swapping for the response packet

---

### BUG-002: VPN DNS Responses Never Written Back to TUN

**Severity:** Critical  
**Component:** `PauseVpnService`  
**Plan Reference:** C.2.1

**Description:**  
In `runDnsLoop()`, the code builds NXDOMAIN or upstream responses but **never writes them back** to the TUN interface. The `when` block computes `resolveUpstream()` or `buildNXDomainResponse()` and discards the result. There is no `FileOutputStream` write to inject the response packet.

**Impact:** All DNS queries (blocked or not) receive no response. Apps/browsers will see DNS timeouts. Blocking appears to "work" only because nothing resolves, but clean domains also fail.

**Fix Plan:**
1. Obtain `FileOutputStream` from `vpnFd.fileDescriptor` (or use `FileDescriptor` with appropriate read/write)
2. After computing `response: ByteArray`, wrap it in IP+UDP headers (see BUG-001)
3. Write the full response packet to the TUN output stream
4. Ensure source/dest addresses and ports are correctly swapped for the reply

---

### BUG-003: BlocklistMatcher Wildcard Uses Raw Domain (Case-Sensitive)

**Severity:** Medium  
**Component:** `BlocklistMatcher`  
**Plan Reference:** C.2.3

**Description:**  
In `isBlocked()`, the wildcard check uses:
```kotlin
domains.any { it.startsWith("*.") && domain.endsWith(it.removePrefix("*.")) }
```
The `domain` parameter is used directly. If the DNS query returns `Sub.Example.COM`, the check `"Sub.Example.COM".endsWith("example.com")` is **false** due to case. The plan specifies "Case-insensitive" for keyword matching; domain matching should be consistent.

**Fix Plan:**  
Use `normalized` instead of `domain` in the wildcard check:
```kotlin
domains.any { it.startsWith("*.") && normalized.endsWith(it.removePrefix("*.").lowercase()) }
```
(Ensure the suffix is also normalized for comparison.)

---

### BUG-004: VPN Config Check at Start Only — No Runtime Disable

**Severity:** Medium  
**Component:** `PauseVpnService`  
**Plan Reference:** C.2.1

**Description:**  
`runDnsLoop()` reads `config.vpnEnabled` once at the beginning. If the user disables the web filter in settings while the VPN is running, the loop continues until the process is killed. There is no periodic re-check or `Flow` observation to stop the VPN when `vpnEnabled` becomes false.

**Fix Plan:**
1. Observe `WebFilterConfigRepository.getConfigFlow()` and cancel the DNS loop when `vpnEnabled` becomes false
2. Or poll `getConfig()` every N seconds and break the loop if disabled
3. Ensure `stopVpn()` is called when config changes

---

## 2. Plan-Specified Bugs (From Part 2 of Plan)

### BUG-005: PauseAccessibilityService Race Condition

**Severity:** Medium  
**Component:** `PauseAccessibilityService`  
**Plan Reference:** Part 2, Bug #5

**Description:**  
`currentForegroundPackage` is written on the main thread in `onAccessibilityEvent` and read inside `serviceScope.launch` coroutines. Multiple rapid app switches can cause interleaving: a coroutine may proceed with a stale `packageName` after the user has already switched to another app.

**Fix Plan:**
1. Capture `packageName` at the start of the coroutine and use it consistently
2. Or use `AtomicReference<String>` with proper memory visibility
3. Re-check `currentForegroundPackage` before each overlay show, and abort if it changed

---

### BUG-006: OverlayManager State Not Thread-Safe

**Severity:** Medium  
**Component:** `OverlayManager`  
**Plan Reference:** Part 2, Bug #6

**Description:**  
`currentState` and `currentOverlay` are read/written from the accessibility service coroutine (which may run on `Dispatchers.Main` or `Dispatchers.Default`) and from direct calls. No synchronization or `@MainThread` enforcement.

**Fix Plan:**
1. Annotate all overlay methods with `@MainThread` and ensure callers use `Dispatchers.Main`
2. Or use a mutex/synchronized block for state updates
3. Or confine overlay operations to a single-threaded dispatcher

---

### BUG-007: accessibility_service_config canRetrieveWindowContent Still False

**Severity:** High (for Phase 4 URL reader)  
**Component:** `accessibility_service_config.xml`  
**Plan Reference:** Part 2, Bug #8; C.3.7

**Description:**  
The plan requires `canRetrieveWindowContent="true"` for Phase 4 URL bar reading from browsers. The config still has `canRetrieveWindowContent="false"`, so `BrowserURLReader` (when implemented) cannot access node content.

**Fix Plan:**  
Set `canRetrieveWindowContent="true"` in `accessibility_service_config.xml`. Add Play Store data safety justification for this permission.

---

### BUG-008: SessionRepository parseBlockedPackages — Fragile JSON Parsing

**Severity:** Low  
**Component:** `SessionRepository`  
**Plan Reference:** Part 2, Suggestion #10

**Description:**  
`parseBlockedPackages()` manually parses `["pkg1","pkg2"]` with `removeSurrounding` and `split`. Package names containing commas or quotes would break. The plan suggests using `kotlinx.serialization` or `Gson`.

**Fix Plan:**  
Replace with `Json.decodeFromString<List<String>>(json)` (kotlinx.serialization) or `Gson().fromJson(json, Array<String>::class.java).toList()`.

---

## 3. Missing Plan Components (Gaps)

### GAP-001: VPN addDisallowedApplication Not Used

**Plan Reference:** C.2.1

**Description:**  
The plan specifies `addDisallowedApplication(packageName)` so the VPN does not route the Pause app’s own traffic through itself (avoiding loops). The current `Builder` config does not call this.

**Fix Plan:**  
Add `config.addDisallowedApplication(context.packageName)` before `establish()`.

---

### GAP-002: DNSResolver, BlockResponseBuilder, LocalHTTPServer Not Implemented

**Plan Reference:** C.2.5, C.2.6, C.2.7

**Description:**  
The plan specifies separate classes:
- `DNSResolver` — forwards to upstream
- `BlockResponseBuilder` — builds NXDOMAIN/redirect (partially in `DNSPacketParser`)
- `LocalHTTPServer` — serves block page on 127.0.0.1:8080

Current implementation inlines logic in `PauseVpnService` and has no LocalHTTPServer or block page.

**Fix Plan:**  
Extract `DNSResolver` and `BlockResponseBuilder`, implement `LocalHTTPServer` with `block_page.html`, and route blocked HTTP requests to it (HTTPS will show browser DNS error).

---

### GAP-003: BootReceiver Does Not Restart VPN

**Plan Reference:** C.6.1

**Description:**  
`BootReceiver` resumes strict and parental sessions but does not restart `PauseVpnService` when `WebFilterConfig.vpnEnabled` is true. After reboot, web filtering stays off until the user manually re-enables it.

**Fix Plan:**  
In `BootReceiver.onReceive`, after strict/parental resume, check `WebFilterConfigRepository.getConfig()?.vpnEnabled` and if true, start `PauseVpnService` with `ACTION_START`.

---

### GAP-004: DNSPacketParser buildNXDomainResponse Question Copy

**Severity:** Low  
**Component:** `DNSPacketParser`

**Description:**  
`buf.put(query.rawPacket, 12, query.rawPacket.size - 12)` can throw if `query.rawPacket.size <= 12`. The question section length is not validated.

**Fix Plan:**  
Add a check: `if (query.rawPacket.size <= 12) return ByteArray(0)` or handle gracefully. Ensure the copied region is valid.

---

### GAP-005: Phase 2 Features Gated — No UI to Enable Phase 2

**Description:**  
Reflection, allowance, launch limits, and focus mode are gated by `FeatureFlags.isPhase2Enabled`, which derives from `PreferencesManager.currentPhase >= 2`. There is no UI for the user to set or advance the phase. Default is `DEFAULT_PHASE = 1`, so Phase 2 features never activate.

**Fix Plan:**  
Add a phase selector in Settings (e.g., "Unlock Phase 2 features") or automatically advance phase when the user completes certain milestones. Alternatively, default `currentPhase` to 2 for testing.

---

## 4. Fix Priority and Sequencing

| Priority | Bug ID    | Effort | Dependency        |
|----------|-----------|--------|-------------------|
| P0       | BUG-001   | 2–3 d  | —                 |
| P0       | BUG-002   | 1–2 d  | BUG-001           |
| P1       | BUG-007   | 0.5 d  | — (for C.3)       |
| P1       | GAP-003   | 0.5 d  | —                 |
| P1       | GAP-001   | 0.25 d | —                 |
| P2       | BUG-003   | 0.25 d | —                 |
| P2       | BUG-004   | 0.5 d  | —                 |
| P2       | BUG-005   | 0.5 d  | —                 |
| P2       | BUG-006   | 0.5 d  | —                 |
| P3       | BUG-008   | 0.25 d | —                 |
| P3       | GAP-002   | 2–3 d  | BUG-001, BUG-002  |
| P3       | GAP-004   | 0.25 d | —                 |
| P3       | GAP-005   | 0.5 d  | —                 |

---

## 5. Recommended Fix Order

1. **BUG-001 + BUG-002** — Fix VPN packet handling and response injection (unblocks web filtering).
2. **GAP-001** — Add `addDisallowedApplication` to avoid VPN routing its own traffic.
3. **GAP-003** — BootReceiver VPN restart.
4. **BUG-007** — Enable `canRetrieveWindowContent` for future URL reader.
5. **BUG-003, BUG-004** — BlocklistMatcher and config reactivity.
6. **BUG-005, BUG-006** — Concurrency and thread safety.
7. **GAP-002** — LocalHTTPServer and block page.
8. **BUG-008, GAP-004, GAP-005** — Lower-priority cleanups.

---

## 6. Testing Recommendations

After fixes:

1. **VPN:** Use `nslookup` or a test app to query a blacklisted domain; verify NXDOMAIN or block page.
2. **Boot:** Reboot device with VPN enabled; verify VPN restarts.
3. **Config:** Disable VPN in settings; verify VPN stops within a few seconds.
4. **BlocklistMatcher:** Add `*.test.com`, query `sub.test.com` and `SUB.TEST.COM`; both should match.
