package com.example.fedup_foodwasteapp

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsFragment : PreferenceFragmentCompat() {
    private val authManager: AuthManager by lazy { AuthManager.getInstance() }
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser ?: return

        // Logout preference
        val logoutPreference = findPreference<Preference>("logout")
        logoutPreference?.setOnPreferenceClickListener {
            authManager.logoutUser(requireContext())
            Log.d("SettingsFragment", "User logged out.")
            true
        }

        // Email (Username) change
        val usernamePreference: EditTextPreference? = findPreference("user_name")
        usernamePreference?.summary = currentUser.email
        usernamePreference?.setOnPreferenceChangeListener { _, newValue ->
            val newEmail = newValue.toString()
            Log.d("SettingsFragment", "Attempting to change email to: $newEmail")
            reauthenticateUser {
                updateEmail(newEmail)
            }
            true
        }

        // Password change
        val changePasswordPreference: Preference? = findPreference("change_password")
        changePasswordPreference?.setOnPreferenceClickListener {
            promptNewPassword { newPassword ->
                Log.d("SettingsFragment", "Attempting to change password.")
                reauthenticateUser {
                    updatePassword(newPassword)
                }
            }
            true
        }

        // Biometric authentication preference
        val biometricPreference: SwitchPreferenceCompat? = findPreference("enable_biometrics") // Updated key
        biometricPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                enableBiometricAuthentication()
            } else {
                disableBiometricAuthentication()
            }
            true
        }
    }


    private fun enableBiometricAuthentication() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt { success ->
                    if (success) {
                        saveBiometricEnabledState(true)
                        Toast.makeText(requireContext(), "Biometric authentication enabled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), "No biometric hardware available.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), "Biometric hardware unavailable.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                }
                startActivity(enrollIntent)
            }
        }
    }

    // Save biometric authentication preference in SharedPreferences
    private fun saveBiometricEnabledState(isEnabled: Boolean) {
        val preferences = requireContext().getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            preferences.edit().putBoolean("biometric_enabled_$userId", isEnabled).apply()
        }
    }



    private fun disableBiometricAuthentication() {
        val preferences = requireContext().getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            preferences.edit().remove("biometric_enabled_$userId").apply()
        }
        Toast.makeText(requireContext(), "Biometric authentication disabled.", Toast.LENGTH_SHORT).show()
    }
    private fun showBiometricPrompt(onAuthenticationSuccess: (Boolean) -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Authentication")
            .setSubtitle("Use your biometrics to log in for enhanced security.")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Biometric authentication enabled", Toast.LENGTH_SHORT).show()
                    onAuthenticationSuccess(true)  // Notify success
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                    onAuthenticationSuccess(false)  // Notify failure
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                    onAuthenticationSuccess(false)  // Notify failure
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }


    private fun updateEmail(newEmail: String) {
        currentUser.updateEmail(newEmail).addOnCompleteListener { task ->
            val view = view ?: return@addOnCompleteListener
            if (task.isSuccessful) {
                Log.d("SettingsFragment", "Email updated to: $newEmail")
                Snackbar.make(view, "Email updated successfully.", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(view, "Failed to update email: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePassword(newPassword: String) {
        currentUser.updatePassword(newPassword).addOnCompleteListener { task ->
            val view = view ?: return@addOnCompleteListener
            if (task.isSuccessful) {
                Snackbar.make(view, "Password updated successfully.", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(view, "Failed to update password: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptCurrentPassword(onPasswordReceived: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null)
        val passwordInput: TextInputEditText = dialogView.findViewById(R.id.password_input)

        AlertDialog.Builder(requireContext())
            .setTitle("Re-authenticate")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    onPasswordReceived(password)
                } else {
                    Snackbar.make(dialogView, "Password cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptNewPassword(onPasswordReceived: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null)
        val passwordInput: TextInputEditText = dialogView.findViewById(R.id.password_input)
        val togglePasswordVisibility: CheckBox = dialogView.findViewById(R.id.toggle_password_visibility)

        togglePasswordVisibility.setOnCheckedChangeListener { _, isChecked ->
            passwordInput.inputType = if (isChecked) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(requireContext())
            .setTitle("New Password")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    onPasswordReceived(password)
                } else {
                    Snackbar.make(dialogView, "Password cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reauthenticateUser(onSuccess: () -> Unit) {
        promptCurrentPassword { currentPassword ->
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    Snackbar.make(view ?: return@addOnCompleteListener, "Re-authentication failed.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
