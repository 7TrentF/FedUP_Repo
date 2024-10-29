package com.example.fedup_foodwasteapp

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
class SyncManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleSyncWork() {
        // Create periodic sync request
        val syncRequest = PeriodicWorkRequestBuilder<IngredientSyncWorker>(
            SyncConstants.SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .build()

        // Enqueue unique periodic work
        workManager.enqueueUniquePeriodicWork(
            SyncConstants.SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun requestImmediateSync() {
        val syncRequest = OneTimeWorkRequestBuilder<IngredientSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "${SyncConstants.SYNC_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun getSyncStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkLiveData(SyncConstants.SYNC_WORK_NAME)
    }
}