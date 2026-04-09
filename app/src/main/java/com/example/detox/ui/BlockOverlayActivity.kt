package com.example.detox.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get blocked package name
        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "Unknown"
        Log.d(TAG, "Blocking: $blockedPackageName")

        // Set up window flags for overlay behavior
        setupWindow()

        // Set up UI
        setupUI()
    }

    /**
     * Configure window to appear above other apps
     */
    private fun setupWindow() {
        window.apply {
            // Make it show over lock screen
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

            // Full screen, no status bar
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
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
            closeBlocker()
        }

        // EXTEND: Add more buttons here
        // - "Take a break" (temporary unblock)
        // - "View stats"
        // - "Emergency access"
    }

    /**
     * Close the blocker and return to home
     */
    private fun closeBlocker() {
        // Go to home screen instead of back (which would go to blocked app)
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(homeIntent)
        finish()
    }

    /**
     * Prevent back button from going to blocked app
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Go home instead of back
        closeBlocker()
    }

    override fun onPause() {
        super.onPause()
        // If user tries to switch away, close this activity
        // This prevents the blocked app from showing
        finish()
    }
}