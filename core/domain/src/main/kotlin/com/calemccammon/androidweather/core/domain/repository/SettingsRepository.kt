package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** What the user has chosen for the app, kept between launches. */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setUnits(units: Units)

    /** Remembers which place the home screen shows; null for none. */
    suspend fun setSelectedPlace(place: Place?)
}
