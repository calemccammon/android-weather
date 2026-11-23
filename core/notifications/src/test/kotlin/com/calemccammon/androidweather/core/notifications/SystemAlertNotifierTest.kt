package com.calemccammon.androidweather.core.notifications

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.calemccammon.androidweather.core.model.WeatherAlert
import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.testing.Samples
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SystemAlertNotifierTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val system = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    private val notifier = SystemAlertNotifier(context)

    private val storm =
        WeatherAlert(Samples.asheville, LocalDate.parse("2026-11-18"), WeatherCondition.Thunderstorm, isToday = true)
    private val ice =
        WeatherAlert(Samples.point, LocalDate.parse("2026-11-19"), WeatherCondition.FreezingRain, isToday = false)

    private val titles
        get() = system.allNotifications.mapNotNull { it.extras.getString(NotificationCompat.EXTRA_TITLE) }.sorted()

    @Test
    fun `each alert is one notification, worded with its condition, day and place`() {
        assertEquals(true, notifier.show(listOf(storm, ice)))

        assertEquals(listOf("Freezing rain tomorrow in 33.86°S, 151.21°E", "Thunderstorm today in Asheville"), titles)
        assertEquals(listOf("Weather alerts"), system.notificationChannels.map { it.name.toString() })
        assertEquals(setOf(SystemAlertNotifier.CHANNEL_ID), system.allNotifications.map { it.channelId }.toSet())
    }

    @Test
    fun `the same alert shown again replaces itself`() {
        notifier.show(listOf(storm))
        notifier.show(listOf(storm))

        assertEquals(1, system.allNotifications.size)
    }

    @Test
    fun `pressing a notification opens the app with the saved place it is about`() {
        // A library's test has no launcher activity of its own, so it is given one.
        val launcher = ComponentName(context, "com.example.Launcher")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(launcher)
            addIntentFilterForActivity(
                launcher,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }

        notifier.show(listOf(ice))

        val opened = shadowOf(system.allNotifications.single().contentIntent).savedIntent
        assertEquals(Samples.point.latitude to Samples.point.longitude, AlertIntents.coordinates(opened))
        assertEquals(null, AlertIntents.coordinates(Intent()))
        assertEquals(null, AlertIntents.coordinates(null))
    }

    @Test
    fun `nothing is shown when the system will not let the app notify`() {
        // As without the permission, or with the app's notifications turned off.
        system.setNotificationsEnabled(false)

        val shown = notifier.show(listOf(storm))

        // The caller is told, so the alert is not recorded as told.
        assertEquals(false, shown)
        assertEquals(0, system.allNotifications.size)
        assertEquals(0, system.notificationChannels.size)
    }

    @Test
    fun `no alerts make no channel`() {
        assertEquals(true, notifier.show(emptyList()))

        assertEquals(0, system.notificationChannels.size)
    }
}
