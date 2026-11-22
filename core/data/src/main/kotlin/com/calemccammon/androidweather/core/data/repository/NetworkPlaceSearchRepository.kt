package com.calemccammon.androidweather.core.data.repository

import com.calemccammon.androidweather.core.domain.repository.PlaceSearchRepository
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.network.WeatherNetworkDataSource
import javax.inject.Inject

/** Place search, straight from the weather service. */
internal class NetworkPlaceSearchRepository
    @Inject
    constructor(
        private val network: WeatherNetworkDataSource,
    ) : PlaceSearchRepository {
        override suspend fun search(query: String): List<Place> = network.searchPlaces(query)
    }
