package com.example.detox.blocking

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat
import com.example.detox.data.BlockedAppsRepository
import com.example.detox.core.AppState
import com.example.detox.core.Constants
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * BlockingManager - Central coordinator for blocking logic
 *
 * Responsibilities:
 * - Decide when to block
 * - Track attempts (for transparency)
 * - Prevent blocking loops
 * - Coordinate with BlockActivity
 * 
 * Thread-safe: Uses ReentrantReadWriteLock for state management
 */
object BlockingManager {

    private const val TAG = "BlockingManager"

    // Track last block time per package to prevent loops
    private val lastBlockTimes = mutableMapOf<String, Long>()
    
    // Lock for thread-safe state management
    private val stateLock = ReentrantReadWriteLock()

    // Currently being blocked package (thread-safe access)
    @Volatile
    var currentlyBlocking: String? = null
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var clearBlockingRunnable: Runnable? = null

    /**
     * Main entry point: check and trigger blocking if needed
     * 
     * @param context Application context
     * @param packageName Package to check and potentially block
     * @return true if app should be blocked
     */
    fun checkAndBlock(context: Context, packageName: String): Boolean {
        // Input validation
        if (packageName.isBlank()) {
            Log.w(TAG, "Blank packageName provided")
            return false
        }

        // Update state
        AppState.currentForegroundPackage = packageName

        Log.d(TAG, "checkAndBlock called for: $packageName")

        // Thread-safe check if already blocking this package
        stateLock.write {
            if (currentlyBlocking == packageName) {
                Log.d(TAG, "Already blocking $packageName")
                return true
            }
        }

        val repository = BlockedAppsRepository(context)
        val blockedApps = repository.getBlockedApps()
        Log.d(TAG, "Blocked apps list: $blockedApps")

        // Check if blocked
        if (!repository.isBlocked(packageName)) {
            Log.d(TAG, "$packageName is NOT blocked")
            return false
        }

        Log.d(TAG, "$packageName IS blocked - proceeding to block")

        // Check cooldown to prevent rapid re-blocking
        val lastBlock = lastBlockTimes[packageName] ?: 0
        val timeSinceLastBlock = System.currentTimeMillis() - lastBlock
        if (timeSinceLastBlock < Constants.BLOCK_COOLDOWN_MS) {
            Log.d(TAG, "Block cooldown active for $packageName (${timeSinceLastBlock}ms)")
            return true
        }

        // Record attempt and trigger block
        val attempts = repository.recordAttempt(packageName)
        Log.d(TAG, "Blocking $packageName (attempt #$attempts)")

        triggerBlockInternal(context, packageName, attempts)
        return true
    }

    /**
     * Public method to trigger blocking for a specific package
     */
    fun triggerBlock(context: Context, packageName: String) {
        if (packageName.isBlank()) {
            Log.w(TAG, "Blank packageName in triggerBlock")
            return
        }
        val repository = BlockedAppsRepository(context)
        val attempts = repository.getAttemptCount(packageName)
        triggerBlockInternal(context, packageName, attempts)
    }

    /**
     * Internal block trigger with cooldown tracking
     * Thread-safe: Handles state updates atomically
     */
    private fun triggerBlockInternal(context: Context, packageName: String, attempts: Int) {
        stateLock.write {
            lastBlockTimes[packageName] = System.currentTimeMillis()
            currentlyBlocking = packageName
            
            // Cancel any pending clear operation
            clearBlockingRunnable?.let { mainHandler.removeCallbacks(it) }
        }

        try {
            val intent = BlockActivity.createIntent(context, packageName, attempts)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BlockActivity", e)
            stateLock.write { currentlyBlocking = null }
            return
        }

        // Schedule clear of currently blocking flag after cooldown
        // This prevents loops while allowing re-blocks after cooldown expires
        val runnable = Runnable {
            stateLock.write {
                if (currentlyBlocking == packageName) {
                    currentlyBlocking = null
                    Log.d(TAG, "Cleared blocking state for $packageName")
                }
            }
        }
        clearBlockingRunnable = runnable
        
        mainHandler.postDelayed(runnable, Constants.BLOCK_COOLDOWN_MS)
    }

    /**
     * Check if an app should be allowed (not blocked)
     */
    fun isAllowed(context: Context, packageName: String): Boolean {
        return !BlockedAppsRepository(context).isBlocked(packageName)
    }

    /**
     * Get attempt count for display
     */
    fun getAttemptCount(context: Context, packageName: String): Int {
        return BlockedAppsRepository(context).getAttemptCount(packageName)
    }

    /**
     * Clear attempt count for a specific app
     */
    fun clearAttemptCount(context: Context, packageName: String) {
        BlockedAppsRepository(context).clearAttemptCount(packageName)
    }

    /**
     * Clear all tracking state (called on app shutdown)
     */
    fun clearState() {
        stateLock.write {
            currentlyBlocking = null
            lastBlockTimes.clear()
        }
        clearBlockingRunnable?.let { mainHandler.removeCallbacks(it) }
        clearBlockingRunnable = null
    }
}