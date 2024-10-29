package com.example.fedup_foodwasteapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3000 // 3 seconds delay
    private lateinit var mAuth: FirebaseAuth
    private lateinit var loadingBar: ProgressBar
    private lateinit var ingredientViewModel: IngredientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash2)
        Log.d("flow", "this is the SplashActivity")

        // Initialize the ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Fetch ingredients from Firebase and observe the LiveData
       //ingredientViewModel.fetchIngredientsFromFirebase()

       // ingredientViewModel.loadIngredients()


        mAuth = Firebase.auth
        loadingBar = findViewById(R.id.loading_bar)

        // Set the progress bar color (optional)
        loadingBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Show loading bar
        loadingBar.visibility = View.VISIBLE

        // Delay for the splash screen, then check if the user is authenticated
        Handler().postDelayed({
            checkAuthentication()
        }, SPLASH_TIME_OUT)
    }

    private fun checkAuthentication() {
        Log.d("SplashActivityLog", "checkAuthentication() called")

        // Retrieve the current authenticated user from FirebaseAuth
        val currentUser = mAuth.currentUser
        Log.d("SplashActivityLog", "Current user: ${currentUser?.uid ?: "No user signed in"}")

        // Hide loading bar before navigating
        loadingBar.visibility = View.GONE
        Log.d("SplashActivityLog", "Loading bar visibility set to GONE")

        if (currentUser != null) {
            // If the user is signed in, navigate to MainActivity
            Log.d("SplashActivityLog", "User is authenticated. Navigating to MainActivity.")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // If no user is signed in, navigate to Login Activity
            Log.d("SplashActivityLog", "No user is authenticated. Navigating to Login Activity.")
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // Finish the SplashActivity to prevent the user from returning to it
        finish()
        Log.d("SplashActivityLog", "SplashActivity finished.")
    }




}
