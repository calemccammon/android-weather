package com.calemccammon.androidweather.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import com.calemccammon.androidweather.core.domain.usecase.TellNewWeatherAlerts
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The background refresh: fetches the forecast of every saved place, so the
 * app opens on recent weather and has something to show with no connection.
 *
 * It writes to the database; a screen or widget showing at the time changes
 * because it watches the database. Then it tells the user of any severe
 * weather in what was fetched, if they have asked to be told.
 */
@HiltWorker
internal class SyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val refreshSavedPlaces: RefreshSavedPlaces,
        private val tellNewWeatherAlerts: TellNewWeatherAlerts,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            val outcome = refreshSavedPlaces()
            // Whatever was fetched, even if some places failed, is looked
            // through for severe weather the user has asked to be told of.
            tellNewWeatherAlerts()

            return when {
                !outcome.isWorthRetrying -> Result.success()
                // Tried again sooner than the next scheduled run, a few times;
                // after that the next scheduled run is soon enough.
                runAttemptCount < MAX_RETRIES -> Result.retry()
                else -> Result.failure()
            }
        }

        companion object {
            const val MAX_RETRIES = 3
        }
    }
