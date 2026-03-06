# Play Store Policy & Permission Rationale

## Accessibility Service Justification

Pause uses the Android Accessibility Service for a single, narrowly scoped purpose:

**Purpose:** Detect when the user opens a monitored app (e.g., Instagram, YouTube) so that Pause can display a brief pause screen before the app becomes fully visible.

**What Pause does:**
- Listens only to `TYPE_WINDOW_STATE_CHANGED` events
- Extracts the foreground application package name
- Compares it against a user-configured list of monitored apps
- If there is a match, displays an overlay with a countdown

**What Pause does NOT do:**
- Does not read screen content
- Does not capture text, passwords, or personal messages
- Does not use `canRetrieveWindowContent`
- Does not access any content within other applications

**User benefit:** Users who struggle with impulsive phone use get a moment of intention before opening distracting apps. The friction helps build awareness and reduce mindless scrolling.

## Privacy Declaration

- **Data collected:** None
- **Data shared:** None
- **Internet permission:** Not required. Pause works entirely offline.
- **Accountability feature:** Daily summaries are shared via the device's SMS app. The user manually sends the message. Pause does not transmit data to any server.

## Permission Rationale (for in-app display)

### Accessibility Service
"Pause needs this permission to detect when you open a monitored app, so it can show a brief pause screen. Pause cannot read your messages, see your passwords, or access personal content."

### Display Over Other Apps
"Pause needs this permission to show the pause screen above other apps when you open them."

### Usage Stats
"Pause uses this to track how much time you spend on monitored apps and to show daily launch counts. This helps you see your usage patterns."

### Notifications (Android 13+)
"Pause uses notifications to show active focus and commitment session timers."
