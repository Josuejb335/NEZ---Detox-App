package com.example.detox.ui

import android.app.Activity
import android.app.ActivityManager
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
 * - Aggressively keeps blocking until user gives up
 */
class BlockOverlayActivity : Activity() {

    companion object {
        private const val TAG = "BlockOverlay"
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    private lateinit var binding: ActivityBlockOverlayBinding
    private var blockedPackageName: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var checkCount = 0
    private val maxChecks = 50 // Check for ~5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get blocked package name
        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "Unknown"
        Log.d(TAG, "Blocking: $blockedPackageName")

        // Set up window flags IMMEDIATELY
        setupWindow()

        binding = ActivityBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        // Start aggressive monitoring
        startAggressiveBlocking()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE)?.let {
            blockedPackageName = it
            binding.tvPackageName.text = it
        }
    }

    /**
     * Configure window to appear above other apps
     */
    private fun setupWindow() {
        window.apply {
            // Show over lock screen
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)

            // Keep screen on
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Full screen
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    /**
     * Set up the UI
     */
    private fun setupUI() {
        binding.tvBlockedApp.text = "App Blocked"
        binding.tvPackageName.text = blockedPackageName

        binding.btnClose.setOnClickListener {
            closeBlocker()
        }
    }

    /**
     * Aggressively monitor and keep blocking
     */
    private fun startAggressiveBlocking() {
        // Initial kill and home
        killBlockedApp()
        goHome()

        // Keep checking and killing for a few seconds
        val checkRunnable = object : Runnable {
            override fun run() {
                checkCount++
                if (checkCount < maxChecks) {
                    killBlockedApp()
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.postDelayed(checkRunnable, 100)
    }

    /**
     * Kill the blocked app process
     */
    private fun killBlockedApp() {
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(blockedPackageName)
            Log.d(TAG, "Killed: $blockedPackageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill process", e)
        }
    }

    /**
     * Send to home screen
     */
    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home", e)
        }
    }

    /**
     * Close the blocker
     */
    private fun closeBlocker() {
        // Final kills before closing
        killBlockedApp()
        goHome()

        handler.removeCallbacksAndMessages(null)
        finish()
    }

    /**
     * Prevent back button
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Don't allow back - force home
        goHome()
    }

    /**
     * Block volume keys
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        // If user somehow pauses us, keep killing the blocked app
        killBlockedApp()
    }

    override fun onStop() {
        super.onStop()
        killBlockedApp()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "BlockOverlayActivity destroyed")
    }
}