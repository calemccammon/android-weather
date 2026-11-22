package com.calemccammon.androidweather.core.network.model

import com.calemccammon.androidweather.core.model.Place
import kotlinx.serialization.Serializable

/** The geocoding response as Open-Meteo sends it. */
@Serializable
internal data class PlaceSearchDto(
    /** With no matches the key is missing altogether. */
    val results: List<PlaceDto> = emptyList(),
)

/** One entry of the geocoding response's `results`. */
@Serializable
internal data class PlaceDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** First-level administrative area: a state, province or similar. */
    val admin1: String? = null,
    val country: String? = null,
) {
    fun toDomain() =
        Place(
            name = name,
            latitude = latitude,
            longitude = longitude,
            region = admin1,
            country = country,
        )
}
