package com.FedUpGroup.fedup_foodwasteapp

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.material.textfield.TextInputLayout
import androidx.preference.*
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.work.WorkManager

import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import android.provider.Settings
import java.util.concurrent.TimeUnit
class SettingsFragment : PreferenceFragmentCompat() {

    private val authManager: AuthManager by lazy { AuthManager.getInstance() }
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private val appPreferences by lazy { AppPreferences.getInstance(requireContext()) }
    private lateinit var themeManager: ThemeManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        themeManager = ThemeManager(requireContext())

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser ?: return

        // Logout preference
        val logoutPreference = findPreference<Preference>("logout")
        logoutPreference?.setOnPreferenceClickListener {
            authManager.logoutUser(requireContext())
            true
        }

        val themeSwitch = findPreference<SwitchPreference>("theme_mode")
        themeSwitch?.isChecked = themeManager.isLightMode()
        themeSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val isLightMode = newValue as Boolean
            themeManager.setTheme(isLightMode)
            true
        }

        // Language Preference
        val languagePreference = findPreference<ListPreference>("language_preference")
        languagePreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val newLanguageCode = newValue as String
            setLocale(requireContext(), newLanguageCode)
            requireActivity().recreate() // Restart to apply new language
            true
        }

        setupNotificationPreferences()

        val timingPreference: ListPreference? = findPreference("notification_timing")
        timingPreference?.setOnPreferenceChangeListener { _, newValue ->
            val days = newValue.toString()
            // Save to SharedPreferences
            requireContext().getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("notification_timing", days)
                .apply()
            true
        }

        // Email (Username) change
        val usernamePreference: EditTextPreference? = findPreference("user_name")
        usernamePreference?.summary = currentUser.email
        usernamePreference?.setOnPreferenceChangeListener { _, newValue ->
            val newEmail = newValue.toString()
            reauthenticateUser {
                updateEmail(newEmail)
            }
            true
        }

        // Password change
        val changePasswordPreference: Preference? = findPreference("change_password")
        changePasswordPreference?.setOnPreferenceClickListener {
            promptNewPassword { newPassword ->
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
        currentUser.updateEmail(newEmail)
            .addOnCompleteListener { task ->
                val view = view ?: return@addOnCompleteListener
                if (task.isSuccessful) {
                    Snackbar.make(view, "Email updated successfully.", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(view, "Failed to update email: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun setLocale(context: Context, languageCode: String) {
        val wrappedContext = LocaleHelper.wrap(context, languageCode)
        requireActivity().apply {
            finish()
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            overridePendingTransition(0, 0)  // Avoid transition flash on restart
        }
    }

    private fun updatePassword(newPassword: String) {
        currentUser.updatePassword(newPassword)
            .addOnCompleteListener { task ->
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
        val passwordInputLayout: TextInputLayout = dialogView.findViewById(R.id.password_input_layout)
        val passwordInput: TextInputEditText = dialogView.findViewById(R.id.password_input)

        val passwordDialog = AlertDialog.Builder(requireContext())
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
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
        passwordDialog.show()
    }

    private fun promptNewPassword(onPasswordReceived: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null)
        val passwordInputLayout: TextInputLayout = dialogView.findViewById(R.id.password_input_layout)
        val passwordInput: TextInputEditText = dialogView.findViewById(R.id.password_input)
        val togglePasswordVisibility: CheckBox = dialogView.findViewById(R.id.toggle_password_visibility)

        // Set up the checkbox listener for toggling password visibility
        togglePasswordVisibility.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordInput.setSelection(passwordInput.text?.length ?: 0) // Move cursor to the end
        }

        val passwordDialog = AlertDialog.Builder(requireContext())
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
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
        passwordDialog.show()
    }


    private fun reauthenticateUser(onSuccess: () -> Unit) {
        promptCurrentPassword { currentPassword ->
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    val view = view ?: return@addOnCompleteListener
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        Snackbar.make(view, "Re-authentication failed: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setupNotificationPreferences() {
        val notificationPreference: SwitchPreferenceCompat? = findPreference("enable_notifications")
        val timingPreference: ListPreference? = findPreference("notification_timing")

        // Set initial state
        notificationPreference?.isChecked = appPreferences.areNotificationsEnabled()
        timingPreference?.isEnabled = notificationPreference?.isChecked == true

        notificationPreference?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            requireContext().getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enable_notifications", enabled)
                .apply()

            if (!enabled) {
                val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
                WorkManager.getInstance(requireContext())
                    .cancelUniqueWork("ExpirationCheckWork")
            } else {
                scheduleExpirationCheck(requireContext())
            }

            timingPreference?.isEnabled = enabled
            true
        }

        timingPreference?.setOnPreferenceChangeListener { _, newValue ->
            val days = newValue.toString()
            requireContext().getSharedPreferences("FedUpPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("notification_timing", days)
                .apply()
            true
        }
    }

    // Add this function to handle work scheduling
    private fun scheduleExpirationCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(
            1, TimeUnit.DAYS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "ExpirationCheckWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().setTheme(R.style.SettingsTheme)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey))
        return view
    }
}