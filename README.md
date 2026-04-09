# Detox App Blocker

A minimal, clean Android app blocker built with Kotlin. This is a starting point for building a complete detox/app blocking application.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml           # App configuration & permissions
├── java/com/example/detox/
│   ├── DetoxApplication.kt       # Application entry point
│   ├── data/
│   │   ├── AppInfo.kt            # Data class for app info
│   │   └── BlockedAppsRepository.kt  # Simple SharedPreferences storage
│   ├── service/
│   │   ├── AppBlockAccessibilityService.kt  # Detects foreground apps
│   │   └── AppBlockService.kt    # Foreground service for persistence
│   └── ui/
│       ├── MainActivity.kt       # Main UI with app list
│       ├── BlockOverlayActivity.kt   # "App Blocked" screen
│       └── AppListAdapter.kt     # RecyclerView adapter
└── res/
    ├── layout/                   # UI layouts
    ├── values/                   # Colors, strings, themes
    └── xml/accessibility_service_config.xml
```

## Setup Instructions

1. **Build and Install**
   ```bash
   ./gradlew installDebug
   ```

2. **Enable Accessibility Service** (REQUIRED)
   - Go to Settings → Accessibility → Detox App Blocker
   - Turn ON the service
   - This is required to detect when apps are opened

3. **Grant Overlay Permission** (REQUIRED)
   - When prompted, or go to:
   - Settings → Apps → Detox → Advanced → Draw over other apps
   - Enable it

4. **Disable Battery Optimization** (RECOMMENDED)
   - Settings → Apps → Detox → Battery → Unrestricted

## Core Features

- **App List**: Shows installed apps with block toggle
- **Block List**: Simple SharedPreferences storage
- **Foreground Detection**: Accessibility service detects app switches
- **Block Overlay**: Full-screen "App Blocked" screen
- **Persistent Service**: Keeps app running in background

## Architecture

- **MVVM**: Light MVVM with ViewModel support
- **Repository Pattern**: Simple data layer
- **Services**: Accessibility + Foreground service
- **Minimal**: No unnecessary abstractions

## Extending the App

### Add Scheduled Blocking
```kotlin
// In BlockedAppsRepository
fun isBlocked(packageName: String, time: LocalTime): Boolean {
    // Check if blocked at current time
}
```

### Add Usage Statistics
```kotlin
// Use UsageStatsManager to track app usage
val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE)
```

### Add Emergency Access
```kotlin
// In BlockOverlayActivity
private fun showEmergencyAccess() {
    // Require confirmation code
}
```

## Files to Modify

| File | Purpose |
|------|---------|
| `BlockedAppsRepository.kt` | Add data fields (schedules, categories) |
| `AppBlockAccessibilityService.kt` | Add detection logic |
| `BlockOverlayActivity.kt` | Customize block screen |
| `MainActivity.kt` | Add UI features |

## Permissions Explained

- `FOREGROUND_SERVICE`: Keep app alive
- `SYSTEM_ALERT_WINDOW`: Show block overlay
- `PACKAGE_USAGE_STATS`: (Optional) Get app usage
- `BIND_ACCESSIBILITY_SERVICE`: Detect foreground app

## License

MIT - Use as a starting point for your own app blocker.