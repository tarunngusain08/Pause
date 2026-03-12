# Accessibility Service and Overlay Manager

This document describes how Focus uses the Android Accessibility Service for foreground app detection and interception, and how the OverlayManager presents delay, reflection, block, and other overlays.

---

## 1. PauseAccessibilityService — Role and Constraints

- **Declared in** `AndroidManifest.xml` and `res/xml/accessibility_service_config.xml`. The service is bound when the user enables “Focus” under Settings → Accessibility.
- **Purpose:**
  - Detect when the **foreground app** changes (`TYPE_WINDOW_STATE_CHANGED`).
  - Optionally read **URL bar text** in supported browsers when Web Filter URL capture is enabled (`TYPE_WINDOW_CONTENT_CHANGED`).
- **What it does not do:** Read generic screen content, passwords, or messages. URL reading is limited to the address bar for classification and visit log only; enforcement is done by the VPN.

The service cannot use Hilt constructor injection (it is created by the system). It uses **PauseAccessibilityEntryPoint** to obtain dependencies from the application context when `onServiceConnected()` runs. Interception logic is delegated to **InterceptionPipeline**, which evaluates stages in order: Strict → Content Shield.

---

## 2. Event Handling

### 2.1 TYPE_WINDOW_STATE_CHANGED

Handled in `handleWindowStateChanged(event)`:

1. **Package change**  
   - If `event.packageName` equals the last known foreground package, the event is ignored.

2. **Excluded and launcher packages**  
   - System UI, launchers, package installer, and the Focus app itself are excluded. (Settings is evaluated and blocked during Focus Mode.) For these, `overlayManager.dismiss()` is called and the handler returns.

3. **InterceptionPipeline.evaluate(pkg)**  
   - The pipeline runs two stages in order; the first that handles the package returns true and stops:
     - **Strict** — If a Strict session is active and the app is blocked, `StrictBlockOverlayView` is shown (with optional emergency exit). If the app is allowed, any block overlay is dismissed.
     - **Content Shield** — If the app or URL is blocked by Content Shield (adult filter, social media filter, domain blacklist, keyword match), `ContentShieldBlockOverlayView` is shown.
   - If no stage handles the package, `overlayManager.dismiss()` is called.

**Unlock detection (lock screen intervention)** — Uses `ACTION_USER_PRESENT` broadcast via `userPresentReceiver` (registered in `onServiceConnected`). When the user unlocks the device, the app records the unlock. If ≥5 unlocks occurred in the last 15 minutes, it shows the lock intervention overlay.

All overlay calls are done via **OverlayManager**. Coroutines are launched on a `serviceScope` (Main + SupervisorJob) so that repository and preference reads are async-safe.

### 2.2 TYPE_WINDOW_CONTENT_CHANGED (Web Filter URL capture)

Handled in `handleWindowContentChanged(event)`:

- **Only for known browsers** — `BrowserURLReader.isKnownBrowser(packageName)` must be true (e.g. Chrome, Firefox, Brave, Samsung Internet, Edge, DuckDuckGo, Opera, Firefox Focus).
- **Debounce** — Per-package debounce of 500ms (`CONTENT_CHANGED_DEBOUNCE_MS`) to avoid excessive URL extraction.
- **URL bar text** — `BrowserURLReader.extractURL(rootInActiveWindow, packageName)` returns the current URL string from the accessibility tree (view ID or EditText fallback).
- **When Web Filter URL capture is enabled** — The URL is normalized, domain extracted, and classified via `URLClassifier.classify()` (CLEAN, KEYWORD_MATCH, BLACKLISTED, WHITELISTED). The result is passed to `URLCaptureQueue.enqueue(...)` for logging. If classification is `KEYWORD_MATCH`, `AutoBlacklistEngine.onKeywordMatch()` is called to add the domain to the blacklist (when `autoBlacklistOnKeywordMatch` is enabled) and create a `PendingReview`. Enforcement of blocked domains remains the responsibility of the VPN.

Details of URL classification and VPN enforcement are in [webfilter.md](webfilter.md).

---

## 3. OverlayManager — Design

