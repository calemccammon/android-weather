package com.calemccammon.androidweather.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The background refresh: fetches the forecast of every saved place, so the
 * app opens on recent weather and has something to show with no connection.
 *
 * It writes to the database and nothing else. A screen that is open at the
 * time shows the newer forecast because it watches the database.
 */
@HiltWorker
internal class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val refreshSavedPlaces: RefreshSavedPlaces,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result =
            when (refreshSavedPlaces()) {
                RefreshSavedPlaces.Outcome.Done -> Result.success()
                // Tried again sooner than the next scheduled run, a few times;
                // after that the next scheduled run is soon enough.
                RefreshSavedPlaces.Outcome.RetryLater ->
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }

        companion object {
            const val MAX_RETRIES = 3
        }
    }
