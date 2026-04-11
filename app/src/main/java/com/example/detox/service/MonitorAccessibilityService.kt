package com.example.detox.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.detox.blocking.BlockingManager
import com.example.detox.core.AppState
import com.example.detox.core.Constants
import com.example.detox.data.EmergencyRepository

/**
 * MonitorAccessibilityService - Detects foreground app changes
 *
 * PURPOSE (for Play Store compliance):
 * This service is used for digital wellbeing purposes - helping users
 * reduce screen time by temporarily blocking distracting apps.
 *
 * WHAT IT DOES:
 * - Detects when user switches between apps
 * - Checks if the newly opened app is in user's block list
 * - Triggers a blocking screen if needed
 *
 * WHAT IT DOES NOT DO:
 * - Does NOT collect personal data
 * - Does NOT transmit information externally
 * - Does NOT prevent uninstall
 * - Does NOT hide itself or behave maliciously
 *
 * User has full control and can disable at any time in Settings.
 */
class MonitorAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MonitorAccessibility"
    }

    private var lastPackageName: String = ""
    private var emergencyBypassPackage: String? = null
    private var emergencyBypassUntilMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        // Configure service to only listen to window state changes
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        setServiceInfo(info)

        // Start foreground service for persistence
        startForegroundService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            // Only handle window state changes (app switches)
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                return
            }

            // Get package name safely
            val packageName = event.packageName?.toString() ?: return

            // Validate package name
            if (packageName.isBlank()) {
                Log.w(TAG, "Received blank package name")
                return
            }

            // Skip if same as last
            if (packageName == lastPackageName) return
            lastPackageName = packageName

            // Update state
            AppState.currentForegroundPackage = packageName
            AppState.lastAccessibilityEventTime = System.currentTimeMillis()

            // Skip system packages
            if (isSystemPackage(packageName)) {
                Log.d(TAG, "Skipping system package: $packageName")
                return
            }

            // Skip our own app
            if (packageName == applicationContext.packageName) {
                Log.d(TAG, "Skipping own package")
                return
            }

            val now = System.currentTimeMillis()
            if (packageName == emergencyBypassPackage && now < emergencyBypassUntilMs) {
                return
            }

            val emergencyRepository = EmergencyRepository(this)
            if (emergencyRepository.isEmergencyActiveFor(packageName)) {
                // Debounce accessibility re-triggers right after emergency unlock activation.
                emergencyBypassPackage = packageName
                emergencyBypassUntilMs = now + 300L
                Log.d(TAG, "Skipping block for active emergency unlock: $packageName")
                return
            }

            Log.d(TAG, "App changed to: $packageName")

            // Check if should block
            val shouldBlock = BlockingManager.checkAndBlock(this, packageName)
            Log.d(TAG, "Block decision: $shouldBlock")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    /**
     * Check if package is a system package we should ignore
     */
    private fun isSystemPackage(packageName: String): Boolean {
        return Constants.SYSTEM_PACKAGES.any { packageName.startsWith(it) }
    }

    /**
     * Start foreground service for persistence
     * Handles API level differences safely
     */
    private fun startForegroundService() {
        try {
            val intent = Intent(this, MonitorForegroundService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                startService(intent)
            }
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }
}
