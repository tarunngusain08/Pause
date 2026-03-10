# Pause

**Pause** is an Android app that reduces impulsive phone usage through graduated behavioral intervention. Instead of hard blocking, it adds intentional friction — delays, reflection prompts, and commitment mechanisms — when opening distracting apps. In **Parental Control Mode**, it also provides web filtering via a local VPN and optional URL capture for safer browsing.

---

## Features

### Core (Self‑Use Mode)

- **Delay screen** — Countdown before opening monitored apps (configurable 5–20s)
- **Reflection prompt** — “Why are you opening this app?” (Bored / Habit / Replying / Intentional)
- **Daily allowance** — Total minutes per day across monitored apps (from UsageStats)
- **Launch limits** — Per‑app max opens per day with soft “Open anyway” option
- **Focus Mode** — Timed sessions with increased friction (no full block)
- **Commitment Mode** — Full block on selected apps for a set duration, with 90s cooldown to break
- **Streaks & shields** — Day valid if limits respected; optional grace day per week
- **Lock screen intervention** — Awareness overlay after repeated unlocks (e.g. 5 in 15 min)
- **Weekly & monthly insights** — Local aggregation only (no cloud)
- **Accountability partner** — Daily summary sent via device SMS (user taps send)

### Strict Mode

- **Strict block** — Selected apps fully blocked for the session; emergency exit with confirmation

### Parental Control Mode

- **PIN‑gated parent dashboard** — Configure blocked apps, schedule bands, web filter
- **App blocking & friction** — Block specific apps or require delay before opening
- **Schedule bands** — Free / Limited / Restricted by time of day
- **Web Filter (Phase 4)**  
  - **VPN DNS filtering** — Block domains for all browsers and apps (local VPN, no remote server)  
  - **Domain blacklist** — Manual + category bundles + auto‑blacklist from keyword matches  
  - **Keyword filter** — Match URL bar text (Accessibility), add domain to blacklist; VPN enforces  
  - **Whitelist** — Override blocked domains  
  - **URL visit log** — Parent reviews visited domains; child can request unblock from block page  

---

## Tech Stack

| Layer        | Technology |
|-------------|------------|
| Language    | Kotlin     |
| UI          | Jetpack Compose, Material 3 |
| DI          | Hilt       |
| Database    | Room       |
| Async       | Kotlin Coroutines, Flow |
| Background  | WorkManager (e.g. midnight reset, accountability) |
| Min SDK     | 26 · Target 34 |

---

## Project Structure

```
app/src/main/java/com/pause/app/
├── PauseApplication.kt          # Hilt app, WorkManager config, midnight worker
├── di/                           # Hilt modules, entry points (AS, VPN, Boot)
├── data/
│   ├── db/                       # Room DB, entities, DAOs, type converters
│   └── repository/               # Blacklist, Whitelist, Keyword, Insights, Session, etc.
├── service/
│   ├── PauseAccessibilityService.kt  # Foreground detection, interception, URL reader hook
│   ├── overlay/                  # OverlayManager, Delay/Reflection/Commitment/Strict/Lock overlays
│   └── webfilter/                # PauseVpnService, DNSPacketParser, BlocklistMatcher, etc.
│       └── url/                  # BrowserURLReader, URLClassifier, KeywordMatcher, URLCaptureQueue
├── receiver/                     # BootReceiver (restart VPN / restore state)
├── worker/                       # MidnightResetWorker
└── ui/                           # Compose screens, ViewModels, navigation
```

---

## Getting Started

### Prerequisites

- Android Studio (or compatible IDE)
- JDK 17
- Android SDK 34

### Build & Run

```bash
./gradlew assembleDebug
# Install: adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and run the **app** configuration.

### Permissions (granted in onboarding)

- **Accessibility Service** — Foreground app detection; no screen content reading except URL bar text when Web Filter URL capture is enabled.
- **Display over other apps** — Show delay/reflection/block overlays.
- **Usage access** — Daily allowance and time‑in‑app (optional).
- **VPN** (optional) — For Web Filter; local only, no traffic sent off‑device.

---

## Documentation

| Document | Description |
|----------|-------------|
| [Design.md](docs/Design.md) | Product overview, HLD/LLD, use cases, sequence/flow/state diagrams (Phases 1–3) |
| [PRDv3.md](docs/PRDv3.md) | Phase 4 Web Filter — VPN, URL reader, keyword/blacklist/whitelist, block page |
| [architecture.md](docs/architecture.md) | Implementation architecture and layer overview |
| [accessibility-and-overlays.md](docs/accessibility-and-overlays.md) | Accessibility Service and OverlayManager behavior |
| [webfilter.md](docs/webfilter.md) | Web Filter: VPN DNS engine, URL capture, keyword matching |
| [data-layer.md](docs/data-layer.md) | Room database, repositories, and DAOs |

Other docs in `docs/`: PRD.md, PRDv2.md, play_store_policy.md, BUG_REPORT_PRDV3.md.

---

## Privacy

- **No cloud.** All data stays on device (Room DB, preferences).
- **No account.** No login or sync.
- **Web Filter:** VPN is local only; DNS and URL bar text used only for filtering and visit log; no HTTPS inspection, no page content reading.
- **Accountability:** Summary is sent via the device’s SMS app; user composes and sends the message.

---

## License

See repository for license information.
