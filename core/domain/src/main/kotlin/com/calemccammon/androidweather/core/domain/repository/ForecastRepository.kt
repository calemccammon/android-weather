package com.calemccammon.androidweather.core.domain.repository

import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Place
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Forecasts, kept on the device.
 *
 * Reading and fetching are separate on purpose. [observe] only ever shows
 * what the device holds, so it works with no connection; [refresh] is the
 * one call that goes to the network, and what it brings back reaches the
 * screen through [observe].
 */
interface ForecastRepository {
    /** The forecast held for [place], in metric units; null while there is none. Emits again when it changes. */
    fun observe(place: Place): Flow<CachedForecast?>

    /**
     * Fetches the forecast for [place] and keeps it. Fails with a
     * [com.calemccammon.androidweather.core.model.WeatherException] and nothing
     * else; what was held before stays.
     */
    suspend fun refresh(place: Place)

    /**
     * Forgets forecasts fetched before [before], so looking at places in
     * passing does not fill the device. Those of saved places are kept, and
     * so are those of [keep].
     */
    suspend fun prune(
        before: Instant,
        keep: Collection<Place> = emptyList(),
    )
}
