package com.FedUpGroup.fedup_foodwasteapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
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
    private lateinit var ingredientViewModel: IngredientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash2)

        // Initialize the ViewModel
        ingredientViewModel = ViewModelProvider(this).get(IngredientViewModel::class.java)

        // Animate the background shape
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.background_rotate)
        findViewById<View>(R.id.background_shape).startAnimation(rotateAnimation)

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
        // Retrieve the current authenticated user from FirebaseAuth
        val currentUser = mAuth.currentUser

        // Hide loading bar before navigating
        loadingBar.visibility = View.GONE

        if (currentUser != null) {
            // If the user is signed in, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // If no user is signed in, navigate to Login Activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        // Finish the SplashActivity to prevent the user from returning to it
        finish()
    }

}