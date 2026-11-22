package com.calemccammon.androidweather.core.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calemccammon.androidweather.core.domain.repository.AlertNotifier
import com.calemccammon.androidweather.core.model.WeatherAlert
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.core.ui.label
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Alerts as system notifications, one for each, in a channel of their own so
 * the user can silence them in the system's settings without touching the app.
 *
 * Pressing one opens the app on the place it is about.
 */
internal class SystemAlertNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : AlertNotifier {
        private val manager = NotificationManagerCompat.from(context)

        // The permission exists only from Android 13. On every version the
        // system says whether it will show the app's notifications, which is
        // false without the permission, so that one question is asked instead.
        @SuppressLint("MissingPermission")
        override fun show(alerts: List<WeatherAlert>) {
            if (alerts.isEmpty() || !manager.areNotificationsEnabled()) return
            createChannel()
            for (alert in alerts) {
                // The same alert shown again replaces itself.
                manager.notify(alert.id.hashCode(), notification(alert))
            }
        }

        /** Makes the channel if it is not there. Making it again changes nothing. */
        private fun createChannel() {
            manager.createNotificationChannel(
                NotificationChannelCompat
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.core_notifications_channel_name))
                    .setDescription(context.getString(R.string.core_notifications_channel_description))
                    .build(),
            )
        }

        private fun notification(alert: WeatherAlert) =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.core_notifications_ic_alert)
                .setContentTitle(
                    context.getString(
                        if (alert.isToday) {
                            R.string.core_notifications_alert_today
                        } else {
                            R.string.core_notifications_alert_tomorrow
                        },
                        alert.condition.label.asString(context.resources),
                        PlaceUiModel.from(alert.place).title.asString(context.resources),
                    ),
                ).setContentText(context.getString(R.string.core_notifications_alert_text))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openPlace(alert))
                .setAutoCancel(true)
                .build()

        /** Opens the app, telling it which saved place to show. */
        private fun openPlace(alert: WeatherAlert): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
            launch.putExtra(AlertIntents.EXTRA_SAVED_PLACE_ID, alert.savedPlaceId)
            return PendingIntent.getActivity(
                context,
                // One pending intent per place; a later alert for the place updates it.
                alert.savedPlaceId.hashCode(),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        companion object {
            const val CHANNEL_ID = "weather-alerts"
        }
    }
