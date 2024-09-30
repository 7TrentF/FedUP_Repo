package com.example.fedup_foodwasteapp


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
                Log.w("LoginActivity", "Google sign in failed", e)
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
                // Show error for incorrect email format
                showSnackbar("Invalid Email Format", android.R.color.holo_red_dark)
                return@setOnClickListener
            }

            if (loginPassword.length < 6) {
                // Show error for short password
                showSnackbar("Password must be at least 6 characters long", android.R.color.holo_red_dark)
                return@setOnClickListener
            }

            // Proceed with login if validations pass
            userLogin(loginEmail, loginPassword)
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
                        Log.d("Login", "User authenticated, token: $token")
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
                         Log.d("JWT Token", "Token received: $idToken") // Print to Logcat
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






}