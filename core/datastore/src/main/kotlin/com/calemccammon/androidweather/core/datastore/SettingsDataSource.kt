package com.calemccammon.androidweather.core.datastore

import androidx.datastore.core.DataStore
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The settings file, read and written in the app's own terms. */
class SettingsDataSource
    @Inject
    constructor(
        private val store: DataStore<StoredSettings>,
    ) {
        val settings: Flow<UserSettings> = store.data.map { it.toModel() }

        suspend fun setUnits(units: Units) {
            val word =
                when (units) {
                    Units.Metric -> StoredSettings.METRIC
                    Units.Imperial -> StoredSettings.IMPERIAL
                }
            store.updateData { it.copy(units = word) }
        }

        suspend fun setSelectedPlace(place: Place?) {
            val stored = place?.run { StoredPlace(latitude, longitude, name, region, country) }
            store.updateData { it.copy(selectedPlace = stored) }
        }

        suspend fun setAlertsEnabled(enabled: Boolean) {
            store.updateData { it.copy(alertsEnabled = enabled) }
        }

        suspend fun setNotifiedAlerts(ids: Set<String>) {
            store.updateData { it.copy(notifiedAlerts = ids) }
        }

        private fun StoredSettings.toModel() =
            UserSettings(
                units =
                    when (units) {
                        StoredSettings.METRIC -> Units.Metric
                        StoredSettings.IMPERIAL -> Units.Imperial
                        // Never chosen, or written by a version with a choice this one lacks.
                        else -> null
                    },
                selectedPlace = selectedPlace?.run { Place(latitude, longitude, name, region, country) },
                alertsEnabled = alertsEnabled,
                notifiedAlerts = notifiedAlerts,
            )
    }
