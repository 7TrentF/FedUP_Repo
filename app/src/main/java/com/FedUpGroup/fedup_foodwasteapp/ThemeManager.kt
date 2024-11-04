package com.FedUpGroup.fedup_foodwasteapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val themeKey = "theme_mode"

    fun setTheme(isLightMode: Boolean) {
        // Save the preference
        prefs.edit().putBoolean(themeKey, isLightMode).apply()

        // Apply the theme
        AppCompatDelegate.setDefaultNightMode(
            if (isLightMode) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )
    }

    fun isLightMode(): Boolean {
        return prefs.getBoolean(themeKey, true) // true = light mode by default
    }

    fun applyTheme() {
        // Apply saved theme on app startup
        setTheme(isLightMode())
    }
}