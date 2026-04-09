package com.example.detox

import android.app.Application

/**
 * Application class - entry point of the app
 *
 * PURPOSE:
 * - Initialize app-wide dependencies
 * - Set up data repository
 * - Single instance of shared resources
 *
 * EXTEND THIS:
 * - Add dependency injection (Hilt/Koin)
 * - Initialize analytics/crash reporting
 * - Set up logging frameworks
 */
class DetoxApplication : Application() {

    companion object {
        // Simple singleton pattern - no DI framework needed for minimal setup
        lateinit var instance: DetoxApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // EXTEND: Add initialization here
        // - Crash reporting
        // - Logging
        // - Dependency injection
    }
}