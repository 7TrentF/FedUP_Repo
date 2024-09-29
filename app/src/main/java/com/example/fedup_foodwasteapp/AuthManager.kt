package com.example.fedup_foodwasteapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth

class AuthManager private constructor() {
    private var cachedToken: String? = null

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }
    }

    // Method to get cached token
    fun getCachedToken(): String? = cachedToken

    fun getIdToken(onTokenReceived: (String?, String?) -> Unit) {
        // Return cached token if available
        if (cachedToken != null) {
            Log.d("Token", "Using cached token: $cachedToken")
            onTokenReceived(cachedToken, null)
            return
        }


        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cachedToken = task.result?.token
                    Log.d("Token", "New token: $cachedToken")  // This will print the token in Logcat
                    onTokenReceived(cachedToken, null)
                } else {
                    Log.d("Token", "Token retrieval failed: ${task.exception?.message}")
                    onTokenReceived(null, task.exception?.message)
                }
            }
    }



    // Function to log out the user
    fun logoutUser(context: Context) {
        cachedToken = null
        FirebaseAuth.getInstance().signOut()

        // Navigate to LoginActivity
        val intent = Intent(context, Login::class.java)

        // Clear the back stack to prevent the user from returning to the previous screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Start the LoginActivity
        context.startActivity(intent)

        // Optionally, if you call this from an Activity, finish it:
        if (context is Activity) {
            (context as Activity).finish()
        }
    }

}



