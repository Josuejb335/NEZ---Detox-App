package com.example.detox.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.detox.data.BlockedAppsRepository
import com.example.detox.ui.BlockOverlayActivity

/**
 * Accessibility Service - detects foreground app changes
 *
 * CRITICAL COMPONENT:
 * This service detects when user switches between apps
 * and triggers blocking if the app is in blocked list
 *
 * SETUP REQUIRED:
 * User MUST enable this in Settings > Accessibility > Detox App Blocker
 *
 * HOW IT WORKS:
 * 1. System sends window state change events
 * 2. We extract the package name
 * 3. Check if package is in blocked list
 * 4. If blocked, show overlay
 *
 * MODIFY THIS:
 * - Add delay before blocking (grace period)
 * - Add whitelisted time windows
 * - Add usage tracking
 */
class AppBlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockService"

        // Package names to ignore (system apps)
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher"
        )
    }

    private lateinit var repository: BlockedAppsRepository
    private var lastDetectedPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected")

        // Initialize repository
        repository = BlockedAppsRepository(this)

        // Configure what events to receive
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        // Start foreground service to keep process alive
        startForegroundService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only care about window changes (app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Get package name from event
        val packageName = event.packageName?.toString() ?: return

        // Skip if same as last detected
        if (packageName == lastDetectedPackage) return
        lastDetectedPackage = packageName

        // Skip system apps
        if (packageName in IGNORED_PACKAGES) return

        // Skip our own app
        if (packageName == packageName) return

        Log.d(TAG, "Foreground app: $packageName")

        // Check if app should be blocked
        if (shouldBlockApp(packageName)) {
            Log.d(TAG, "Blocking app: $packageName")
            blockApp(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    /**
     * Determine if app should be blocked
     * EXTEND THIS:
     * - Add time-based rules
     * - Add daily usage limits
     * - Add break intervals
     */
    private fun shouldBlockApp(packageName: String): Boolean {
        return repository.isBlocked(packageName)
    }

    /**
     * Trigger the block overlay
     */
    private fun blockApp(packageName: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    /**
     * Start foreground service for persistence
     */
    private fun startForegroundService() {
        val intent = Intent(this, AppBlockService::class.java).apply {
            action = AppBlockService.ACTION_START
        }
        startService(intent)
    }
}