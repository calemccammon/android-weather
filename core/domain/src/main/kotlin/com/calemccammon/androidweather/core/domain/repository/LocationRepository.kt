package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.Place

/** Where the device is. */
interface LocationRepository {
    /**
     * The device's whereabouts as a place with no name, or null if the device
     * cannot say: location is turned off, no fix came in time, or the app has
     * not been allowed to ask. Asking for that permission is the screen's job.
     */
    suspend fun currentPlace(): Place?
}
