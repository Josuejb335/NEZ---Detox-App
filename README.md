# Detox App Blocker

A minimal, clean Android app blocker built with Kotlin. Build in progress

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
## Architecture

- **MVVM**: Light MVVM with ViewModel support
- **Repository Pattern**: Simple data layer
- **Services**: Accessibility + Foreground service
- **Minimal**: No unnecessary abstractions
