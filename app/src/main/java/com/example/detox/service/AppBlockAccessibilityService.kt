package com.example.detox.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
            "com.android.settings",
            "com.google.android.apps.nexuslauncher"
        )
    }

    private lateinit var repository: BlockedAppsRepository
    private var lastDetectedPackage: String = ""
    private var lastBlockTime: Long = 0
    private val BLOCK_COOLDOWN_MS = 1000 // Prevent rapid re-blocking

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected - package: $packageName")

        // Initialize repository
        repository = BlockedAppsRepository(this)

        // Configure what events to receive
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        // Start foreground service to keep process alive
        startForegroundService()

        // Log current blocked apps
        val blockedApps = repository.getBlockedApps()
        Log.d(TAG, "Currently blocked apps: $blockedApps")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only care about window changes (app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Get package name from event
        val packageName = event.packageName?.toString() ?: run {
            Log.d(TAG, "Event has no package name")
            return
        }

        // Skip if same as last detected
        if (packageName == lastDetectedPackage) return
        lastDetectedPackage = packageName

        // Skip system apps
        if (packageName in IGNORED_PACKAGES) {
            Log.d(TAG, "Ignoring system app: $packageName")
            return
        }

        // Skip our own app - compare to our actual package name
        if (packageName == applicationContext.packageName) {
            Log.d(TAG, "Ignoring self: $packageName")
            return
        }

        Log.d(TAG, "Foreground app detected: $packageName")

        // Check if app should be blocked
        if (shouldBlockApp(packageName)) {
            Log.d(TAG, "☠☠☠ BLOCKING: $packageName ☠☠☠")
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
        val blockedApps = repository.getBlockedApps()
        val isBlocked = repository.isBlocked(packageName)
        Log.d(TAG, "Checking $packageName against blocked list: $blockedApps")
        Log.d(TAG, "Is $packageName blocked? $isBlocked")
        return isBlocked
    }

    /**
     * Trigger the block overlay
     */
    private fun blockApp(packageName: String) {
        // Cooldown check to prevent spam
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            Log.d(TAG, "Block cooldown active, skipping")
            return
        }
        lastBlockTime = currentTime

        Log.d(TAG, "Starting BlockOverlayActivity for: $packageName")

        try {
            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            }
            startActivity(intent)
            Log.d(TAG, "BlockOverlayActivity started successfully")

            // Also send user to home screen to prevent app from showing
            Handler(Looper.getMainLooper()).postDelayed({
                sendUserToHome()
            }, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BlockOverlayActivity", e)
        }
    }

    /**
     * Send user to home screen
     */
    private fun sendUserToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    /**
     * Start foreground service for persistence
     */
    private fun startForegroundService() {
        try {
            val intent = Intent(this, AppBlockService::class.java).apply {
                action = AppBlockService.ACTION_START
            }
            startService(intent)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }
}