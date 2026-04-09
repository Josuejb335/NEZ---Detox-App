package com.example.detox

import android.app.Application
import android.util.Log

/**
 * Application class - app entry point
 *
 * Initializes global state and services.
 */
class DetoxApplication : Application() {

    companion object {
        private const val TAG = "DetoxApp"
        lateinit var instance: DetoxApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Application started")
    }
}
