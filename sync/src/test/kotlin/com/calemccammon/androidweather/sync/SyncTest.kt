package com.calemccammon.androidweather.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.testing.FakeForecastRepository
import com.calemccammon.androidweather.core.testing.FakeSavedPlacesRepository
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SyncTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = MutableClock()
    private val forecasts = FakeForecastRepository(clock)
    private val saved = FakeSavedPlacesRepository(listOf(Samples.asheville, Samples.lisbon))

    /** Builds the worker as Hilt would, with stand-ins behind the use case. */
    private val factory =
        object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = SyncWorker(appContext, workerParameters, RefreshSavedPlaces(saved, forecasts, clock))
        }

    private fun worker(attempt: Int = 0) =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(factory)
            .setRunAttemptCount(attempt)
            .build()

    @Test
    fun `a run fetches every saved place's forecast and succeeds`() =
        runTest {
            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf(Samples.asheville, Samples.lisbon), forecasts.refreshed)
            assertEquals(Samples.forecast(), forecasts.heldFor(Samples.lisbon)?.forecast)
        }

    @Test
    fun `a run that could not reach the service asks to be run again soon`() =
        runTest {
            forecasts.failure = WeatherFailure.NoConnection

            assertEquals(ListenableWorker.Result.retry(), worker().doWork())
        }

    @Test
    fun `after a few tries it gives up until the next scheduled run`() =
        runTest {
            forecasts.failure = WeatherFailure.NoConnection

            assertEquals(ListenableWorker.Result.retry(), worker(attempt = SyncWorker.MAX_RETRIES - 1).doWork())
            assertEquals(ListenableWorker.Result.failure(), worker(attempt = SyncWorker.MAX_RETRIES).doWork())
        }

    @Test
    fun `a failure that waiting will not mend is not retried`() =
        runTest {
            forecasts.failure = WeatherFailure.BadResponse

            assertEquals(ListenableWorker.Result.success(), worker().doWork())
        }

    @Test
    fun `the refresh is scheduled every three hours, only with a connection and battery to spare`() {
        val spec = SyncScheduler.request().workSpec

        assertEquals(true, spec.isPeriodic)
        assertEquals(Duration.ofHours(3).toMillis(), spec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(true, spec.constraints.requiresBatteryNotLow())
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(Duration.ofMinutes(10).toMillis(), spec.backoffDelayDuration)
        assertEquals(SyncWorker::class.java.name, spec.workerClassName)
    }
}
