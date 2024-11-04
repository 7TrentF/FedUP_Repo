package com.example.fedup_foodwasteapp


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider

class Login : AppCompatActivity() {
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var signIn: Button
    private val BIOMETRIC_PREF_KEY = "biometric_enabled_"
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var signUp: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var togglePasswordVisibility: ImageView
    private var isPasswordVisible = false // Track visibility state
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        emailEdit = findViewById(R.id.editTextEmailAddress)
        passwordEdit = findViewById(R.id.editTextPassword)
        signIn = findViewById(R.id.btnSignIn)
        signUp = findViewById(R.id.SignUpButton)
        mAuth = Firebase.auth
        togglePasswordVisibility = findViewById(R.id.TogglePassword) // Toggle checkbox

        // Initialize Biometric prompt and prompt info
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Automatically log the user in after successful biometric authentication
                    val loginEmail = emailEdit.text.toString()
                    val loginPassword = passwordEdit.text.toString()

                    // Use email and password for Firebase login if needed, or directly navigate to main screen
                    if (loginEmail.isNotBlank() && loginPassword.isNotBlank()) {
                        userLogin(loginEmail, loginPassword)
                    } else {
                        // Alternatively, navigate to the main screen if biometrics alone are sufficient
                        val intent = Intent(this@Login, MainActivity::class.java)
                        startActivity(intent)
                        finish()  // Optional: Finish Login activity to prevent returning to login screen
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showSnackbar("Authentication failed", android.R.color.holo_red_dark)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showSnackbar("Authentication error: $errString", android.R.color.holo_red_dark)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for FedUp")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("160835827466-av691ujd9v27dd98hhhdqnad7d58o3f1.apps.googleusercontent.com")  // client_id from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Use Snackbar to show the error message
                val rootView = findViewById<View>(android.R.id.content) // Get the root view
                Snackbar.make(rootView, "Google Sign-In Failed", Snackbar.LENGTH_LONG).show()
            }
        }

        // Set click listener for Google sign-in button
        googleSignInButton = findViewById(R.id.btnGoogleSSO)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        signIn.setOnClickListener {
            val loginEmail = emailEdit.text.toString()
            val loginPassword = passwordEdit.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(loginEmail).matches()) {
                showSnackbar("Invalid Email Format", android.R.color.holo_red_dark)
                return@setOnClickListener
            }

            if (loginPassword.length < 6) {
                showSnackbar("Password must be at least 6 characters long", android.R.color.holo_red_dark)
                return@setOnClickListener
            }

            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                userLogin(loginEmail, loginPassword)
            }
        }

        // Set click listener for sign-up button
        signUp.setOnClickListener {
            val intent = Intent(this@Login, Register::class.java)
            startActivity(intent)
        }
        // Set password visibility toggle
        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        val googleAccount = GoogleSignIn.getLastSignedInAccount(this)

        if (currentUser != null && googleAccount != null) {
            // User is signed in with Google, trigger the Google sign-in launcher
            signInWithGoogle()
        } else if (currentUser != null) {
            // User is signed in with email/password, navigate to main app
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showSnackbar(message: String, color: Int) {
        val rootView = findViewById<View>(android.R.id.content) // Get the root view
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(color, theme))
            .show()
    }

    private fun userLogin(loginEmail: String, loginPassword: String) {
        mAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, now get the Firebase token
                    getFirebaseToken { token ->
                        // Now you have the token, make the network request to your API

                        val rootView = findViewById<View>(android.R.id.content) // Get the root view

                        showSnackbar("Login Successful", android.R.color.holo_green_dark)
                        val intent = Intent(this@Login, MainActivity::class.java)
                        // Pass token to MainActivity if needed
                        intent.putExtra("jwt_token", token)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // If sign-in fails, display a message to the user
                    val rootView = findViewById<View>(android.R.id.content) // Get the root view
                    showSnackbar("Login Failed", android.R.color.holo_red_dark)

                }
            }
    }

    // Biometric authentication with a callback
    private fun authenticateUserWithBiometrics(onSuccess: () -> Unit) {
        // Shared preference check ensures biometrics are only used if enabled
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val biometricEnabled = sharedPref.getBoolean(BIOMETRIC_PREF_KEY, false)
        if (biometricEnabled) {
            val executor = ContextCompat.getMainExecutor(this)
            biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showSnackbar("Incorrect biometrics", android.R.color.holo_red_dark)
                }
            })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Confirm your identity with biometrics")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            showSnackbar("Biometric login is disabled in settings", android.R.color.holo_red_dark)
        }
    }

    // Function to handle successful biometric authentication
    private fun onSuccessfulBiometricAuthentication() {
        showSnackbar("Login Successful", android.R.color.holo_green_dark)
        navigateToMain()
    }

    // Navigate to MainActivity
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startBiometricPrompt(email: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Handle successful biometric login
                navigateToMain("")  // Or re-authenticate silently with Firebase if needed
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
        } else {
            passwordEdit.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_visibility)
        }
        passwordEdit.setSelection(passwordEdit.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    // Get Firebase JWT token
    private fun getFirebaseToken(onTokenReceived: (String) -> Unit) {
        val user = mAuth.currentUser
        user?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result?.token
                    if (idToken != null) {

                        val rootView = findViewById<View>(android.R.id.content) // Get the root view
                        Snackbar.make(rootView, "Token: $idToken", Snackbar.LENGTH_LONG).show()

                        onTokenReceived(idToken)
                    }
                } else {
                    // Handle error

                    val rootView = findViewById<View>(android.R.id.content) // Get the root view
                    Snackbar.make(rootView, "Failed to get token", Snackbar.LENGTH_LONG).show()
                }
            }
    }


    private fun signInWithGoogle() {
        // Sign out of the existing Google account to force account selection each time
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }


    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    getFirebaseToken { token ->
                        navigateToMain(token)
                    }
                } else {

                    val rootView = findViewById<View>(android.R.id.content) // Get the root view
                    Snackbar.make(rootView, "Authentication Failed", Snackbar.LENGTH_LONG).show()

                }
            }
    }

    // Navigate to the MainActivity
    private fun navigateToMain(token: String) {

        val rootView = findViewById<View>(android.R.id.content) // Get the root view
        Snackbar.make(rootView, "Login Successful", Snackbar.LENGTH_LONG).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("jwt_token", token)
        startActivity(intent)
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = PreferenceManager.getDefaultSharedPreferences(newBase).getString("language_preference", "en") ?: "en"
        val context = LocaleHelper.wrap(newBase, languageCode)
        super.attachBaseContext(context)
    }


}