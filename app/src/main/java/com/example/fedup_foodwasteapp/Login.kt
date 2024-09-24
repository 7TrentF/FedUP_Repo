package com.example.fedup_foodwasteapp


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {
    private lateinit var emailEdit:EditText
    private lateinit var passwordEdit:EditText
    private lateinit var google:Button
    private lateinit var signIn:Button
    private lateinit var signUp:Button
    private lateinit var  mAuth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        emailEdit=findViewById(R.id.editTextEmailAddress)
        passwordEdit=findViewById(R.id.editTextPassword)
        google=findViewById(R.id.btnGoogleSSO)
        signIn=findViewById(R.id.btnSignIn)
        signUp=findViewById(R.id.SignUpButton)
        mAuth=Firebase.auth
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


}