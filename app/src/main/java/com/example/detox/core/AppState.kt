package com.example.detox.core

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * AppState - Central state management and utility functions
 *
 * Provides:
 * - Accessibility service status detection (corrected logic)
 * - Current foreground app tracking
 * - Global app state
 * 
 * Thread-safe: Uses @Volatile for state variables
 */
object AppState {

    private const val TAG = "AppState"

    // Currently detected foreground package
    @Volatile
    var currentForegroundPackage: String = ""

    // Last time accessibility service was active
    @Volatile
    var lastAccessibilityEventTime: Long = 0

    /**
     * Check if accessibility service is enabled
     * 
     * Fixed: Properly parses service format (package/class)
     * Accessibility services are stored as "package/ClassName" in ENABLED_ACCESSIBILITY_SERVICES
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val packageName = context.packageName
            val serviceName = "${packageName}/com.example.detox.service.MonitorAccessibilityService"
            
            // Split by colon and check if our service is in the list
            val services = enabledServices.split(":")
            val isEnabled = services.any { service ->
                // Match exact service name or just package name
                service == serviceName || service.startsWith("$packageName/")
            }
            
            Log.d(TAG, "Accessibility check for $packageName: $isEnabled")
            return isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status", e)
            return false
        }
    }

    /**
     * Log current state for debugging
     */
    fun logState() {
        val timeSinceLastEvent = if (lastAccessibilityEventTime > 0) {
            System.currentTimeMillis() - lastAccessibilityEventTime
        } else {
            -1
        }
        Log.d(TAG, "Current foreground: $currentForegroundPackage")
        Log.d(TAG, "Last event: ${timeSinceLastEvent}ms ago")
    }
}