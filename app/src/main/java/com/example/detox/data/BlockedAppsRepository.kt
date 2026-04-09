package com.example.detox.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for blocked apps data
 *
 * ARCHITECTURE:
 * - Uses SharedPreferences for simple persistence
 * - No database needed for basic functionality
 * - Stores package names as comma-separated string
 *
 * MODIFY THIS:
 * - Replace with Room database for complex data
 * - Add categories/schedules for blocked apps
 * - Add statistics tracking
 */
class BlockedAppsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "detox_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get all blocked app package names
     */
    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    /**
     * Check if a specific app is blocked
     */
    fun isBlocked(packageName: String): Boolean {
        return getBlockedApps().contains(packageName)
    }

    /**
     * Add an app to blocked list
     */
    fun blockApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.add(packageName)
        saveBlockedApps(current)
    }

    /**
     * Remove an app from blocked list
     */
    fun unblockApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.remove(packageName)
        saveBlockedApps(current)
    }

    /**
     * Toggle block status for an app
     */
    fun toggleBlock(packageName: String): Boolean {
        val isNowBlocked = !isBlocked(packageName)
        if (isNowBlocked) {
            blockApp(packageName)
        } else {
            unblockApp(packageName)
        }
        return isNowBlocked
    }

    /**
     * Clear all blocked apps
     */
    fun clearAll() {
        prefs.edit().remove(KEY_BLOCKED_APPS).apply()
    }

    private fun saveBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, apps).apply()
    }
}