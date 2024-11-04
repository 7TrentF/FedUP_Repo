package com.FedUpGroup.fedup_foodwasteapp
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class SyncService(
    private val repository: IngredientRepository,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun syncUnsyncedIngredients() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return
        }

        try {
            val token = getAuthToken() ?: run {
                return
            }
            val unsyncedIngredients = repository.getUnsyncedIngredients()
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
                            repository.updateIngredient(updatedIngredient)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun getAuthToken(): String? = suspendCancellableCoroutine { continuation ->
        AuthManager.getInstance().getIdToken { token, error ->
            if (error != null) {

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