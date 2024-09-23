package com.example.fedup_foodwasteapp

import com.google.firebase.auth.FirebaseAuth

class AuthManager private constructor() {

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
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTokenReceived(task.result?.token, null)
                } else {
                    onTokenReceived(null, task.exception?.message)
                }
            }
    }
}



