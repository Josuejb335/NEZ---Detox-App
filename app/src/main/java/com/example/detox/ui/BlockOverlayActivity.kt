package com.example.detox.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.example.detox.databinding.ActivityBlockOverlayBinding

/**
 * BlockOverlayActivity - Shows when a blocked app is opened
 *
 * PURPOSE:
 * - Displays full-screen "App Blocked" message
 * - Prevents user from accessing blocked app
 * - User must go back to exit
 *
 * MODIFY THIS:
 * - Add countdown timer before allowing exit
 * - Add motivational quotes
 * - Add "Emergency access" with confirmation
 * - Add usage statistics
 * - Customize theme/colors
 */
class BlockOverlayActivity : Activity() {

    companion object {
        private const val TAG = "BlockOverlay"
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    private lateinit var binding: ActivityBlockOverlayBinding
    private var blockedPackageName: String = ""
    private var canExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get blocked package name before setting content
        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "Unknown"
        Log.d(TAG, "Blocking: $blockedPackageName")

        // Set up window flags IMMEDIATELY before setting content view
        setupWindow()

        binding = ActivityBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up UI
        setupUI()

        // Allow exit after delay (prevents accidental immediate close)
        Handler(Looper.getMainLooper()).postDelayed({
            canExit = true
        }, 500)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Update blocked package if new intent received
        intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE)?.let {
            blockedPackageName = it
            binding.tvPackageName.text = it
            Log.d(TAG, "Updated blocked package: $it")
        }
    }

    /**
     * Configure window to appear above other apps
     * IMPORTANT: These flags must be set before setContentView
     */
    private fun setupWindow() {
        window.apply {
            // Show over lock screen
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

            // Keep screen on and bright
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Full screen
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            // Secure flag - prevents screenshots (optional security)
            // addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Set up the UI elements
     */
    private fun setupUI() {
        // Show which app is blocked
        binding.tvBlockedApp.text = "App Blocked"
        binding.tvPackageName.text = blockedPackageName

        // Close button
        binding.btnClose.setOnClickListener {
            if (canExit) {
                closeBlocker()
            }
        }
    }

    /**
     * Close the blocker and return to home
     */
    private fun closeBlocker() {
        Log.d(TAG, "Closing blocker, sending to home")

        // Go to home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        startActivity(homeIntent)

        // Finish this activity
        finish()
    }

    /**
     * Prevent back button from going to blocked app
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Go home instead of back to blocked app
        closeBlocker()
    }

    /**
     * Block volume and other keys
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Block volume keys and other system keys while showing blocker
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_HOME -> {
                true // Consume these events
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        // Keep the blocker showing - don't finish on pause
        // This was causing the blocker to disappear too quickly
    }

    override fun onStop() {
        super.onStop()
        // Try to keep the blocker active
        // If user navigates away, we want them to come back to this
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BlockOverlayActivity destroyed")
    }
}