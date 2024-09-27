package com.example.fedup_foodwasteapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3000 // 3 seconds delay
    private lateinit var mAuth: FirebaseAuth
    private lateinit var loadingBar: ProgressBar
    private var progressStatus = 0 // To track progress
    private lateinit var ingredientViewModel: IngredientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash2)

        // Initialize the ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Fetch ingredients from Firebase and observe the LiveData
        ingredientViewModel.fetchIngredientsFromFirebase()

        mAuth = Firebase.auth
        loadingBar = findViewById(R.id.loading_bar)

        // Set the progress bar color
        loadingBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Show loading bar and start updating progress
        loadingBar.visibility = View.VISIBLE
        updateProgressBar()

        // Delay for the splash screen, then check if the user is authenticated
        Handler().postDelayed({
            checkAuthentication()
        }, SPLASH_TIME_OUT)
    }

    private fun updateProgressBar() {
        // Create a new thread to simulate progress
        Thread {
            while (progressStatus < 100) {
                progressStatus += 10 // Increase progress by 10%
                // Update the progress on the UI thread
                runOnUiThread {
                    loadingBar.progress = progressStatus
                }
                try {
                    Thread.sleep(300) // Sleep for a short time to simulate loading (adjust as needed)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun checkAuthentication() {
        val currentUser = mAuth.currentUser
        loadingBar.visibility = View.GONE // Hide loading bar before navigating
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // User is not signed in, navigate to Login Activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        finish() // Close the SplashActivity
    }
}
