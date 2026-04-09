package com.example.detox.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for blocked apps storage
 *
 * Uses SharedPreferences for simplicity and reliability.
 * Thread-safe for basic operations.
 */
class BlockedAppsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "detox_blocked_apps"
        private const val KEY_BLOCKED_SET = "blocked_set"
        private const val KEY_ATTEMPT_COUNT = "attempt_count_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Add app to blocked list
     */
    fun addApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.add(packageName)
        saveBlockedApps(current)
    }

    /**
     * Remove app from blocked list
     */
    fun removeApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.remove(packageName)
        saveBlockedApps(current)
    }

    /**
     * Check if app is blocked
     */
    fun isBlocked(packageName: String): Boolean {
        return getBlockedApps().contains(packageName)
    }

    /**
     * Get all blocked app package names
     */
    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_SET, emptySet())?.toSet() ?: emptySet()
    }

    /**
     * Clear all blocked apps
     */
    fun clearAll() {
        prefs.edit().remove(KEY_BLOCKED_SET).apply()
    }

    /**
     * Record an attempt to open a blocked app
     * Returns the new attempt count for this app
     */
    fun recordAttempt(packageName: String): Int {
        val key = KEY_ATTEMPT_COUNT + packageName
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()
        return count
    }

    /**
     * Get attempt count for an app
     */
    fun getAttemptCount(packageName: String): Int {
        return prefs.getInt(KEY_ATTEMPT_COUNT + packageName, 0)
    }

    /**
     * Clear attempt count for an app
     */
    fun clearAttemptCount(packageName: String) {
        prefs.edit().remove(KEY_ATTEMPT_COUNT + packageName).apply()
    }

    /**
     * Clear all attempt counts
     */
    fun clearAllAttemptCounts() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_ATTEMPT_COUNT) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private fun saveBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_SET, apps).apply()
    }
}