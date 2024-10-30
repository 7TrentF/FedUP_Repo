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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit


class IngredientSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams)

{

    private val ingredientDao = (applicationContext as FedUpFoodWaste).database.ingredientDao()
    private val apiService = RetrofitClient.apiService

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.i("SyncWorkerLog", "Sync started")

                if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                    Log.w("SyncWorkerLog", "Network unavailable, scheduling retry")
                    return@withContext Result.retry()
                }

                // Add exponential backoff for retries
                val runAttemptCount = runAttemptCount
                if (runAttemptCount > 3) {
                    Log.e("SyncWorkerLog", "Too many retry attempts, marking as failure")
                    return@withContext Result.failure()
                }

               // syncIngredients()
                Log.i("SyncWorkerLog", "Sync completed successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e("SyncWorkerLog", "Sync failed due to an error", e)
                Result.retry()
            }
        }
    }

    /*
    private suspend fun syncIngredients() {
        // First, sync local changes to server
        val unsynced = ingredientDao.getUnsyncedIngredients()
        Log.i("SyncWorkerLog", "Found ${unsynced.size} unsynced ingredients")

        // Use a mutex to prevent race conditions during sync
        val syncMutex = Mutex()

        unsynced.forEach { ingredient ->
            syncMutex.withLock {
                try {
                    when {
                        ingredient.isDeleted -> handleDeletedIngredient(ingredient)
                        else -> handleUpsertIngredient(ingredient)
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorkerLog", "Error processing ingredient ${ingredient.id}", e)
                    // Don't throw here - continue processing other ingredients
                }
            }
        }

        // Then fetch server changes
        fetchAndUpdateFromServer()
    }

    private suspend fun handleDeletedIngredient(ingredient: Ingredient) {
        if (ingredient.firebaseId.isEmpty()) {
            // If it was never synced, just delete locally
            ingredientDao.delete(ingredient)
            return
        }

        try {
            val response = apiService.deleteIngredient(ingredient.firebaseId)
            if (response.isSuccessful) {
                Log.i("SyncWorkerLog", "Deleted ingredient ${ingredient.firebaseId}")
                ingredientDao.delete(ingredient)
            } else {
                Log.w("SyncWorkerLog", "Failed to delete ingredient ${ingredient.firebaseId}: ${response.code()}")
                // Keep the delete flag and retry later
            }
        } catch (e: Exception) {
            Log.e("SyncWorkerLog", "Error deleting ingredient ${ingredient.firebaseId}", e)
            throw e
        }
    }

    private suspend fun handleUpsertIngredient(ingredient: Ingredient) {
        try {
            val response = if (ingredient.firebaseId.isEmpty()) {
                apiService.addIngredient(ingredient)
            } else {
                apiService.updateIngredient(ingredient.firebaseId, ingredient)
            }

            if (response.isSuccessful) {
                response.body()?.let { serverIngredient ->
                    (serverIngredient as? Ingredient)?.let {
                        val updatedIngredient = ingredient.copy(
                            firebaseId = it.firebaseId,
                            isSynced = true,
                            version = it.version,
                            lastModified = System.currentTimeMillis()
                        )
                        ingredientDao.updateIng(updatedIngredient)
                        Log.i("SyncWorkerLog", "Successfully synced ingredient ${ingredient.id}")
                    }
                }
            } else {
                Log.w("SyncWorkerLog", "Failed to sync ingredient ${ingredient.id}: ${response.code()}")
                handleSyncError(response.code(), ingredient)
            }
        } catch (e: Exception) {
            Log.e("SyncWorkerLog", "Error syncing ingredient ${ingredient.id}", e)
            throw e
        }
    }

    private suspend fun fetchAndUpdateFromServer() {
        try {
            Log.i("SyncWorkerLog", "Fetching server changes")
            val response = apiService.getIngredients()

            if (response.isSuccessful) {
                response.body()?.let { serverIngredients ->
                    updateLocalDatabase(serverIngredients)
                }
            } else {
                Log.w("SyncWorkerLog", "Failed to fetch server changes: ${response.code()}")
                throw IOException("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SyncWorkerLog", "Error fetching server changes", e)
            throw e
        }
    }

    private suspend fun updateLocalDatabase(serverIngredients: List<Ingredient>) {
        serverIngredients.forEach { serverIngredient ->
            try {
                val localIngredient = ingredientDao.getIngredientByFirebaseId(serverIngredient.firebaseId)

                when {
                    localIngredient == null -> {
                        // New server ingredient
                        ingredientDao.insert(serverIngredient.copy(
                            isSynced = true,
                            lastModified = System.currentTimeMillis()
                        ))
                    }
                    localIngredient.version < serverIngredient.version && localIngredient.isSynced -> {
                        // Update only if local version is older and local changes aren't pending
                        ingredientDao.updateIng(serverIngredient.copy(
                            id = localIngredient.id,
                            isSynced = true,
                            lastModified = System.currentTimeMillis()
                        ))
                    }
                    !localIngredient.isSynced -> {
                        Log.i("SyncWorkerLog", "Skipping server update for ${localIngredient.id} - local changes pending")
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncWorkerLog", "Error processing server ingredient ${serverIngredient.firebaseId}", e)
                // Continue processing other ingredients
            }
        }
    }

    private fun handleSyncError(responseCode: Int, ingredient: Ingredient) {
        when (responseCode) {
            409 -> {
                // Conflict - mark for re-sync
                Log.w("SyncWorkerLog", "Conflict detected for ingredient ${ingredient.id}")
            }
            401, 403 -> {
                // Auth error - might need to refresh token
                Log.e("SyncWorkerLog", "Authentication error")
            }
            404 -> {
                // Resource not found - might need to recreate
                Log.w("SyncWorkerLog", "Resource not found for ingredient ${ingredient.id}")
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<IngredientSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "IngredientSync",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }
    }

 */
}