package com.example.detox.data

import android.graphics.drawable.Drawable

/**
 * Simple data class for app information
 *
 * EXTEND THIS:
 * - Add app usage statistics
 * - Add app category (social, games, etc.)
 * - Add custom block schedules
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false
)