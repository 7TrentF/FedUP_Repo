package com.example.fedup_foodwasteapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword:EditText
    private lateinit var btGoogle: Button
    private lateinit var btSignUp:Button
    private lateinit var auth: FirebaseAuth
    private lateinit var togglePasswordVisibility: ImageView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.edtPassword);
        btGoogle=findViewById(R.id.GoogleBtn)
        btSignUp=findViewById(R.id.BtnSignUp)
        auth= Firebase.auth
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility) // Add this

        btSignUp.setOnClickListener {
            val userEmail = editTextEmail.text.toString()
            val userPassword = editTextPassword.text.toString()
            if (isValidEmail(userEmail) && isValidPassword(userPassword)) {
                registerFirebase(userEmail, userPassword)
            } else {
                val rootView = findViewById<View>(android.R.id.content)
                when {
                    !isValidEmail(userEmail) -> Snackbar.make(rootView, "Please enter a valid email address.", Snackbar.LENGTH_LONG).show()
                    !isValidPassword(userPassword) -> Snackbar.make(rootView, "Password must be at least 6 characters long.", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Set up password visibility toggle
        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }

    }
    private fun registerFirebase(userEmail: String, userPassword: String) {
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Toast.makeText(baseContext,"Registration Successful", Toast.LENGTH_LONG).show()
                    val intent = Intent( this@Register,MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    when {
                        task.exception is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(baseContext, "The password is too weak.", Toast.LENGTH_SHORT).show()
                        }
                        task.exception is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(baseContext, "The username is taken.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(baseContext, "Registration failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

    }


private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun isValidPassword(password: String): Boolean {
    return password.length >= 6
}

private fun togglePasswordVisibility() {
    if (isPasswordVisible) {
        editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
    } else {
        editTextPassword.inputType = InputType.TYPE_CLASS_TEXT
        togglePasswordVisibility.setImageResource(R.drawable.ic_visibility)
    }
    editTextPassword.setSelection(editTextPassword.text.length)
    isPasswordVisible = !isPasswordVisible
}


    override fun attachBaseContext(newBase: Context) {
        val languageCode = PreferenceManager.getDefaultSharedPreferences(newBase).getString("language_preference", "en") ?: "en"
        val context = LocaleHelper.wrap(newBase, languageCode)
        super.attachBaseContext(context)
    }
}