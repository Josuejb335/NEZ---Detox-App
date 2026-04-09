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
 * 4. If blocked, show overlay IMMEDIATELY and repeatedly
 */
class AppBlockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppBlockService"

        // Package names to ignore (system apps)
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.settings",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.apps.launcher"
        )

        // Very short cooldown - only for same app
        private const val BLOCK_COOLDOWN_MS = 200L
    }

    private lateinit var repository: BlockedAppsRepository
    private var lastDetectedPackage: String = ""
    private var lastBlockTime: Long = 0
    private var lastBlockedPackage: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val blockRunnable = mutableMapOf<String, Runnable>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected - package: ${applicationContext.packageName}")

        // Initialize repository
        repository = BlockedAppsRepository(this)

        // Configure what events to receive
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 0 // No delay
        }
        setServiceInfo(info)

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
            return
        }

        // Skip system apps
        if (packageName in IGNORED_PACKAGES) {
            return
        }

        // Skip our own app
        if (packageName == applicationContext.packageName) {
            return
        }

        // Only process if it's a new package
        if (packageName == lastDetectedPackage) {
            // Still check if it should be blocked (user might have closed blocker)
            if (shouldBlockApp(packageName)) {
                // Check if app is still in foreground and re-block if needed
                ensureBlocked(packageName)
            }
            return
        }
        lastDetectedPackage = packageName

        Log.d(TAG, "Foreground app changed to: $packageName")

        // Check if app should be blocked
        if (shouldBlockApp(packageName)) {
            Log.d(TAG, "☠☠☠ BLOCKING: $packageName ☠☠☠")
            // Block immediately and aggressively
            blockAppAggressive(packageName)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    /**
     * Determine if app should be blocked
     */
    private fun shouldBlockApp(packageName: String): Boolean {
        return repository.isBlocked(packageName)
    }

    /**
     * Ensure app stays blocked - check if still in foreground
     */
    private fun ensureBlocked(packageName: String) {
        val currentTime = System.currentTimeMillis()
        // Only re-block if enough time passed since last block
        if (currentTime - lastBlockTime > 500 && lastBlockedPackage == packageName) {
            blockAppAggressive(packageName)
        }
    }

    /**
     * Block app aggressively - show overlay AND send to home
     */
    private fun blockAppAggressive(packageName: String) {
        val currentTime = System.currentTimeMillis()

        // Check cooldown - only skip if same app blocked very recently
        if (lastBlockedPackage == packageName &&
            currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            return
        }

        lastBlockTime = currentTime
        lastBlockedPackage = packageName

        // Cancel any pending block for this package
        blockRunnable[packageName]?.let { handler.removeCallbacks(it) }

        // Show overlay IMMEDIATELY
        showBlockOverlay(packageName)

        // Also kill the app process to prevent it from loading
        killAppProcess(packageName)

        // Send to home with minimal delay
        handler.postDelayed({
            sendUserToHome()
        }, 50)

        // Re-check after short delay and block again if needed
        val reblockRunnable = Runnable {
            if (lastDetectedPackage == packageName && shouldBlockApp(packageName)) {
                Log.d(TAG, "Re-blocking $packageName - still detected")
                showBlockOverlay(packageName)
                killAppProcess(packageName)
                sendUserToHome()
            }
        }
        blockRunnable[packageName] = reblockRunnable
        handler.postDelayed(reblockRunnable, 300)
        handler.postDelayed(reblockRunnable, 600)
    }

    /**
     * Show the block overlay
     */
    private fun showBlockOverlay(packageName: String) {
        try {
            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BlockOverlayActivity", e)
        }
    }

    /**
     * Kill the blocked app process
     */
    private fun killAppProcess(packageName: String) {
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            Log.d(TAG, "Killed process: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill process", e)
        }
    }

    /**
     * Send user to home screen
     */
    private fun sendUserToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to home", e)
        }
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