- **Singleton** — Injected via Hilt and also obtained by the Accessibility Service through `PauseAccessibilityEntryPoint.getOverlayManager()`.
- **Single active overlay** — `currentOverlay: View?` and `currentState: OverlayState` represent the one visible overlay. Showing a new overlay typically calls `dismissOverlay()` first (except where the same overlay type is intentionally re-shown).
- **Window type** — Overlays are added with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` (API 26+); below that, `TYPE_PHONE`. They use `FLAG_NOT_FOCUSABLE` and `FLAG_LAYOUT_IN_SCREEN` with `MATCH_PARENT` and `Gravity.CENTER`.

---

## 4. Overlay Types and State

`OverlayState` is an enum with `priority` and optional `isInformational` (informational overlays are lower priority and may be dismissed differently):

| State | Priority | View / Behavior |
|-------|----------|------------------|
| `IDLE` | 0 | No overlay |
| `SHOWING_SCHEDULE_RESUME` | 10 | Schedule band change notification |
| `SHOWING_SESSION_RESUME` | 10 | Session resume notification |
| `SHOWING_SESSION_COMPLETE` | 10 | Session complete notification |
| `SHOWING_LOCK_INTERVENTION` | 20 | LockInterventionOverlayView — “You’ve unlocked X times…” |
| `SHOWING_PARENTAL_BLOCK` | 90 | ParentalBlockOverlayView — app blocked by parent; optional emergency contact |
| `SHOWING_POWER_BLOCK` | 90 | PowerMenuBlockOverlayView — power menu blocked during strict/parental |
| `SHOWING_PIN_ENTRY` | 95 | PINEntryOverlayView — parent PIN to access dashboard |
| `SHOWING_EMERGENCY_CONFIRM` | 95 | EmergencyConfirmOverlayView — confirm emergency exit |
| `SHOWING_CONTENT_SHIELD_BLOCK` | 100 | ContentShieldBlockOverlayView — app/URL blocked by Content Shield |
| `SHOWING_STRICT_BLOCK` | 100 | StrictBlockOverlayView — app blocked for Focus Mode session; optional emergency exit |

---

## 5. Dismiss Behavior

- **dismiss()** — Only dismisses overlays that are considered “interception” (delay, reflection, block, cooldown, launch limit, allowance, lock intervention, PIN entry, etc.). It does **not** dismiss session-complete or session-resume overlays so the user can read and close them.
- **dismissOverlay()** — Removes `currentOverlay` from the `WindowManager` and sets state to `IDLE`. Used internally when the user completes or cancels an overlay.

---

## 6. Navigation and Home

- **navigateHome()** — Dispatches `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)` with `FLAG_ACTIVITY_NEW_TASK`. Used after Cancel on delay, after lock intervention “I’m aware,” or when returning from a block overlay so the user is not left inside the blocked app.

---

## 7. Power Key, Recents, and Strict / Parental

- **onKeyEvent** — **Recents (APP_SWITCH):** When `SHOWING_STRICT_BLOCK` or `SHOWING_PARENTAL_BLOCK` is active, the service consumes the recents key and calls `performGlobalAction(GLOBAL_ACTION_HOME)` to prevent switching away from the block overlay. **Power long-press:** If a Strict session is active or Parental Control is active, the service shows `PowerMenuBlockOverlayView` to discourage turning off the device. The event is consumed (`return true`) in those cases.

---

## 8. Implementation Notes

- **Threading** — Overlay views must be attached/detached on the main thread. The Accessibility Service uses `serviceScope` with `Dispatchers.Main` for calls that lead to overlay show/dismiss.
- **Re-checking package** — `InterceptionPipeline` receives an `isForeground` lambda that checks `_foregroundPackage.value == pkg` (the service uses `MutableStateFlow<String?>` for the current foreground package). Before showing an overlay, stages call `isForeground(pkg)` and return early if the user switched apps.
- **Overlay permission** — Before adding overlays, the code checks `Settings.canDrawOverlays(context)` (API 23+). If permission is missing, overlay show is skipped and a log is written.

For Web Filter URL capture flow and VPN, see [webfilter.md](webfilter.md). For product flows and states, see [Design.md](Design.md).
