package com.example.fedup_foodwasteapp

import android.util.Log
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
}



