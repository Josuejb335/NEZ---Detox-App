package com.example.detox.core

/**
 * Constants file for app-wide configuration
 *
 * MODIFY THIS:
 * - Add feature flags
 * - Add configuration values
 * - Add magic numbers/strings
 */
object Constants {

    // SharedPreferences keys
    const val PREFS_NAME = "detox_prefs"
    const val KEY_BLOCKED_APPS = "blocked_apps"

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "detox_service_channel"
    const val NOTIFICATION_ID = 1

    // Ignored system packages (never block these)
    val IGNORED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.settings",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.launcher"
    )

    // EXTEND: Add more constants
    // - Default block duration
    // - Max apps allowed
    // - Feature flags
}