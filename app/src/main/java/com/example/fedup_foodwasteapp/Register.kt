package com.example.fedup_foodwasteapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        btSignUp.setOnClickListener() {
            val userEmail = editTextEmail.text.toString()
            val userPassword = editTextPassword.text.toString()
            registerFirebase(userEmail,userPassword)
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
}