package com.example.fedup_foodwasteapp

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

// BaseFragment.kt
abstract class BaseFragment : Fragment() {
    override fun onAttach(context: Context) {
        val languageCode = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("language_preference", "en") ?: "en"
        super.onAttach(LocaleHelper.wrap(context, languageCode))
    }

    // Helper method to update language from fragment
    protected fun updateLanguage(languageCode: String) {
        activity?.let { activity ->
            PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString("language_preference", languageCode)
                .apply()

            // Recreate the activity to apply new language
            activity.recreate()
        }
    }
}