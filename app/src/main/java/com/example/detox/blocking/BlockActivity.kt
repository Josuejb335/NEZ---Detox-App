package com.example.detox.blocking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.detox.core.Constants
import com.example.detox.data.BlockedAppsRepository

/**
 * BlockActivity - Full-screen blocking UI
 *
 * Displays when user attempts to open a blocked app.
 * Transparent, reversible, and user-friendly.
 *
 * Features:
 * - Shows blocked app name
 * - Provides exit to home screen
 * - Re-triggers if closed while app still blocked
 * - Shows attempt count (transparency)
 */
class BlockActivity : Activity() {

    companion object {
        private const val TAG = "BlockActivity"
        private const val EXTRA_PACKAGE = "blocked_package"
        private const val EXTRA_ATTEMPTS = "attempt_count"

        /**
         * Create launch intent for BlockActivity
         */
        fun createIntent(context: android.content.Context, packageName: String, attempts: Int): Intent {
            return Intent(context, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(EXTRA_PACKAGE, packageName)
                putExtra(EXTRA_ATTEMPTS, attempts)
            }
        }
    }

    private lateinit var repository: BlockedAppsRepository
    private var blockedPackage: String = ""
    private var attemptCount: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isClosing = false
    private var reTriggerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = BlockedAppsRepository(this)
        blockedPackage = intent?.getStringExtra(EXTRA_PACKAGE) ?: ""
        attemptCount = intent?.getIntExtra(EXTRA_ATTEMPTS, 0) ?: 0

        if (blockedPackage.isEmpty()) {
            Log.e(TAG, "No package provided, closing")
            finish()
            return
        }

        Log.d(TAG, "Blocking $blockedPackage (attempt #$attemptCount)")

        // Setup UI BEFORE any transitions
        setupSimpleUi()

        // Send to home immediately to hide blocked app
        goHome()
        
        // Provide haptic feedback to user
        @Suppress("DEPRECATION")
        window?.decorView?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Minimal programmatic UI
     */
    private fun setupSimpleUi() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // App name text - safely get label
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(blockedPackage, 0)
            ).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get app label for $blockedPackage", e)
            blockedPackage
        }

        // Title
        val title = android.widget.TextView(this).apply {
            text = "📱 App Blocked"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // App name
        val appLabel = android.widget.TextView(this).apply {
            text = appName
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // Attempt count (transparency)
        val attemptText = android.widget.TextView(this).apply {
            text = "You've tried to open this $attemptCount time${if (attemptCount != 1) "s" else ""}"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 0, 0, 60)
            visibility = if (attemptCount > 0) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Exit button
        val exitButton = android.widget.Button(this).apply {
            text = "Go Back"
            setOnClickListener { onUserExit() }
        }

        // Take a break button (optional feature)
        val breakButton = android.widget.Button(this).apply {
            text = "Take a 5-min Break"
            setOnClickListener { onTakeBreak() }
            setPadding(0, 20, 0, 0)
        }

        layout.addView(title)
        layout.addView(appLabel)
        layout.addView(attemptText)
        layout.addView(exitButton)
        layout.addView(breakButton)

        // Set content BEFORE transitions
        setContentView(layout)
    }

    /**
     * User clicked exit - close and go home
     */
    private fun onUserExit() {
        isClosing = true
        goHome()
        finish()
    }

    /**
     * User wants a temporary break (TODO: implement timer-based unblock)
     */
    private fun onTakeBreak() {
        isClosing = true
        goHome()
        finish()
    }

    /**
     * Send user to home screen with exception handling
     */
    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch home screen", e)
            // Fallback: just finish the activity
            finish()
        }
    }

    /**
     * Check if we should re-trigger blocking
     * Called when user tries to dismiss
     */
    private fun shouldReTrigger(): Boolean {
        return !isClosing && repository.isBlocked(blockedPackage)
    }

    override fun onBackPressed() {
        // Go home instead of back
        onUserExit()
    }

    override fun onPause() {
        super.onPause()
        
        // If not user-initiated close, check if we should re-trigger
        if (!isClosing && repository.isBlocked(blockedPackage)) {
            Log.d(TAG, "Activity paused, checking re-trigger for $blockedPackage")
            
            val runnable = Runnable {
                if (!isFinishing && !isDestroyed) {
                    try {
                        BlockingManager.triggerBlock(this@BlockActivity, blockedPackage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to re-trigger block", e)
                    }
                }
            }
            reTriggerRunnable = runnable
            handler.postDelayed(runnable, Constants.BLOCK_RETRIGGER_DELAY_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up pending callbacks
        reTriggerRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
    }
}