package com.example.fedup_foodwasteapp
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class SyncService(
    private val repository: IngredientRepository,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun syncUnsyncedIngredients() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d("SyncService", "No network available, skipping sync")
            return
        }

        try {
            val token = getAuthToken() ?: run {
                Log.e("SyncService", "Failed to get auth token, skipping sync")
                return
            }

            val unsyncedIngredients = repository.getUnsyncedIngredients()
            Log.d("SyncService", "Unsynced ingredients: $unsyncedIngredients")

            for (ingredient in unsyncedIngredients) {
                try {
                    val response = RetrofitClient.apiService.addIngredient(ingredient)
                    if (response.isSuccessful) {
                        val syncedIngredient = response.body()
                        if (syncedIngredient != null) {
                            val updatedIngredient = ingredient.copy(
                                firebaseId = syncedIngredient.firebaseId,
                                isSynced = true,
                                lastModified = System.currentTimeMillis()
                            )
                            Log.d("SyncService", "Updating local ingredient: $updatedIngredient")
                            repository.updateIngredient(updatedIngredient)
                        } else {
                            Log.e("SyncService", "Failed to sync ingredient ${ingredient.id}: Response body is null")
                        }
                    } else {
                        Log.e("SyncService", "Failed to sync ingredient ${ingredient.id}: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("SyncService", "Error syncing ingredient ${ingredient.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncService", "Error during sync", e)
        }
    }

    private suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        AuthManager.getInstance().getIdToken { token, error ->
            if (error != null) {
                Log.e("SyncWorkerLog", "Error getting auth token", error as? Throwable ?: Exception(error))

                continuation.resume(null) { }
            } else {
                continuation.resume(token) { }
            }
        }
    }

    fun startSync() {
        scope.launch {
            syncUnsyncedIngredients()
        }
    }
}