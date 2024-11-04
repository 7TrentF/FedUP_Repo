package com.FedUpGroup.fedup_foodwasteapp

import android.content.Context

class AppPreferences private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean("enable_notifications", true)
    }

    fun getNotificationDays(): Int {
        return prefs.getString("notification_timing", "3")?.toIntOrNull() ?: 3
    }

    companion object {
        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}