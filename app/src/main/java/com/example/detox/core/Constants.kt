package com.example.detox.core

/**
 * App constants
 */
object Constants {

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "detox_service"
    const val NOTIFICATION_ID = 1

    // Service
    const val SERVICE_RESTART_DELAY_MS = 1000L

    // Blocking
    const val BLOCK_RETRIGGER_DELAY_MS = 300L
    const val BLOCK_COOLDOWN_MS = 500L

    // SharedPreferences
    const val PREFS_NAME = "detox_prefs"
    const val KEY_FIRST_LAUNCH = "first_launch"

    // System packages to ignore
    val SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.settings",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.launcher"
    )
}