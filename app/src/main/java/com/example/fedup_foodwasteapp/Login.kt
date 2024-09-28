package com.example.fedup_foodwasteapp


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
import com.google.firebase.auth.GoogleAuthProvider

class Login : AppCompatActivity() {
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var signIn: Button
    private lateinit var signUp: Button
    private lateinit var mAuth: FirebaseAuth

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
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for Google sign-in button
        googleSignInButton = findViewById(R.id.btnGoogleSSO)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }



        signIn.setOnClickListener (){
            val loginEmail = emailEdit.text.toString()
            val loginPassword = passwordEdit.text.toString()
            userLogin(loginEmail,loginPassword)
        }



        signUp.setOnClickListener (){
            val intent = Intent(this@Login,Register::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate to main app
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun userLogin(loginEmail: String, loginPassword: String) {
        mAuth.signInWithEmailAndPassword(loginEmail, loginPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, now get the Firebase token
                    getFirebaseToken { token ->
                        Log.d("Login", "User authenticated, token: $token")
                        // Now you have the token, make the network request to your API
                        Toast.makeText(baseContext,"Login Successful", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@Login, MainActivity::class.java)
                        // Pass token to MainActivity if needed
                        intent.putExtra("jwt_token", token)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // If sign-in fails, display a message to the user
                    Toast.makeText(baseContext, "Login failed.", Toast.LENGTH_SHORT).show()
                }
            }
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
                        Toast.makeText(baseContext, "Token: $idToken", Toast.LENGTH_LONG).show() // Display in Toast
                        onTokenReceived(idToken)
                    }
                } else {
                    // Handle error
                    Toast.makeText(baseContext, "Failed to get token", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
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
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }





    // Navigate to the MainActivity
    private fun navigateToMain(token: String) {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("jwt_token", token)
        startActivity(intent)
        finish()
    }

}