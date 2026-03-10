# Data Layer — Room, Repositories, DAOs

Pause stores all app and web-filter data locally using **Room** (SQLite). Repositories wrap DAOs and expose Kotlin Flow or suspend APIs to the rest of the app. This document summarizes the schema, DAOs, and repository roles.

---

## 1. PauseDatabase

- **Class:** `com.pause.app.data.db.PauseDatabase`
- **Version:** 3
- **TypeConverters:** `Converters` (for enums, timestamps, etc.)
- **Storage:** Built with `createDeviceProtectedStorageContext()` on API 24+ so data is available after device unlock (e.g. for WorkManager and services).

Migrations:

- **1 → 2:** No schema change (placeholder).
- **2 → 3:** Adds Web Filter tables: `blacklisted_domains`, `whitelisted_domains`, `keyword_entries`, `url_visit_log`, `pending_review`, `web_filter_config`, plus indexes.

---

## 2. Entities and DAOs (Overview)

| Entity | DAO | Purpose |
|--------|-----|---------|
| **MonitoredApp** | MonitoredAppDao | Apps selected for delay/reflection/limits (package, name, friction level, daily launch limit). |
| **LaunchEvent** | LaunchEventDao | Each open attempt: package, timestamp, cancelled or not, delay duration, reflection reason. |
| **Session** | SessionDao | Focus and commitment sessions: type, start/end, active, broken, blocked packages. |
| **Streak** | StreakDao | Current streak days, streak start, last valid day, shields used/remaining, longest streak. |
| **ReflectionResponse** | ReflectionResponseDao | User’s “why” (e.g. Bored, Habit) linked to launch events. |
| **UnlockEvent** | UnlockEventDao | Unlock timestamps for lock-screen intervention stats. |
| **Accountability** | AccountabilityDao | Accountability partner contact, name, accepted, last summary sent. |
| **StrictBreakLog** | StrictBreakLogDao | Log of strict session breaks (e.g. emergency exit). |
| **ParentalConfig** | ParentalConfigDao | Parental mode: PIN hash, schedule, etc. |
| **ScheduleBandEntity** | ScheduleBandDao | Time bands (e.g. Free / Limited / Restricted) for parental schedule. |
| **ParentalBlockedApp** | ParentalBlockedAppDao | Apps blocked or friction-only under parental control. |
| **PINAuditLog** | PINAuditLogDao | Audit log for parent PIN entry (e.g. success/fail). |
| **BlacklistedDomain** | BlacklistedDomainDao | Web Filter: blocked domains (domain, source, category, active, pending review). |
| **WhitelistedDomain** | WhitelistedDomainDao | Web Filter: allowed domains (override blacklist). |
| **KeywordEntry** | KeywordDao | Web Filter: keyword list (keyword, category, active, bundled). |
| **UrlVisitLog** | UrlVisitLogDao | Web Filter: visited URLs (fullUrl, domain, browser, time, blocked, classification). |
| **PendingReview** | PendingReviewDao | Web Filter: unblock requests and auto-blocks for parent review. |
| **WebFilterConfig** | WebFilterConfigDao | Web Filter: singleton config (vpn_enabled, url_reader_enabled, upstream_dns, etc.). |

---

## 3. Repository Layer

Repositories are provided by Hilt and used by ViewModels and services (via entry points). They encapsulate one or more DAOs and optional system APIs.

### Core app behavior

- **AppRepository** — Monitored apps: add/remove, get by package, friction level, launch limit, `isMonitored(packageName)`.
- **LaunchRepository** — Record launch events, `getTodayLaunchCount(packageName)`, cancel last launch; used by OverlayManager and MidnightResetWorker.
- **SessionRepository** — Save/get focus and commitment sessions, `getActiveFocusSession()`, `getActiveCommitmentSession()`, `isPackageInCommitmentBlockList()`, `markSessionBroken()`.
- **StreakDao / StreakRepository** (or equivalent) — Streak read/update, shield use, reset; used at midnight and on commitment break.
- **InsightsRepository** — Unlock count, weekly/monthly aggregates, daily summary for accountability; uses LaunchEventDao, UnlockEventDao, UsageStatsManager.
- **AccountabilityRepository** — Partner setup, last summary sent; used by accountability dispatch worker.

### Parental and strict

- **ParentalConfigRepository** — Load/save parental config (PIN, schedule).
- **ParentalBlockedAppRepository** — Blocked/friction apps under parental control; `getByPackageName()`.
- **StrictSessionManager** — Strict mode session state (active session, remaining time, blocked packages, emergency exit); uses SessionRepository and app config.

### Web Filter

- **BlacklistRepository** — Active blacklisted domains for VPN and UI; `getActiveDomainsAsList()`, add/remove/toggle, pending review.
- **WhitelistRepository** — Whitelisted domains; `getAllAsList()`, add/remove; used by WhitelistMatcher.
- **KeywordRepository** — Keyword list for URL classifier; active keywords, add/remove, categories.
- **UrlVisitLogRepository** — Insert visit log, get recent for parent, mark reviewed; used by URLCaptureQueue and Web Filter UI.
- **WebFilterConfigRepository** — Get/update singleton WebFilterConfig (vpn_enabled, url_reader_enabled, upstream_dns, etc.); used by VPN and Accessibility Service.
- **PendingReviewDao / repository** — Unblock requests and auto-blocked domains for parent review queue.

---

## 4. Preferences and Feature Flags

- **PreferencesManager** (DataStore or SharedPreferences) — Delay duration, Phase 2 enabled, overlay and usage preferences. Not in Room; provided separately (e.g. AppModule).
- **FeatureFlags** — Exposes `isPhase2Enabled` Flow; used by Accessibility Service to decide reflection vs delay-only.

---

## 5. Usage from Services

- **PauseAccessibilityService** — Gets repositories and managers via **PauseAccessibilityEntryPoint**: AppRepository, LaunchRepository, SessionRepository, AllowanceTracker, PreferencesManager, FeatureFlags, StrictSessionManager, ParentalControlManager, ParentalBlockedAppRepository, WebFilterConfigRepository, BrowserURLReader, URLClassifier, URLCaptureQueue, InsightsRepository.
- **PauseVpnService** — Gets **VpnEntryPoint**: BlocklistMatcher, WhitelistMatcher, WebFilterConfigRepository. BlocklistMatcher and WhitelistMatcher internally use BlacklistRepository and WhitelistRepository and cache domain sets in memory.

---

## 6. Workers

- **MidnightResetWorker** — Scheduled at midnight; resets daily counters and evaluates streak (reads LaunchEventDao / SessionDao / StreakDao via constructor-injected repositories). Uses Hilt’s `HiltWorkerFactory`.
- **AccountabilityDispatchWorker** (if present) — Builds daily summary from InsightsRepository and AccountabilityRepository and triggers SMS intent.

---

## 7. Conventions

- **Suspend and Flow** — Repositories expose `suspend` or `Flow` for DB access; ViewModels collect flows on the main dispatcher.
- **Single source of truth** — All persistent state goes through Room or DataStore; no critical state only in memory.
- **Migrations** — New schema changes require a new migration and version bump in `PauseDatabase`.

For Web Filter table details and field meanings, see [PRDv3.md](PRDv3.md). For architecture and entry points, see [architecture.md](architecture.md).
