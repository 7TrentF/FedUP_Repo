package com.FedUpGroup.fedup_foodwasteapp

import android.content.Context

class SyncManager private constructor(
    private val context: Context,
    private val repository: IngredientRepository
) {
    private val syncService = SyncService(repository, context)
    private val connectivityManager = ConnectivityManager(syncService, context)

    fun startSync() {
        connectivityManager.startListening()
        if (connectivityManager.isNetworkAvailable()) {
            syncService.startSync()
        }
    }

    fun stopSync() {
        connectivityManager.stopListening()
    }

    companion object {
        @Volatile
        private var instance: SyncManager? = null

        fun getInstance(context: Context, repository: IngredientRepository): SyncManager {
            return instance ?: synchronized(this) {
                instance ?: SyncManager(context.applicationContext, repository).also {
                    instance = it
                }
            }
        }
    }
}