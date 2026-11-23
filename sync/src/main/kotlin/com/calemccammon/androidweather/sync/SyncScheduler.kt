package com.calemccammon.androidweather.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import javax.inject.Inject

/** Keeps the background refresh scheduled. */
class SyncScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) {
        /**
         * Schedules the refresh to run every [INTERVAL] for as long as the app
         * is installed. Called at every launch: the schedule already running is
         * kept, and only brought in line with [request] if that has changed
         * since the version that set it.
         */
        fun schedule() {
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request())
        }

        companion object {
            const val WORK_NAME = "refresh-saved-places"

            /** Weather models are run every few hours; asking more often brings nothing new. */
            val INTERVAL: Duration = Duration.ofHours(3)

            private val RETRY_AFTER: Duration = Duration.ofMinutes(10)

            internal fun request(): PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(INTERVAL)
                    .setConstraints(
                        Constraints
                            .Builder()
                            // Not attempted at all without a connection, and
                            // left for later when the battery is low.
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    ).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_AFTER)
                    .build()
        }
    }
