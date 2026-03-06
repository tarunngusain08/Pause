# Pause — Product Requirements Document
### Version 1.0 | Android | All Phases

---

## Table of Contents

1. [Overview](#overview)
2. [Problem Statement](#problem-statement)
3. [Product Philosophy](#product-philosophy)
4. [Target Users](#target-users)
5. [Permissions & Feasibility](#permissions--feasibility)
6. [Phased Feature Plan](#phased-feature-plan)
   - [Phase 1 — Awareness & Friction](#phase-1--awareness--friction)
   - [Phase 2 — Control & Reflection](#phase-2--control--reflection)
   - [Phase 3 — Commitment & Accountability](#phase-3--commitment--accountability)
7. [Full User Flows](#full-user-flows)
8. [Technical Architecture](#technical-architecture)
9. [Data & Privacy](#data--privacy)
10. [Non-Goals](#non-goals)
11. [Risk Register](#risk-register)
12. [Success Metrics](#success-metrics)
13. [Development Timeline](#development-timeline)

---

## Overview

**Pause** is a minimal Android application that reduces impulsive phone usage by introducing intentional friction, behavioral reflection, and commitment mechanisms when users open distracting apps.

Unlike aggressive blockers, Pause operates on a graduated intervention model — from gentle friction in Phase 1, to active habit control in Phase 2, to social accountability in Phase 3. Each phase is independently shippable and builds on the last.

**Core Principle:** Users are not locked out. They are asked to *pause*, *reflect*, and *choose* — making every app-open a conscious decision rather than an automatic reflex.

---

## Problem Statement

Modern smartphone usage is dominated by impulsive, unconscious behavior:

- Opening social media without intention
- Habitual unlocking and checking
- Endless scrolling as a default idle state

Existing tools fail because they:

- Block apps completely (too aggressive, high uninstall rate)
- Offer only passive analytics with no behavior change mechanism
- Are complex to configure and easy to bypass
- Provide no accountability or commitment system

**Users need a graduated intervention system** that starts with gentle friction, deepens into active control, and ultimately supports real commitment and accountability.

---

## Product Philosophy

Pause is built on three behavioral design principles:

1. **Friction over blocking** — A well-placed pause is more effective than a hard block. Users who feel controlled uninstall. Users who feel supported improve.

2. **Awareness before restriction** — Users cannot change what they don't notice. Reflection prompts and counters build the self-awareness that makes later restrictions meaningful.

3. **Commitment over willpower** — Long-term behavior change requires commitment devices, not repeated acts of willpower. Phase 3 features are designed around this principle.

---

## Target Users

| Segment | Core Need |
|---|---|
| Social media over-users | Reduce mindless scrolling |
| Students | Protect study sessions |
| Professionals | Avoid distraction during deep work |
| Digital minimalists | Build intentional phone habits |

**Typical monitored apps:** Instagram, YouTube, Reddit, Twitter/X, TikTok, Facebook, Snapchat

---

## Permissions & Feasibility

These are the most critical feasibility concerns for this app. Each permission carries UX and Play Store implications that must be addressed in onboarding.

### Accessibility Service
- **Purpose:** Detect foreground app changes and trigger the delay overlay
- **Feasibility:** Fully supported on Android 26+. However, Google restricts Play Store distribution of apps using Accessibility Service to those with a clearly justified use case. Pause must declare its use explicitly in the Play Store listing.
- **UX Risk:** Users are suspicious of this permission. Mitigation: a dedicated onboarding screen with plain-language explanation of exactly what is and is not monitored.

### Display Over Other Apps (SYSTEM_ALERT_WINDOW)
- **Purpose:** Show the delay/reflection overlay above the target app
- **Feasibility:** Requires explicit user grant via system settings. Supported on all target SDK versions.
- **UX Risk:** Requires redirecting users to system settings. Must be guided step-by-step.

### Usage Stats (UsageStatsManager)
- **Purpose:** Track app launch frequency and time spent per app
- **Feasibility:** Must be granted via Settings > Apps > Special App Access. Cannot be requested via runtime dialog.
- **UX Risk:** Third manual permission step. Group all permission requests into a single onboarding flow to reduce abandonment.

### Notification Permission (Android 13+)
- **Purpose:** Accountability partner alerts, streak updates, daily summaries
- **Feasibility:** Standard runtime permission on Android 13+.

> **Onboarding Rule:** Request all permissions in a single guided flow on first launch, with a clear rationale screen before each. Never request permissions cold.

---

## Phased Feature Plan

---

### Phase 1 — Awareness & Friction

**Theme:** Make the unconscious conscious.

**Estimated Build Time:** 5 days

**Goal:** Users install, configure in under 2 minutes, and immediately experience friction on distracting apps.

---

#### 1.1 — Onboarding Flow

A first-run experience that explains the app's purpose and collects all necessary permissions in one guided sequence.

**Screens:**
1. Welcome — "Pause helps you use your phone on your terms."
2. How it works — Brief 3-step visual explanation
3. Permission: Accessibility Service — with plain-language explanation
4. Permission: Display Over Apps — with system settings redirect guide
5. Permission: Usage Stats — with system settings redirect guide
6. App selection — user picks apps to monitor
7. Done — monitoring begins

**Requirements:**
- Permission explanations must state explicitly: "Pause cannot read your messages, see your passwords, or access personal content."
- If a permission is denied, show a non-blocking warning and allow partial functionality.

---

#### 1.2 — App Selection

Users select which installed apps Pause will monitor.

**Behavior:**
- Display full list of user-installed apps with icons
- Allow multi-select
- Allow editing the list at any time from the home screen

**Requirements:**
- Filter out system apps by default
- Persist selection to local storage
- Show currently monitored count on home screen

---

#### 1.3 — Delay Screen (Core Feature)

When a monitored app is opened, Pause intercepts and displays a delay screen before allowing access.

**Default Behavior:**
```
Take a breath.

Opening Instagram in...

  10

[Cancel]
```

**Design Decisions:**
- Phase 1 has NO "Open Now" / skip button. The only choices are wait or cancel. This maximizes friction and is the whole point of Phase 1.
- The countdown should be visually prominent — large number, centered, calming UI (not alarming).
- Soft background color, not aggressive red.

**Configurable delay:** 5s / 10s / 20s (default: 10s)

**On countdown complete:** App opens automatically. No additional tap required.

---

#### 1.4 — Daily Launch Counter

Tracks and displays how many times each monitored app was opened today.

**Home screen display:**
```
Today's Launches
Instagram    ████░░░░  7
YouTube      ██░░░░░░  3
Reddit       █░░░░░░░  1
```

**Requirements:**
- Reset counter at midnight
- Store rolling 7-day history (used in Phase 2 insights)
- Sourced from UsageStatsManager where possible; fall back to internal counter from Accessibility Service events

---

#### 1.5 — Home Dashboard (Phase 1)

Minimal home screen showing monitored apps and today's launch counts.

```
Pause

Monitored Apps (3)
  📱 Instagram   7 opens today
  📺 YouTube     3 opens today
  💬 Reddit      1 open today

[+ Add / Edit Apps]
```

No heavy analytics. Awareness only.

---

### Phase 2 — Control & Reflection

**Theme:** Give users active levers, not just a mirror.

**Estimated Build Time:** 5–6 additional days

**Prerequisites:** Phase 1 shipped and stable. Usage Stats data collected for at least 7 days provides meaningful baseline.

---

#### 2.1 — Behavioral Reflection Prompt

> **Promoted from Phase 2 hero feature.** This is the single most effective behavior change mechanism in the entire PRD.

Before the delay countdown begins, users are shown a one-tap prompt:

```
Why are you opening Instagram?

  😐 Bored
  🔁 Habit / Not sure
  💬 Replying to someone
  ✅ I have a reason
```

**Design Decisions:**
- Tap is required to proceed — cannot skip
- Response is stored locally for weekly insights
- Prompt appears *before* countdown, so even if user proceeds, they've named the trigger
- "Bored" and "Habit / Not sure" selections add 5 additional seconds to the delay automatically

**Why this works:** Labeling a behavior trigger (Fogg Behavior Model, Atomic Habits) is the first step in breaking the automaticity of the habit loop. The user doesn't need to stop — just to notice.

---

#### 2.2 — Daily Usage Allowance

Users set a daily time budget for monitored apps collectively or per-app.

**Setup:**
```
Daily Allowance
Total: 60 minutes/day
```

**In use:**
```
Daily Allowance
Used: 35 min  ████████░░░░
Remaining: 25 min
```

**When exhausted:**
```
Daily allowance reached.
You've used 60 minutes on distracting apps today.

[Open Anyway]  [I'm Done]
```

"Open Anyway" is available but requires an extra tap — preserving autonomy while adding friction.

**Requirements:**
- Time tracking uses UsageStatsManager
- Allowance resets at midnight
- Widget-ready data structure (for Phase 3 home screen widget)

---

#### 2.3 — App Launch Limits

Users set a maximum number of opens per app per day.

**Setup:**
```
Launch Limits
Instagram     5 per day
YouTube       3 per day
Reddit        Unlimited
```

**When limit reached:**
```
You've opened Instagram 5 times today.
Your limit is 5.

Still want to open it?
[Yes, open]  [No, I'll skip]
```

**Requirements:**
- Launch count sourced from Phase 1 counter
- Limit shown on home dashboard as `4 / 5`
- If both allowance and launch limit are set, the stricter one triggers first

---

#### 2.4 — Focus Mode Sessions

A timed session where monitored apps become harder to access.

**Starting a session:**
```
Start Focus Session
Duration: [25 min] [45 min] [1 hr] [Custom]
```

**During Focus Mode:**
- Delay increases to 20 seconds (regardless of personal setting)
- Reflection prompt is mandatory
- A banner persists in the notification shade: "Focus session: 38 min remaining"

**Interception during Focus Mode:**
```
You're in a Focus Session.
38 minutes remaining.

Opening YouTube now will distract you.

[Go Back]  [Open Anyway]
```

"Open Anyway" is always available — Pause never hard-blocks in Phase 2.

---

#### 2.5 — Streak Tracking

Users build a streak by staying within their daily limits.

**Display:**
```
Focus Streak  🔥 6 days
```

**Streak rules:**
- A day counts if the user did not exceed their launch limit or allowance
- Days where no monitored apps are opened always count
- **Streak Shield:** One grace day per week. If a user breaks limits on one day, the streak is preserved with a shield indicator. This prevents the "what's the point" abandonment loop that hard resets cause.

**On streak break (without shield):**
```
Streak reset.
You were at 6 days.

Fresh start — what's your intention today?
[Set today's focus]
```

The re-entry prompt is mandatory on first open after a break — do not silently reset.

---

#### 2.6 — Weekly Insights

A simple summary screen available every Monday.

```
Last Week

Most opened: Instagram (42 times)
Time on monitored apps: 4h 20m
Most common trigger: Bored (58%)
Peak usage: 9–11pm

Best day: Wednesday (0 limit breaks)
```

**Requirements:**
- Built from locally stored reflection responses + launch counts
- No ML required — simple aggregation
- Tappable insight cards, not a wall of stats

---

#### 2.7 — "Cost of a Scroll" Daily Card

Once per day, on first open of Pause, show a reframing card:

```
Yesterday, you opened Instagram 14 times.
That's about 42 minutes.

In a week: ~5 hours
In a year: ~10 days

What would you do with 10 extra days?
```

**Requirements:**
- Shown once per day only, on app open (not as a notification)
- Calculation: average 3 min per Instagram session × opens
- Dismissible, but not skippable on first daily open

---

#### 2.8 — Home Dashboard (Phase 2)

```
Pause

🔥 Focus Streak: 6 days
⏱ Daily Allowance: 25 min left

Today's Launches
  Instagram   4 / 5
  YouTube     2 / 3
  Reddit      1

[Start Focus Session]
[Weekly Insights]
```

---

### Phase 3 — Commitment & Accountability

**Theme:** Make it harder to break your own rules.

**Estimated Build Time:** 6–7 additional days

**Prerequisites:** Phase 2 stable. Reflection + streak data provides meaningful context for commitment features.

---

#### 3.1 — Commitment Mode

Users can create a session where selected apps are fully blocked for a defined duration.

**Setup:**
```
Commitment Session
Duration: 2 hours
Apps: Instagram, YouTube, Reddit
```

**During session (interception):**
```
You committed to staying focused.
1 hour 14 minutes remaining.

This app is blocked until your session ends.

[Break Commitment]
```

**Requirements:**
- "Break Commitment" initiates a cooldown (see 3.2)
- Session survives app restarts and phone reboots (persisted to RoomDB with AlarmManager)
- Notification channel shows live session timer

---

#### 3.2 — Hard Override Cooldown

Breaking a commitment requires surviving a 90-second countdown (not 60s — research on craving suppression suggests 2–3 minutes is where urges peak and fall; 90s is a practical middle ground).

```
Break Commitment

Are you sure?
Most cravings pass in 90 seconds.

  1:27

[Cancel — Stay Focused]
```

After the countdown, the user must confirm once more:

```
Your session will end.
Your streak will reset.

[Confirm Break]  [Go Back]
```

**Two-step friction prevents nearly all impulsive breaks.**

---

#### 3.3 — Friction Levels Per App

Users can assign a friction level per monitored app.

| Level | Intervention |
|---|---|
| Low | 5s delay only |
| Medium | Reflection prompt + 10s delay |
| High | Reflection prompt + 20s delay + second confirmation tap |
| Commitment | Full block during commitment sessions |

Shown in app settings per app with a simple slider/selector.

---

#### 3.4 — Environmental Modes

Users define named presets that set which apps are monitored and at what friction level.

**Built-in presets (editable):**

*Work Mode:*
- Blocked during Commitment: Instagram, Reddit, Twitter
- Medium friction: YouTube
- Allowed freely: Slack, Gmail, Calendar

*Study Mode:*
- Blocked during Commitment: YouTube, Instagram, TikTok
- Low friction: Reddit

**Switching:**
```
Active Mode: Work Mode
[Switch Mode]
```

Switching a mode takes effect immediately.

---

#### 3.5 — Lock Screen Intervention

After N unlocks in a short window, Pause interrupts with a brief notice.

**Trigger:** 5+ unlocks in 15 minutes (configurable)

**Intervention:**
```
You've unlocked your phone 8 times
in the last 15 minutes.

Take a breath before continuing.

[I'm aware]
```

**Requirements:**
- Tracked via Accessibility Service window events
- Does not block access — awareness only
- Count shown on home dashboard: "23 unlocks today"

---

#### 3.6 — Accountability Partner

Users can designate a trusted contact as an accountability partner.

**Setup:**
- User enters partner's phone number or email
- Partner receives a one-time opt-in notification and must accept

**What gets shared (only if partner has accepted):**
- Daily summary: apps opened, commitment sessions completed or broken
- Streak status

**Partner notification example:**
```
Pause — Tarun's summary (Tuesday)
Focus sessions: 2 completed, 1 broken
Streak: 4 days 🔥
Most opened: Instagram (6 times)
```

**Partner can reply** with a short message (via SMS/email — Pause does not build a messaging system). This keeps the feature lightweight and feasible.

**Requirements:**
- Fully opt-in, explicit consent on both sides
- Summary sent once per day at 9pm (configurable)
- Easy removal of partner from settings at any time
- Pause never shares content — only behavioral counts

---

#### 3.7 — Long-Term Habit Insights

Monthly report available on the first of each month.

```
March — Monthly Report

Time saved vs February: +2h 40m
Most improved: YouTube (−12 opens/week)
Biggest challenge: Instagram (avg 8/day)
Peak distraction window: 9–11pm

Commitment sessions: 14 completed, 3 broken
Best streak this month: 12 days
```

---

#### 3.8 — Home Dashboard (Phase 3)

```
Pause

🔥 Streak: 12 days  [🛡 1 shield available]
⏱ Allowance: 18 min left
🔒 Commitment Session: 1h 12m remaining

Active Mode: Work Mode  [Switch]

Today's Launches
  Instagram   2 / 5
  YouTube     1 / 3

Unlocks today: 14

[View Insights]  [Accountability]
```

---

## Full User Flows

### First Launch

```
Install app
    ↓
Welcome screen
    ↓
Permission: Accessibility Service
    ↓
Permission: Display Over Apps (system redirect)
    ↓
Permission: Usage Stats (system redirect)
    ↓
Select monitored apps
    ↓
Home dashboard — monitoring active
```

### App Interception (Phase 1)

```
User opens Instagram
    ↓
Accessibility Service detects foreground change
    ↓
Overlay displayed: "Take a breath"
    ↓
10-second countdown
    ↓
[Cancel] or countdown completes → app opens
```

### App Interception (Phase 2)

```
User opens Instagram
    ↓
Reflection prompt: "Why are you opening this?"
    ↓
User selects reason (required)
    ↓
Delay countdown (extended if "Bored" / "Habit")
    ↓
[Cancel] or countdown completes → app opens
    ↓
Launch count incremented, reason stored
```

### Commitment Break Attempt (Phase 3)

```
User opens Instagram during Commitment session
    ↓
Full block screen: "You committed to staying focused"
    ↓
User taps "Break Commitment"
    ↓
90-second cooldown with cancel option
    ↓
Second confirmation screen
    ↓
Session ends, streak resets, re-entry prompt shown
```

### Post-Streak-Break Re-entry

```
User opens Pause after breaking streak
    ↓
"Fresh start" screen — streak reset acknowledged
    ↓
"What's your intention today?" prompt
    ↓
User sets a focus or skips
    ↓
Home dashboard
```

---

## Technical Architecture

### Platform
- **Android**, minimum SDK 26 (Android 8.0)
- Language: Kotlin
- Architecture: MVVM + Repository pattern

### Core Components

**AccessibilityService**
- Monitors `TYPE_WINDOW_STATE_CHANGED` events
- Compares foreground package name against monitored apps list
- Triggers overlay on match
- Also tracks unlock events for lock screen intervention

**OverlayManager**
- Uses `SYSTEM_ALERT_WINDOW` permission
- Displays delay/reflection/commitment screens above all apps
- Managed as a system-level window, not an activity

**UsageStatsManager**
- Pulls app usage data for time-spent tracking and daily allowance
- Queried at intervals (not real-time) to avoid battery impact

**Session Manager**
- Manages Focus Mode and Commitment sessions
- Persisted via RoomDB
- AlarmManager ensures sessions survive reboots

**Local Database (RoomDB)**
- Tables: `MonitoredApps`, `LaunchEvents`, `ReflectionResponses`, `Sessions`, `Streaks`, `Preferences`
- No external DB. No sync.

**SharedPreferences**
- Lightweight settings: delay duration, friction levels, allowance limits

**Notification Manager**
- Active session timer (persistent notification)
- Daily summary (once per day at configurable time)
- Accountability summary dispatch

### What NOT to Build
- No custom messaging system for accountability (use SMS/email)
- No backend server in any phase
- No ML model — insights are simple aggregations
- No iOS version

---

## Data & Privacy

All data is stored exclusively on-device. Pause has no server, no account system, and no analytics SDK.

| Data Type | Storage | Retention |
|---|---|---|
| Monitored app list | SharedPreferences | Until user clears |
| Launch events | RoomDB | 90 days rolling |
| Reflection responses | RoomDB | 90 days rolling |
| Session history | RoomDB | 90 days rolling |
| Streak history | RoomDB | Indefinite |
| User preferences | SharedPreferences | Until user clears |

**Accountability partner notifications** are dispatched via the device's default SMS/email client — Pause does not transmit data to any server.

**Play Store privacy declaration:** No data collected. No data shared. No internet permission required.

---

## Non-Goals

These will not be built across any phase:

- iOS version
- Cloud sync or user accounts
- Real-time AI insights or coaching
- Social feed or community features
- Cross-device support
- Chrome extension (may revisit post-Phase 3)
- Parental controls
- Paid subscription model (Phase 1–2)

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Accessibility permission refusal | High | High | Dedicated onboarding screen with plain-language explanation; partial functionality without it |
| Play Store rejection (Accessibility misuse policy) | Medium | High | Declare use case clearly in listing; follow Google's restricted permissions guidelines |
| Users feel controlled and uninstall | Medium | High | All Phase 3 features are opt-in; "Open Anyway" always available in Phases 1–2 |
| Battery drain from Accessibility Service | Medium | Medium | Use event-based detection only (not polling); test on low-end devices |
| Streak-break causing abandonment | High | Medium | Streak Shield mechanic; mandatory re-entry prompt |
| Accountability feature privacy concerns | Low | High | Explicit double opt-in; partner must accept; easy removal |
| 90-second cooldown feeling punitive | Medium | Low | Framed as "most cravings pass in 90 seconds" — science-backed copy |

---

## Success Metrics

### Phase 1
- % of delay screens that result in cancellation (target: >20%)
- D7 retention (target: >40%)
- Permission grant rate during onboarding (target: >60%)

### Phase 2
- Reflection prompt completion rate (target: >80% — it's mandatory)
- % of days users stay within launch limits
- Weekly insight screen opens per user

### Phase 3
- Commitment session completion rate (target: >65%)
- Accountability partner activation rate
- 30-day streak retention
- Reduction in monitored app launches vs Week 1 baseline

---

## Development Timeline

Assumes one experienced Android developer.

### Phase 1 — ~5 days

| Task | Time |
|---|---|
| Project setup, architecture, RoomDB schema | 0.5 day |
| Accessibility Service + foreground detection | 1 day |
| Overlay delay screen (no skip button) | 1 day |
| App selection UI | 0.5 day |
| Launch counter + UsageStats integration | 0.5 day |
| Onboarding permission flow | 0.5 day |
| Testing + Play Store prep | 1 day |

### Phase 2 — ~5–6 days

| Task | Time |
|---|---|
| Reflection prompt (pre-delay) | 1 day |
| Daily allowance tracker | 1 day |
| Launch limits per app | 0.5 day |
| Focus Mode sessions | 1 day |
| Streak tracking + Streak Shield | 0.5 day |
| Weekly insights screen | 0.5 day |
| "Cost of a Scroll" daily card | 0.5 day |
| Testing | 1 day |

### Phase 3 — ~6–7 days

| Task | Time |
|---|---|
| Commitment Mode + session persistence | 1.5 days |
| 90-second override cooldown flow | 0.5 day |
| Environmental modes (presets) | 1 day |
| Lock screen unlock tracking | 0.5 day |
| Accountability partner flow | 1.5 days |
| Monthly insights | 0.5 day |
| Testing + regression | 1 day |

**Total estimated build time: ~17 days across all phases**

---

*Pause — A minimal digital discipline platform.*
*Not a blocker. Not a tracker. A moment of intention before every scroll.*