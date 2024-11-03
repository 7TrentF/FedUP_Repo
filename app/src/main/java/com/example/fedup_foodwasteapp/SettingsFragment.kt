package com.example.fedup_foodwasteapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
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
import android.content.res.Configuration
import java.util.Locale


class SettingsFragment : PreferenceFragmentCompat() {

    private val authManager: AuthManager by lazy { AuthManager.getInstance() }
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser ?: return

        Log.d("SettingsFragment", "Current user: ${currentUser.email}")

        // Logout preference
        val logoutPreference = findPreference<Preference>("logout")
        logoutPreference?.setOnPreferenceClickListener {
            authManager.logoutUser(requireContext())
            Log.d("SettingsFragment", "User logged out.")
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
    }

    private fun updateEmail(newEmail: String) {
        currentUser.updateEmail(newEmail)
            .addOnCompleteListener { task ->
                val view = view ?: return@addOnCompleteListener
                if (task.isSuccessful) {
                    Log.d("SettingsFragment", "Email updated to: $newEmail")
                    Snackbar.make(view, "Email updated successfully.", Snackbar.LENGTH_LONG).show()
                } else {
                    Log.e("SettingsFragment", "Failed to update email: ${task.exception?.message}")
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
                    Log.d("SettingsFragment", "Password updated successfully.")
                    Snackbar.make(view, "Password updated successfully.", Snackbar.LENGTH_LONG).show()
                } else {
                    Log.e("SettingsFragment", "Failed to update password: ${task.exception?.message}")
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
                    Log.d("SettingsFragment", "Password entered for re-authentication.")
                    onPasswordReceived(password)
                } else {
                    Log.e("SettingsFragment", "Password is empty.")
                    Snackbar.make(dialogView, "Password cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d("SettingsFragment", "Re-authentication cancelled.")
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
                    Log.d("SettingsFragment", "New password entered.")
                    onPasswordReceived(password)
                } else {
                    Log.e("SettingsFragment", "New password is empty.")
                    Snackbar.make(dialogView, "Password cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d("SettingsFragment", "New password entry cancelled.")
                dialog.cancel()
            }
            .create()

        passwordDialog.show()
    }


    private fun reauthenticateUser(onSuccess: () -> Unit) {
        promptCurrentPassword { currentPassword ->
            Log.d("SettingsFragment", "Attempting to re-authenticate user.")
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    val view = view ?: return@addOnCompleteListener
                    if (task.isSuccessful) {
                        Log.d("SettingsFragment", "Re-authentication successful.")
                        onSuccess()
                    } else {
                        Log.e("SettingsFragment", "Re-authentication failed: ${task.exception?.message}")
                        Snackbar.make(view, "Re-authentication failed: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
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