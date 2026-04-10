package com.example.detox.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detox.data.BlockedAppsRepository
import com.example.detox.core.AppState
import com.example.detox.databinding.ActivityMainBinding
import com.example.detox.service.MonitorForegroundService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity - Entry point
 *
 * Features:
 * - App list with block toggles
 * - Permission status indicators
 * - Accessibility service guidance
 * - Attempt statistics display
 */
class MainActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BlockedAppsRepository
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BlockedAppsRepository(this)

        setupRecyclerView()
        setupButtons()
        startServices()

        checkFirstLaunch()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter { packageName, appName, isBlocked ->
            onAppToggled(packageName, appName, isBlocked)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupButtons() {
        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        binding.btnClearAttempts.setOnClickListener {
            repository.clearAllAttemptCounts()
            Toast.makeText(this, "Attempt counts cleared", Toast.LENGTH_SHORT).show()
            loadApps()
        }
    }

    private fun startServices() {
        try {
            val intent = Intent(this, MonitorForegroundService::class.java)
            startService(intent)
            Log.d(TAG, "Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun checkFirstLaunch() {
        // Show guidance on first launch (TODO: implement)
    }

    private fun onAppToggled(packageName: String, appName: String, isBlocked: Boolean) {
        if (packageName.isBlank()) {
            Log.w(TAG, "Attempted to toggle blank package name")
            return
        }
        
        if (isBlocked) {
            repository.addApp(packageName)
            Toast.makeText(this, "$appName blocked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "App blocked: $packageName")
        } else {
            repository.removeApp(packageName)
            repository.clearAttemptCount(packageName)
            Toast.makeText(this, "$appName unblocked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "App unblocked: $packageName")
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps()
            }
            adapter.submitList(apps)
        }
    }

    private fun getInstalledApps(): List<AppItem> {
        val pm = packageManager
        val blockedApps = repository.getBlockedApps()
        val myPackage = packageName
        
        try {
            // Query launcher apps
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            return resolveInfos
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != myPackage }
                .mapNotNull { pkg ->
                    try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        AppItem(
                            packageName = pkg,
                            appName = pm.getApplicationLabel(info).toString(),
                            icon = pm.getApplicationIcon(info),
                            isBlocked = blockedApps.contains(pkg),
                            attemptCount = repository.getAttemptCount(pkg)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load app info for $pkg", e)
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying installed apps", e)
            return emptyList()
        }
    }

    private fun updatePermissionStatus() {
        val enabled = AppState.isAccessibilityEnabled(this)
        binding.tvStatus.text = if (enabled) {
            "Status: Active ✓"
        } else {
            "Status: Accessibility Required"
        }
        binding.btnEnableAccessibility.visibility =
            if (enabled) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable Detox Monitoring", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
}