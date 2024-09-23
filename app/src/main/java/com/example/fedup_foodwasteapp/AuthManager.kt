package com.example.fedup_foodwasteapp

import com.google.firebase.auth.FirebaseAuth

class AuthManager {
    fun getIdToken(onTokenReceived: (String?) -> Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTokenReceived(task.result?.token)
                } else {
                    onTokenReceived(null)
                }
            }
    }
}
