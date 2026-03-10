# Accessibility Service and Overlay Manager

This document describes how Pause uses the Android Accessibility Service for foreground app detection and interception, and how the OverlayManager presents delay, reflection, block, and other overlays.

---

## 1. PauseAccessibilityService — Role and Constraints

- **Declared in** `AndroidManifest.xml` and `res/xml/accessibility_service_config.xml`. The service is bound when the user enables “Pause” under Settings → Accessibility.
- **Purpose:**
  - Detect when the **foreground app** changes (`TYPE_WINDOW_STATE_CHANGED`).
  - Optionally read **URL bar text** in supported browsers when Web Filter URL capture is enabled (`TYPE_WINDOW_CONTENT_CHANGED`).
- **What it does not do:** Read generic screen content, passwords, or messages. URL reading is limited to the address bar for classification and visit log only; enforcement is done by the VPN.

The service cannot use Hilt constructor injection (it is created by the system). It uses **PauseAccessibilityEntryPoint** to obtain dependencies from the application context when `onServiceConnected()` runs.

---

## 2. Event Handling

### 2.1 TYPE_WINDOW_STATE_CHANGED

Handled in `handleWindowStateChanged(event)`:

1. **Package change**  
   - If `event.packageName` equals the last known foreground package, the event is ignored.

2. **Unlock detection (lock screen intervention)**  
   - If the new package is a launcher and the previous package was not, the app treats this as an “unlock.” It records the unlock and checks whether to show the lock intervention overlay (e.g. ≥5 unlocks in 15 minutes).

3. **Excluded packages**  
   - System UI, launchers, Settings, package installer, and the Pause app itself are excluded from interception.

4. **Strict Mode**  
   - If a Strict session is active and the current app is blocked, `StrictBlockOverlayView` is shown (with optional emergency exit). If the app is not blocked, any existing block overlay is dismissed.

5. **Commitment Mode**  
   - If an active commitment session blocks this package, `CommitmentBlockOverlayView` is shown. “Break commitment” leads to the 90s cooldown and then session break + streak reset.

6. **Parental Control**  
   - If the app is in the parental blocked list, the parental block overlay is shown (with optional emergency contact). If the app only requires friction, the delay overlay is shown.

7. **Self-use monitored apps**  
   - If the package is in the monitored apps list:
     - **Allowance exhausted** → `AllowanceReachedOverlayView` (with “Open anyway”).
     - **Launch limit reached** → `LaunchLimitOverlayView` (with “Open anyway”).
     - Otherwise → reflection (if Phase 2 or friction forces it) then delay overlay, or delay only.

All overlay calls are done via **OverlayManager**, which is obtained from the entry point. Coroutines are launched on a `serviceScope` (Main + SupervisorJob) so that repository and preference reads are async-safe.

### 2.2 TYPE_WINDOW_CONTENT_CHANGED (Web Filter URL capture)

Handled in `handleWindowContentChanged(event)`:

- **Only for known browsers** — The entry point’s `BrowserURLReader.isKnownBrowser(packageName)` must be true (e.g. Chrome, Firefox, Brave).
- **URL bar text** — `BrowserURLReader.extractURL(rootInActiveWindow, packageName)` returns the current URL string from the accessibility tree.
- **When Web Filter URL capture is enabled** — The URL is normalized, classified (e.g. CLEAN, KEYWORD_MATCH, BLACKLISTED) via `URLClassifier`, and then passed to `URLCaptureQueue.enqueue(...)` for logging and optional auto-blacklist. Enforcement of blocked domains remains the responsibility of the VPN.

Details of URL classification and VPN enforcement are in [webfilter.md](webfilter.md).

---

## 3. OverlayManager — Design

- **Singleton** — Injected via Hilt and also obtained by the Accessibility Service through `PauseAccessibilityEntryPoint.getOverlayManager()`.
- **Single active overlay** — `currentOverlay: View?` and `currentState: OverlayState` represent the one visible overlay. Showing a new overlay typically calls `dismissOverlay()` first (except where the same overlay type is intentionally re-shown).
- **Window type** — Overlays are added with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` (API 26+); below that, `TYPE_PHONE`. They use `FLAG_NOT_FOCUSABLE` and `FLAG_LAYOUT_IN_SCREEN` with `MATCH_PARENT` and `Gravity.CENTER`.

---

## 4. Overlay Types and State

`OverlayState` is an enum used to track what is currently shown. Examples:

| State | View / Behavior |
|-------|------------------|
| `IDLE` | No overlay |
| `SHOWING_REFLECTION` | ReflectionOverlayView — “Why are you opening &lt;app&gt;?” with reason buttons |
| `SHOWING_DELAY` | DelayOverlayView — countdown, Cancel / wait |
| `SHOWING_COMMITMENT_BLOCK` | CommitmentBlockOverlayView — “Break commitment” → cooldown |
| `SHOWING_STRICT_BLOCK` | StrictBlockOverlayView — app blocked for strict session; optional emergency exit |
| `SHOWING_ALLOWANCE_REACHED` | AllowanceReachedOverlayView — daily allowance used; “Open anyway” / “I’m done” |
| `SHOWING_LAUNCH_LIMIT` | LaunchLimitOverlayView — per-app limit reached; “Open anyway” / skip |
| `SHOWING_COOLDOWN` | CooldownOverlayView — 90s cooldown for commitment break |
| `SHOWING_LOCK_INTERVENTION` | LockInterventionOverlayView — “You’ve unlocked X times…” |
| `SHOWING_PARENTAL_BLOCK` | ParentalBlockOverlayView — app blocked by parent; optional emergency contact |
| `SHOWING_POWER_BLOCK` | PowerMenuBlockOverlayView — power menu blocked during strict/parental |
| `SHOWING_PIN_ENTRY` | PINEntryOverlayView — parent PIN to access dashboard |
| … | Session complete, session resume, schedule resume, emergency confirm, etc. |

---

## 5. Dismiss Behavior

- **dismiss()** — Only dismisses overlays that are considered “interception” (delay, reflection, block, cooldown, launch limit, allowance, lock intervention, PIN entry, etc.). It does **not** dismiss session-complete or session-resume overlays so the user can read and close them.
- **dismissOverlay()** — Removes `currentOverlay` from the `WindowManager` and sets state to `IDLE`. Used internally when the user completes or cancels an overlay.

---

## 6. Navigation and Home

- **navigateHome()** — Dispatches `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)` with `FLAG_ACTIVITY_NEW_TASK`. Used after Cancel on delay, after lock intervention “I’m aware,” or when returning from a block overlay so the user is not left inside the blocked app.

---

## 7. Power Key and Strict / Parental

- **onKeyEvent** — If the key is power long-press and either a Strict session is active or Parental Control is active, the service shows `PowerMenuBlockOverlayView` (or similar) to discourage turning off the device to bypass. The event is consumed (`return true`) in that case.

---

## 8. Implementation Notes

- **Threading** — Overlay views must be attached/detached on the main thread. The Accessibility Service uses `serviceScope` with `Dispatchers.Main` for calls that lead to overlay show/dismiss.
- **Re-checking package** — Before showing an overlay, the code often checks `currentForegroundPackage != packageName` and returns early to avoid showing stale overlays if the user switched apps quickly.
- **Overlay permission** — Before adding overlays, the code checks `Settings.canDrawOverlays(context)` (API 23+). If permission is missing, overlay show is skipped and a log is written.

For Web Filter URL capture flow and VPN, see [webfilter.md](webfilter.md). For product flows and states, see [Design.md](Design.md).
