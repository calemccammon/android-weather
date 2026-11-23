package com.calemccammon.androidweather.core.model

import kotlin.math.abs

/** A point on the map. */
data class Place(
    val latitude: Double,
    val longitude: Double,
    /** What the place is called. A point picked straight off the map has none. */
    val name: String? = null,
    /** State, province or similar. Tells two places of one name apart. */
    val region: String? = null,
    val country: String? = null,
) {
    /** Whether this is a point on the globe at all. Written so that a NaN is not. */
    val isOnGlobe: Boolean
        get() = abs(latitude) <= MAX_LATITUDE && longitude.isFinite()

    companion object {
        private const val MAX_LATITUDE = 90.0
        private const val HALF_TURN = 180.0
        private const val FULL_TURN = 360.0

        /**
         * A place picked straight off the map, which has no name.
         *
         * A map that has been dragged round the world more than once reports
         * longitudes past 180 degrees, so the longitude is brought back into range.
         */
        fun fromCoordinates(
            latitude: Double,
            rawLongitude: Double,
        ) = Place(latitude = latitude, longitude = normalizeLongitude(rawLongitude))

        /** [longitude] as the same meridian between -180 (inclusive) and 180. */
        fun normalizeLongitude(longitude: Double): Double = (longitude + HALF_TURN).mod(FULL_TURN) - HALF_TURN
    }
}

/** A place the user has kept, with the number the device knows it by. */
data class SavedPlace(
    val id: Long,
    val place: Place,
)
