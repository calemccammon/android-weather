package com.calemccammon.androidweather.core.data

import com.calemccammon.androidweather.core.model.Place
import kotlin.math.roundToLong

/** Places are told apart to a hundredth of a degree: about a kilometre. */
private const val HUNDREDTHS = 100

/**
 * What a place's forecast is filed under: its spot on the map, to about a
 * kilometre. Two places closer than that share a forecast, which is finer
 * than the weather service's own grid. The name plays no part, so a point
 * picked off the map and the town found there by search are one spot.
 */
internal val Place.key: String
    get() {
        val longitude = Place.normalizeLongitude(longitude)
        return "${(latitude * HUNDREDTHS).roundToLong()},${(longitude * HUNDREDTHS).roundToLong()}"
    }
