package com.example.detox.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Repository for blocked apps storage
 *
 * Uses SharedPreferences for simplicity and reliability.
 * Thread-safe for basic operations.
 * Implements daily attempt count reset for accurate stats.
 */
class BlockedAppsRepository(context: Context) {

    companion object {
        private const val TAG = "BlockedAppsRepository"
        private const val PREFS_NAME = "detox_blocked_apps"
        private const val KEY_BLOCKED_SET = "blocked_set"
        private const val KEY_ATTEMPT_COUNT = "attempt_count_"
        private const val KEY_ATTEMPT_DATE = "attempt_date_"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Check if daily reset is needed
        performDailyResetIfNeeded()
    }

    /**
     * Add app to blocked list
     */
    fun addApp(packageName: String) {
        if (packageName.isBlank()) {
            Log.w(TAG, "Attempted to add blank package name")
            return
        }
        val current = getBlockedApps().toMutableSet()
        current.add(packageName)
        saveBlockedApps(current)
        Log.d(TAG, "Added app to block list: $packageName")
    }

    /**
     * Remove app from blocked list
     */
    fun removeApp(packageName: String) {
        if (packageName.isBlank()) {
            Log.w(TAG, "Attempted to remove blank package name")
            return
        }
        val current = getBlockedApps().toMutableSet()
        current.remove(packageName)
        saveBlockedApps(current)
        Log.d(TAG, "Removed app from block list: $packageName")
    }

    /**
     * Check if app is blocked
     */
    fun isBlocked(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return getBlockedApps().contains(packageName)
    }

    /**
     * Get all blocked app package names
     */
    fun getBlockedApps(): Set<String> {
        val result = prefs.getStringSet(KEY_BLOCKED_SET, emptySet())
        return result?.toSet() ?: emptySet()
    }

    /**
     * Clear all blocked apps
     */
    fun clearAll() {
        prefs.edit().remove(KEY_BLOCKED_SET).apply()
        Log.d(TAG, "Cleared all blocked apps")
    }

    /**
     * Record an attempt to open a blocked app
     * Returns the new attempt count for this app (today)
     */
    fun recordAttempt(packageName: String): Int {
        if (packageName.isBlank()) {
            Log.w(TAG, "Attempted to record attempt for blank package")
            return 0
        }
        
        // Check if needs daily reset first
        performDailyResetIfNeeded()
        
        val countKey = KEY_ATTEMPT_COUNT + packageName
        val count = prefs.getInt(countKey, 0) + 1
        
        val editor = prefs.edit()
        editor.putInt(countKey, count)
        editor.putLong(KEY_ATTEMPT_DATE + packageName, System.currentTimeMillis())
        editor.apply()
        
        Log.d(TAG, "Recorded attempt for $packageName: $count")
        return count
    }

    /**
     * Get attempt count for an app (only counts attempts from today)
     */
    fun getAttemptCount(packageName: String): Int {
        if (packageName.isBlank()) return 0
        
        // Reset if needed before returning
        performDailyResetIfNeeded()
        
        return prefs.getInt(KEY_ATTEMPT_COUNT + packageName, 0)
    }

    /**
     * Clear attempt count for a specific app
     */
    fun clearAttemptCount(packageName: String) {
        if (packageName.isBlank()) return
        
        val editor = prefs.edit()
        editor.remove(KEY_ATTEMPT_COUNT + packageName)
        editor.remove(KEY_ATTEMPT_DATE + packageName)
        editor.apply()
        
        Log.d(TAG, "Cleared attempt count for $packageName")
    }

    /**
     * Clear all attempt counts
     */
    fun clearAllAttemptCounts() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_ATTEMPT_COUNT) }
            .forEach { editor.remove(it) }
        prefs.all.keys
            .filter { it.startsWith(KEY_ATTEMPT_DATE) }
            .forEach { editor.remove(it) }
        editor.apply()
        
        Log.d(TAG, "Cleared all attempt counts")
    }

    /**
     * Perform daily reset of attempt counts if 24 hours have passed
     * This ensures attempt counts are accurate and relevant to "today"
     */
    private fun performDailyResetIfNeeded() {
        val lastResetTime = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        val now = System.currentTimeMillis()
        val dayInMs = TimeUnit.DAYS.toMillis(1)
        
        if (now - lastResetTime > dayInMs) {
            Log.d(TAG, "Performing daily reset of attempt counts")
            clearAllAttemptCounts()
            prefs.edit().putLong(KEY_LAST_RESET_DATE, now).apply()
        }
    }

    /**
     * Check if attempt count is from today
     */
    private fun isAttemptFromToday(packageName: String): Boolean {
        val lastAttemptTime = prefs.getLong(KEY_ATTEMPT_DATE + packageName, 0)
        if (lastAttemptTime == 0L) return false
        
        val now = System.currentTimeMillis()
        val dayInMs = TimeUnit.DAYS.toMillis(1)
        return now - lastAttemptTime < dayInMs
    }

    private fun saveBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_SET, apps).apply()
    }
}