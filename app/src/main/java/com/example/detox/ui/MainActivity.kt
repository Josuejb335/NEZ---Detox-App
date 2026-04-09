package com.example.detox.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detox.data.AppInfo
import com.example.detox.data.BlockedAppsRepository
import com.example.detox.databinding.ActivityMainBinding
import com.example.detox.service.AppBlockService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainActivity - Entry point UI
 *
 * PURPOSE:
 * - Displays list of installed apps
 * - Shows which apps are blocked
 * - Provides toggle to block/unblock
 * - Guides user to enable required permissions
 *
 * MODIFY THIS:
 * - Add categories/filtering
 * - Add search functionality
 * - Add statistics dashboard
 * - Add schedules/timers
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BlockedAppsRepository
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize
        repository = BlockedAppsRepository(this)
        setupRecyclerView()
        setupButtons()

        // Start foreground service
        startAppBlockService()

        // Check permissions on startup
        checkPermissions()

        // Load apps
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning (in case permissions changed)
        updatePermissionStatus()
    }

    /**
     * Set up the RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = AppListAdapter { appInfo, isChecked ->
            onAppToggled(appInfo, isChecked)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        // Enable Accessibility Service button
        binding.btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        // Enable Overlay permission button
        binding.btnEnableOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        // Refresh button (optional)
        binding.btnRefresh.setOnClickListener {
            loadApps()
        }
    }

    /**
     * Handle app block toggle
     */
    private fun onAppToggled(appInfo: AppInfo, isBlocked: Boolean) {
        if (isBlocked) {
            repository.blockApp(appInfo.packageName)
            Toast.makeText(this, "${appInfo.appName} blocked", Toast.LENGTH_SHORT).show()
        } else {
            repository.unblockApp(appInfo.packageName)
            Toast.makeText(this, "${appInfo.appName} unblocked", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "${appInfo.appName} is now ${if (isBlocked) "blocked" else "unblocked"}")
    }

    /**
     * Load installed apps (excluding system apps)
     * Runs on IO thread to avoid blocking UI
     */
    private fun loadApps() {
        lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    getInstalledApps()
                }
                adapter.submitList(apps)
                Log.d(TAG, "Loaded ${apps.size} apps")

                if (apps.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No apps found. Check QUERY_ALL_PACKAGES permission.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading apps: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Get list of user-installed apps
     * Shows all apps that have a launcher icon (can be opened from home screen)
     */
    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val blockedApps = repository.getBlockedApps()
        val myPackageName = applicationContext.packageName

        // Query apps with MAIN/LAUNCHER intent filter - these show on home screen
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        Log.d(TAG, "Apps with launcher intent: ${resolveInfos.size}")

        // Get unique packages (some apps have multiple activities)
        val uniquePackages = resolveInfos
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != myPackageName }

        return uniquePackages.mapNotNull { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                AppInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    isBlocked = blockedApps.contains(packageName)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not load app info for $packageName")
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Start the foreground service
     */
    private fun startAppBlockService() {
        val intent = Intent(this, AppBlockService::class.java).apply {
            action = AppBlockService.ACTION_START
        }
        startService(intent)
    }

    /**
     * Check and update permission status UI
     */
    private fun checkPermissions() {
        updatePermissionStatus()
    }

    /**
     * Update UI based on permission status
     */
    private fun updatePermissionStatus() {
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasOverlay = Settings.canDrawOverlays(this)

        binding.tvAccessibilityStatus.text =
            "Accessibility: ${if (hasAccessibility) "✓ Enabled" else "✗ Required"}"
        binding.tvOverlayStatus.text =
            "Overlay: ${if (hasOverlay) "✓ Enabled" else "✗ Required"}"

        // Show/hide enable buttons
        binding.btnEnableAccessibility.visibility =
            if (hasAccessibility) android.view.View.GONE else android.view.View.VISIBLE
        binding.btnEnableOverlay.visibility =
            if (hasOverlay) android.view.View.GONE else android.view.View.VISIBLE
    }

    /**
     * Open Accessibility settings
     */
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Enable \"Detox App Blocker\" in Accessibility", Toast.LENGTH_LONG).show()
    }

    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    /**
     * Check if accessibility service is enabled
     * The enabled services list is colon-separated
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // The service can be in different formats:
        // com.example.detox/.service.AppBlockAccessibilityService
        // com.example.detox/com.example.detox.service.AppBlockAccessibilityService
        val myPackageName = applicationContext.packageName
        val shortServiceName = "$myPackageName/.service.AppBlockAccessibilityService"
        val fullServiceName = "$myPackageName/com.example.detox.service.AppBlockAccessibilityService"

        Log.d(TAG, "Checking accessibility service in: $enabledServices")

        // Split by colon as the list is colon-separated
        val services = enabledServices.split(":")
        return services.any { service ->
            service.contains(shortServiceName) || service.contains(fullServiceName) ||
                    service.contains("AppBlockAccessibilityService")
        }
    }
}