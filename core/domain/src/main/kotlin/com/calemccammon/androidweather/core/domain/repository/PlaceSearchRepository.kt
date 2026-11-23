package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.Place

/** Finds places by name. Searching needs a connection: nothing is kept. */
interface PlaceSearchRepository {
    /**
     * Places whose name matches [query], best match first. Fails with a
     * [com.calemccammon.androidweather.core.model.WeatherException] and nothing else.
     */
    suspend fun search(query: String): List<Place>
}
