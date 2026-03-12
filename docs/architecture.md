# Focus — Implementation Architecture

This document describes how the Focus Android app is structured at the code level: layers, entry points, and how the main subsystems interact.

---

## 1. High-Level Layers

```
┌─────────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                            │
│  HomeScreen, ParentDashboardScreen, WebFilterDashboardScreen,    │
│  OnboardingScreen, SettingsScreen, FocusModeScreen, etc.         │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│  ViewModels (Hilt-injected)                                       │
│  HomeViewModel, ChildStatusViewModel, WebFilterDashboardViewModel│
│  State: UiState / Flow; call repositories and services            │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│  Repository Layer                                                │
│  AppRepository, LaunchRepository, BlacklistRepository,           │
│  WhitelistRepository, SessionRepository, InsightsRepository, ...  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│  Room DB (PauseDB)   │ │  DataStore / Prefs    │ │  Android APIs         │
│  DAOs, Entities      │ │  PreferencesManager   │ │  UsageStatsManager    │
└──────────────────────┘ └──────────────────────┘ └──────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  Services (run outside UI process)                              │
│  PauseAccessibilityService, PauseVpnService                      │
│  Use Entry Points to get repositories / managers (no full Hilt)  │
└─────────────────────────────────────────────────────────────────┘
```

- **UI** uses Compose and `PauseNavGraph` for navigation; screens use ViewModels with `hiltViewModel()`.
- **ViewModels** expose `StateFlow`/`Flow` and call **repositories**; they do not hold references to the Accessibility Service or VPN.
- **Repositories** encapsulate Room DAOs, DataStore, and system APIs; they are the single source of truth for app and parent configuration, launch events, web filter data, etc.
- **Services** cannot use constructor-injected Hilt components (they are created by the system). They use **Hilt Entry Points** (`PauseAccessibilityEntryPoint`, `VpnEntryPoint`, `BootEntryPoint`) to obtain dependencies at runtime.

---

## 2. Dependency Injection (Hilt)

- **Application:** `PauseApplication` is annotated with `@HiltAndroidApp` and implements `Configuration.Provider` for WorkManager with `HiltWorkerFactory`.
- **Modules:** `DatabaseModule` provides `PauseDatabase` and DAOs; `AppModule` provides repositories, overlay manager, session managers, and other app-scoped objects.
- **Entry Points** (for non-Hilt contexts):
  - **PauseAccessibilityEntryPoint** — Used by `PauseAccessibilityService` to get `OverlayManager`, `AppRepository`, `LaunchRepository`, `AllowanceTracker`, `SessionRepository`, `ParentalBlockedAppRepository`, `PreferencesManager`, `StrictSessionManager`, `ParentalControlManager`, `InsightsRepository`, `BrowserURLReader`, `URLClassifier`, `URLCaptureQueue`, `AutoBlacklistEngine`, `WebFilterConfigRepository`.
  - **VpnEntryPoint** — Used by `PauseVpnService` to get `BlocklistMatcher`, `WhitelistMatcher`, `WebFilterConfigRepository` (also exposes `BlacklistRepository`, `WhitelistRepository`).
  - **BootEntryPoint** — Used by `BootReceiver` to get `StrictSessionManager`, `ParentalControlManager`, `WebFilterConfigRepository` for resuming sessions and VPN after boot.

Only the minimum set of dependencies is exposed per entry point to keep services decoupled from the full graph.

---

## 3. Navigation

- **Single Activity:** `MainActivity` hosts Compose and sets `setContent { PauseTheme { Surface { PauseNavGraph() } } }`.
- **PauseNavGraph** uses a single `NavHost` and `rememberNavController()`. Start destination is `Routes.ONBOARDING` or `Routes.HOME` based on `OnboardingViewModel.onboardingComplete` (via `collectAsStateWithLifecycle`).
- **Routes** — Centralized in `Routes` object: `Routes.HOME`, `Routes.APP_SELECTION`, `Routes.PARENT_DASHBOARD`, `Routes.WEB_FILTER_DASHBOARD`, `Routes.UNBLOCK_REQUEST`, etc.
- **Deep link:** `focus://unblock-request?domain={domain}` opens `UnblockRequestScreen` with the given domain (e.g. from the block page “Request Review” button).

---

## 4. Application Startup

1. **PauseApplication.onCreate()**  
   - Schedules `MidnightResetWorker` as unique periodic work (24-hour period, first run at next midnight).

2. **First launch / not onboarding complete**  
   - Nav graph shows `OnboardingScreen`; user grants Accessibility, overlay, and (optionally) usage access, then selects monitored apps. On complete, `onboardingComplete` is persisted and nav moves to `home`.

3. **Accessibility Service**  
   - User enables Focus in Settings → Accessibility. `PauseAccessibilityService.onServiceConnected()` runs; it obtains `OverlayManager` and other deps via `PauseAccessibilityEntryPoint` and then handles `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED` for app interception and URL capture.

4. **Web Filter (if enabled by parent)**  
   - Parent enables Web Filter from Parent Dashboard; app starts `PauseVpnService` with `ACTION_START`. VPN establishes a TUN interface (excluding the Focus app via `addDisallowedApplication`) and runs the DNS loop on a coroutine. `BootReceiver` re-starts VPN after `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` if `vpnEnabled` was true.

---

## 5. Key Cross-Cutting Behaviors

- **Foreground detection** — Only in Accessibility Service; uses `event.packageName` and ignores when it hasn’t changed. Interception is delegated to **InterceptionPipeline** (Strict → Commitment → Parental → Standard).
- **Overlays** — All shown via `OverlayManager` (delay, reflection, commitment block, strict block, parental block, lock intervention, PIN entry, etc.). Manager holds a single `currentOverlay` and `OverlayState`; `dismiss()` only clears overlays that are “interception” related, not session-complete or informational ones.
- **Persistence** — All configuration and behavioral data goes through Room or DataStore; no in-memory-only critical state.
- **Midnight reset** — `MidnightResetWorker` runs once per day (scheduled for midnight); resets daily counters and evaluates streaks. WorkManager is configured with Hilt so workers can receive repositories via constructor injection.

---

## 6. Related Docs

- [accessibility-and-overlays.md](accessibility-and-overlays.md) — How the Accessibility Service and OverlayManager implement interception and overlays.
- [webfilter.md](webfilter.md) — VPN DNS engine, URL reader, keyword matching, and block page.
- [data-layer.md](data-layer.md) — Room schema, entities, DAOs, and repositories.

For product-level design (phases, use cases, flows), see [Design.md](Design.md) and [PRDv3.md](PRDv3.md).
