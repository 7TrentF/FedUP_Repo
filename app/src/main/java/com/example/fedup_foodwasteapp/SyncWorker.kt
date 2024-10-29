package com.example.fedup_foodwasteapp

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// SyncWorker.kt
class IngredientSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    private val ingredientDao = (applicationContext as FedUpFoodWaste).database.ingredientDao()
    private val apiService = RetrofitClient.apiService

    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val LAST_SYNC_KEY = "last_sync_timestamp"

    override suspend fun doWork(): Result {
        return try {
            if (NetworkUtils.isNetworkAvailable(applicationContext)) {
                syncIngredients()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    private suspend fun syncIngredients() {
        // First, handle local unsynced changes
        val unsynced = ingredientDao.getUnsyncedIngredients()

        unsynced.forEach { ingredient ->
            try {
                if (ingredient.isDeleted) {
                    // Delete from API
                    val response = apiService.deleteIngredient(ingredient.firebaseId)
                    if (response.isSuccessful) {


                        ingredientDao.deleteByFirebaseId(ingredient.firebaseId)
                    }
                } else {
                    // Update or Create in API
                    val response = if (ingredient.firebaseId.isNotEmpty()) {
                        apiService.updateIngredient(ingredient.firebaseId, ingredient)
                    } else {
                        apiService.addIngredient(ingredient)
                    }

/*
                    if (response.isSuccessful) {
                        response.body()?.let { serverIngredient ->
                            ingredient.apply {
                                firebaseId = serverIngredient.firebaseId // Ensure this is set from server
                                isSynced = true
                                version = serverIngredient.version // Ensure this is set from server
                            }
                            ingredientDao.update(ingredient)
                        }
                    }*/




                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync ingredient ${ingredient.id}", e)
                // Continue with next item
            }
        }

        // Then fetch all remote ingredients
        try {
            val response = apiService.getIngredients()
            if (response.isSuccessful) {
                response.body()?.let { serverIngredients ->
                    updateLocalDatabase(serverIngredients)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to fetch remote ingredients", e)
        }
    }

    private suspend fun updateLocalDatabase(serverIngredients: List<Ingredient>) {
        serverIngredients.forEach { serverIngredient ->
            val localIngredient = ingredientDao.getIngredientByFirebaseId(serverIngredient.firebaseId)

            when {
                localIngredient == null -> {
                    // New ingredient from server
                    ingredientDao.insert(serverIngredient.copy(isSynced = true))
                }
                localIngredient.version < serverIngredient.version -> {
                    // Server version is newer
                    ingredientDao.update(serverIngredient.copy(
                        id = localIngredient.id,
                        isSynced = true
                    ))
                }
                // Local version is newer or same, no action needed
            }
        }
    }
}