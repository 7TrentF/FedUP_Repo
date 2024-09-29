package com.example.fedup_foodwasteapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {


    private val authManager = AuthManager.getInstance()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Find the logout preference and set up the click listener
        val logoutPreference = findPreference<Preference>("logout")
        logoutPreference?.setOnPreferenceClickListener {
            // Call the logout method when clicked
            authManager.logoutUser(requireContext())

            true
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        return view
    }




}