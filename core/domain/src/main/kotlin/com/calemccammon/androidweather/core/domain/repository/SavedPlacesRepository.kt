package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.SavedPlace
import kotlinx.coroutines.flow.Flow

/** The places the user has kept. Their forecasts are refreshed in the background. */
interface SavedPlacesRepository {
    /** Every saved place, oldest first. */
    val places: Flow<List<SavedPlace>>

    /** Whether [place] is among the saved ones. */
    fun isSaved(place: Place): Flow<Boolean>

    /** Keeps [place]. Saving a place twice keeps it once. */
    suspend fun save(place: Place)

    suspend fun remove(place: Place)
}
