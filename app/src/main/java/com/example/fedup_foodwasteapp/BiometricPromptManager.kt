package com.example.fedup_foodwasteapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BiometricPromptManager(private val context: Context) {
    private val biometricManager: BiometricManager = BiometricManager.from(context)

    fun isBiometricSupported(): Boolean {
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun showBiometricPrompt(): BiometricResult {
        return suspendCoroutine { continuation ->
            val prompt = BiometricPrompt(
                context as AppCompatActivity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Handle error
                        continuation.resume(BiometricResult.AuthenticationError)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        // Authentication succeeded
                        continuation.resume(BiometricResult.AuthenticationSuccess)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Authentication failed
                        continuation.resume(BiometricResult.AuthenticationFailed)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build()

            prompt.authenticate(promptInfo)
        }
    }
}

sealed class BiometricResult {
    object AuthenticationSuccess : BiometricResult()
    object AuthenticationFailed : BiometricResult()
    object AuthenticationError : BiometricResult()
}
