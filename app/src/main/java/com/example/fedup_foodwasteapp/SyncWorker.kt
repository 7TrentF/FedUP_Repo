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

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {

                if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                    return@withContext Result.retry()
                }

                // Add exponential backoff for retries
                val runAttemptCount = runAttemptCount
                if (runAttemptCount > 3) {
                    return@withContext Result.failure()
                }

               // syncIngredients()
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

}