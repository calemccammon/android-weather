package com.calemccammon.androidweather.core.testing

import com.calemccammon.androidweather.core.domain.repository.AlertNotifier
import com.calemccammon.androidweather.core.domain.repository.DeviceRepository
import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.LocationRepository
import com.calemccammon.androidweather.core.domain.repository.PlaceSearchRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import com.calemccammon.androidweather.core.model.WeatherAlert
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant

/*
 * Stand-ins for the repositories. Each keeps what it is given in memory and
 * can be told to fail, so a test decides what the world looks like.
 */

/**
 * Holds forecasts in memory. [refresh] "fetches" whatever [onNetwork] holds
 * for the place, or fails with [failure] while that is set.
 */
class FakeForecastRepository(
    private val clock: Clock = MutableClock(),
) : ForecastRepository {
    private val held = MutableStateFlow<Map<Place, CachedForecast>>(emptyMap())

    /** What a refresh brings back, by place. A place missing here gets [Samples.forecast]. */
    val onNetwork = mutableMapOf<Place, Forecast>()

    /** While set, every refresh fails with it. */
    var failure: WeatherFailure? = null

    /** While set, a refresh waits for it before answering. */
    var gate: CompletableDeferred<Unit>? = null

    /** Every place a refresh was asked for, in order. */
    val refreshed = mutableListOf<Place>()

    /** The cut-off of every [prune], in order. */
    val pruned = mutableListOf<Instant>()

    /** Puts a forecast in as if fetched at [fetchedAt]. */
    fun hold(
        place: Place,
        forecast: Forecast = Samples.forecast(),
        fetchedAt: Instant = clock.instant(),
    ) = held.update { it + (place to CachedForecast(forecast, fetchedAt)) }

    fun heldFor(place: Place): CachedForecast? = held.value[place]

    override fun observe(place: Place): Flow<CachedForecast?> = held.map { it[place] }

    override suspend fun refresh(place: Place) {
        refreshed += place
        gate?.await()
        failure?.let { throw WeatherException(it) }
        hold(place, onNetwork[place] ?: Samples.forecast())
    }

    override suspend fun prune(before: Instant) {
        pruned += before
    }
}

class FakePlaceSearchRepository : PlaceSearchRepository {
    /** What a search finds, whatever is asked. */
    var results: List<Place> = emptyList()

    var failure: WeatherFailure? = null

    /** While set, a search waits for it before answering. */
    var gate: CompletableDeferred<Unit>? = null

    /** Every query asked, in order. */
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<Place> {
        queries += query
        gate?.await()
        failure?.let { throw WeatherException(it) }
        return results
    }
}

class FakeSavedPlacesRepository(
    initial: List<Place> = emptyList(),
) : SavedPlacesRepository {
    private var nextId = 1L
    private val saved = MutableStateFlow(initial.map { SavedPlace(nextId++, it) })

    override val places: Flow<List<SavedPlace>> = saved

    val current: List<Place>
        get() = saved.value.map { it.place }

    override fun isSaved(place: Place): Flow<Boolean> = saved.map { all -> all.any { it.place == place } }

    override suspend fun save(place: Place) {
        saved.update { all -> if (all.any { it.place == place }) all else all + SavedPlace(nextId++, place) }
    }

    override suspend fun remove(place: Place) {
        saved.update { all -> all.filterNot { it.place == place } }
    }
}

class FakeSettingsRepository(
    initial: UserSettings = UserSettings(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state

    val current: UserSettings
        get() = state.value

    override suspend fun setUnits(units: Units) = state.update { it.copy(units = units) }

    override suspend fun setSelectedPlace(place: Place?) = state.update { it.copy(selectedPlace = place) }

    override suspend fun setAlertsEnabled(enabled: Boolean) = state.update { it.copy(alertsEnabled = enabled) }

    override suspend fun setNotifiedAlerts(ids: Set<String>) = state.update { it.copy(notifiedAlerts = ids) }
}

class FakeDeviceRepository(
    override var country: String? = "GB",
) : DeviceRepository

class FakeLocationRepository(
    /** Where the device says it is; null when it cannot say. */
    var place: Place? = Samples.point,
) : LocationRepository {
    /** While set, the device takes until it completes to answer. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun currentPlace(): Place? {
        gate?.await()
        return place
    }
}

class FakeAlertNotifier : AlertNotifier {
    /** Every alert shown, in order. */
    val shown = mutableListOf<WeatherAlert>()

    override fun show(alerts: List<WeatherAlert>) {
        shown += alerts
    }
}
