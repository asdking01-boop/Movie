package com.example.sync.engine

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.database.KrasDatabase
import java.util.concurrent.TimeUnit

class CatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = KrasDatabase.getInstance(applicationContext)
            val syncEngine = CatalogSyncEngine(db.krasDao())
            val results = syncEngine.synchronizeAllEnabledSources()
            if (results.any { !it.isSuccess }) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "kras_movies_periodic_catalog_sync"

        fun schedulePeriodicSync(context: Context, intervalHours: Long = 6L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(
                intervalHours.coerceAtLeast(1L),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }
    }
}
