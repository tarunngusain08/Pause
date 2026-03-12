# Data Layer — Room, Repositories, DAOs

Focus stores all app and web-filter data locally using **Room** (SQLite). Repositories wrap DAOs and expose Kotlin Flow or suspend APIs to the rest of the app. This document summarizes the schema, DAOs, and repository roles.

---

## 1. PauseDatabase

- **Class:** `com.pause.app.data.db.PauseDatabase`
- **Version:** 6
- **TypeConverters:** `Converters` (for enums, timestamps, etc.)
- **Storage:** Built with `createDeviceProtectedStorageContext()` on API 24+ so data is available after device unlock (e.g. for WorkManager and services).

Migrations:

- **1 → 2:** No schema change (placeholder).
- **2 → 3:** Adds Web Filter tables: `blacklisted_domains`, `keyword_entries`, `url_visit_log`, `pending_review`, `web_filter_config`, plus indexes.
- **5 → 6:** Drops `whitelisted_domains` table (whitelist removed).

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
| **KeywordEntry** | KeywordDao | Web Filter: keyword list (keyword, category, active, bundled). |
| **UrlVisitLog** | UrlVisitLogDao | Web Filter: visited URLs (fullUrl, domain, browser, time, blocked, classification). |
| **PendingReview** | PendingReviewDao | Web Filter: unblock requests and auto-blocks for parent review. |
| **WebFilterConfig** | WebFilterConfigDao | Web Filter: singleton config (vpn_enabled, url_reader_enabled, upstream_dns, etc.). |

---

## 3. Repository Layer

Repositories are provided by Hilt and used by ViewModels and services (via entry points). They encapsulate one or more DAOs and optional system APIs.

### Core app behavior

- **LaunchRepository** — Record launch events; used by MidnightResetWorker for cleanup (`deleteEventsOlderThan`).
- **SessionRepository** — Save/get focus and strict sessions, `getActiveFocusSession()`, `markSessionBroken()`.
- **InsightsRepository** — Unlock count, weekly/monthly aggregates, daily summary for accountability; uses LaunchEventDao, UnlockEventDao, UsageStatsManager.
- **AccountabilityRepository** — Partner setup, last summary sent. (AccountabilityDispatchWorker for daily SMS summary is not yet implemented.)

### Parental and strict

- **ParentalConfigRepository** — Load/save parental config (PIN, schedule).
- **ParentalBlockedAppRepository** — Blocked/friction apps under parental control; `getByPackageName()`.
- **StrictSessionManager** — Strict mode session state (active session, remaining time, blocked packages, emergency exit); uses SessionRepository and app config.

### Web Filter

- **BlacklistRepository** — Active blacklisted domains for VPN and UI; `getActiveDomainsAsList()`, add/remove/toggle, pending review.
- **KeywordRepository** — Keyword list for URL classifier; active keywords, add/remove, categories.
- **UrlVisitLogRepository** — Insert visit log, get recent for parent, mark reviewed; used by URLCaptureQueue and Web Filter UI.
- **WebFilterConfigRepository** — Get/update singleton WebFilterConfig (vpn_enabled, url_reader_enabled, upstream_dns, etc.); used by VPN and Accessibility Service.
- **PendingReviewDao / repository** — Unblock requests and auto-blocked domains for parent review queue.

---

## 4. Preferences

- **PreferencesManager** — Uses **DataStore** (`pause_preferences`). Holds: `onboardingComplete`, parental setup step, PIN hash, recovery phrase hash, PIN attempt/lockout. Provided via Hilt.

---

## 5. Usage from Services

- **PauseAccessibilityService** — Gets dependencies via **PauseAccessibilityEntryPoint**: OverlayManager, StrictSessionManager, ContentShieldManager, InsightsRepository, BrowserURLReader, URLClassifier, URLCaptureQueue, AutoBlacklistEngine, WebFilterConfigRepository. Uses **InterceptionPipeline** for app interception logic.
- **PauseVpnService** — Gets **VpnEntryPoint**: BlocklistMatcher, WebFilterConfigRepository. BlocklistMatcher uses BlacklistRepository and caches domain set in memory (with mutex and 60s reload).

---

## 6. Workers

- **MidnightResetWorker** — Scheduled as unique periodic work (24h, first run at next midnight). In `doWork()`: deletes launch events, reflection responses, unlock events, and sessions older than 90 days; deletes URL visit log and pending reviews older than 30/90 days. Uses Hilt’s `HiltWorkerFactory`; injects LaunchRepository, InsightsRepository, SessionRepository, UrlVisitLogRepository, PendingReviewDao.

---

## 7. Conventions

- **Suspend and Flow** — Repositories expose `suspend` or `Flow` for DB access; ViewModels collect flows on the main dispatcher.
- **Single source of truth** — All persistent state goes through Room or DataStore; no critical state only in memory.
- **Migrations** — New schema changes require a new migration and version bump in `PauseDatabase`.

For Web Filter table details and field meanings, see [PRDv3.md](PRDv3.md). For architecture and entry points, see [architecture.md](architecture.md).
