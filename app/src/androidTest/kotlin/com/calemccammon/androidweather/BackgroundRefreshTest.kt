package com.calemccammon.androidweather

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.sync.SyncScheduler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The scheduled refresh on a device: the real worker, built by Hilt, run by
 * WorkManager as scheduled, writing to the real database, against a stand-in
 * for the weather service. WorkManager's test driver stands in for the
 * passing of three hours and for the device's connection and battery.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackgroundRefreshTest {
    private val openMeteo = FakeOpenMeteo()
    private val hilt = HiltAndroidRule(this)

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(openMeteo)
            .around(hilt)
            .around(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    GrantPermissionRule.grant()
                },
            )

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var saved: SavedPlacesRepository

    @Inject lateinit var forecasts: ForecastRepository

    @Inject lateinit var settings: SettingsRepository

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val asheville = Place(35.6, -82.55, "Asheville", "North Carolina", "United States")
    private val lisbon = Place(38.72, -9.13, "Lisbon", "Lisbon", "Portugal")

    @Before
    fun setUp() {
        hilt.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .setExecutor(SynchronousExecutor())
                .build(),
        )
    }

    /**
     * Schedules the refresh as the app does at launch, lets one run come due,
     * and waits for it to finish: the server has been asked once more for
     * each of [places] places.
     */
    private suspend fun runScheduledRefresh(places: Int): WorkInfo.State? {
        val askedBefore = forecastRequests()
        val workManager = WorkManager.getInstance(context)
        SyncScheduler(workManager).schedule()
        val work = workManager.getWorkInfosForUniqueWork(SyncScheduler.WORK_NAME).get().single()

        WorkManagerTestInitHelper.getTestDriver(context)!!.apply {
            setAllConstraintsMet(work.id)
            setPeriodDelayMet(work.id)
        }
        // The worker is a coroutine on its own threads, so it is waited for.
        await { forecastRequests() == askedBefore + places }
        await { workManager.getWorkInfoById(work.id).get()?.state != WorkInfo.State.RUNNING }
        return workManager.getWorkInfoById(work.id).get()?.state
    }

    private fun forecastRequests() = openMeteo.requests.count { it.startsWith("/v1/forecast") }

    private suspend fun await(condition: () -> Boolean) =
        withTimeout(TIMEOUT) {
            while (!condition()) delay(POLL)
        }

    private fun notificationTitles() =
        context
            .getSystemService(NotificationManager::class.java)
            .activeNotifications
            .mapNotNull { it.notification.extras.getString(NotificationCompat.EXTRA_TITLE) }
            .sorted()

    @Test
    fun aScheduledRunFetchesEverySavedPlaceAndStaysScheduled() =
        runBlocking {
            saved.save(asheville)
            saved.save(lisbon)
            assertNull(forecasts.observe(lisbon).first())

            val state = runScheduledRefresh(places = 2)

            assertEquals(
                7.0,
                forecasts
                    .observe(asheville)
                    .first()
                    ?.forecast
                    ?.current
                    ?.temperature,
            )
            assertEquals(
                18.0,
                forecasts
                    .observe(lisbon)
                    .first()
                    ?.forecast
                    ?.current
                    ?.temperature,
            )
            // Periodic work goes back to waiting for its next turn.
            assertEquals(WorkInfo.State.ENQUEUED, state)
        }

    @Test
    fun aRunWithTheServiceDownKeepsWhatWasHeld() =
        runBlocking {
            saved.save(asheville)
            runScheduledRefresh(places = 1)
            val held = forecasts.observe(asheville).first()
            assertEquals(7.0, held?.forecast?.current?.temperature)
            openMeteo.down = true

            runScheduledRefresh(places = 1)

            assertEquals(held, forecasts.observe(asheville).first())
        }

    @Test
    fun aRunTellsOfAStormAtASavedPlaceOnceAlertsAreOn() =
        runBlocking {
            saved.save(lisbon)
            settings.setAlertsEnabled(true)
            openMeteo.lisbonCode = THUNDERSTORM

            runScheduledRefresh(places = 1)

            await { notificationTitles().size == 2 }
            assertEquals(
                listOf("Thunderstorm today in Lisbon", "Thunderstorm tomorrow in Lisbon"),
                notificationTitles(),
            )
        }

    private companion object {
        const val THUNDERSTORM = 95
        const val TIMEOUT = 15_000L
        const val POLL = 50L
    }
}
