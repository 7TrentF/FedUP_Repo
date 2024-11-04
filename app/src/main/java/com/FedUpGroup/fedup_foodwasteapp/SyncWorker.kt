package com.FedUpGroup.fedup_foodwasteapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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