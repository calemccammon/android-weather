package com.calemccammon.androidweather.core.data.repository

import com.calemccammon.androidweather.core.datastore.SettingsDataSource
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Settings, kept in a file on the device. */
internal class DataStoreSettingsRepository
    @Inject
    constructor(
        private val source: SettingsDataSource,
    ) : SettingsRepository {
        override val settings: Flow<UserSettings> = source.settings

        override suspend fun setUnits(units: Units) = source.setUnits(units)

        override suspend fun setSelectedPlace(place: Place?) = source.setSelectedPlace(place)

        override suspend fun setAlertsEnabled(enabled: Boolean) = source.setAlertsEnabled(enabled)

        override suspend fun setNotifiedAlerts(ids: Set<String>) = source.setNotifiedAlerts(ids)
    }
