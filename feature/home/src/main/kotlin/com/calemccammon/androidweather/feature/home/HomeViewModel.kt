package com.calemccammon.androidweather.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calemccammon.androidweather.core.domain.repository.LocationRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.domain.usecase.ObserveForecast
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.domain.usecase.RefreshForecast
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.message
import com.calemccammon.androidweather.feature.home.model.ForecastState
import com.calemccammon.androidweather.feature.home.model.ForecastUiModel
import com.calemccammon.androidweather.feature.home.model.HomeEvent
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * The home screen's state, put together from what the device holds.
 *
 * The view model keeps nothing of its own to show. The selected place and
 * the units are settings; the forecast is whatever the database holds for
 * that place. An action here writes to one of those, and the screen changes
 * because what it watches changed. So the screen is the same after the
 * process is killed, with no connection, or when the background refresh
 * brings a newer forecast while it is open.
 *
 * The one thing held here is how the latest fetch is going, which nothing
 * else needs to know.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        observeUnits: ObserveUnits,
        private val observeForecast: ObserveForecast,
        private val refreshForecast: RefreshForecast,
        private val settings: SettingsRepository,
        private val savedPlaces: SavedPlacesRepository,
        private val location: LocationRepository,
    ) : ViewModel() {
        private val selectedPlace: Flow<Place?> = settings.settings.map { it.selectedPlace }.distinctUntilChanged()

        /** The place [fetch] works on: the latest [selectedPlace]. */
        private var place: Place? = null

        /** The fetch under way, if any. */
        private var fetching: Job? = null
        private val fetch = MutableStateFlow<Fetch?>(null)

        private val locating = MutableStateFlow(false)

        private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)

        /** Each event is delivered once, to whoever is collecting. */
        val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<HomeState> =
            combine(selectedPlace, observeUnits(), ::Pair)
                .flatMapLatest { (place, units) -> if (place == null) flowOf(idle(units)) else showing(place, units) }
                .combine(locating) { state, locating -> state.copy(locating = locating) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), idle(Units.Metric))

        init {
            // Each newly selected place is brought up to date, unless what is
            // held for it is fresh enough.
            viewModelScope.launch {
                selectedPlace.collect {
                    place = it
                    fetch(force = false)
                }
            }
        }

        /** Makes [place] the selected place. Its forecast follows. */
        fun select(place: Place) {
            viewModelScope.launch { settings.setSelectedPlace(place) }
        }

        /** Selects a point picked straight off the map. */
        fun selectPoint(
            latitude: Double,
            longitude: Double,
        ) = select(Place.fromCoordinates(latitude, longitude))

        /**
         * Selects the place the device is at. The screen calls this once the
         * user has allowed the app to ask.
         */
        fun selectCurrentLocation() {
            if (locating.value) return
            locating.value = true
            viewModelScope.launch {
                try {
                    val place = location.currentPlace()
                    if (place == null) {
                        eventChannel.send(HomeEvent.LocationUnavailable)
                    } else {
                        settings.setSelectedPlace(place)
                    }
                } finally {
                    locating.value = false
                }
            }
        }

        fun clear() {
            viewModelScope.launch { settings.setSelectedPlace(null) }
        }

        /** Shows forecasts in [chosen] from now on, and remembers the choice. Nothing is fetched. */
        fun setUnits(chosen: UnitsUiModel) {
            viewModelScope.launch { settings.setUnits(chosen.toDomain()) }
        }

        /** Keeps the selected place if it is not saved, and stops keeping it if it is. */
        fun toggleSaved() {
            val place = place ?: return
            val saved = state.value.saved
            viewModelScope.launch { if (saved) savedPlaces.remove(place) else savedPlaces.save(place) }
        }

        /** Fetches the selected place's forecast again, however fresh the one held is. */
        fun retryForecast() = fetch(force = true)

        /**
         * Brings the selected place's forecast up to date if it has grown old.
         * The screen calls this each time it comes back into view: the app may
         * have been in the background for hours.
         */
        fun refreshIfStale() {
            if (fetching?.isActive != true) fetch(force = false)
        }

        private fun fetch(force: Boolean) {
            // A fetch for a place no longer selected is given up.
            fetching?.cancel()
            val place = place ?: return
            fetching =
                viewModelScope.launch {
                    fetch.value = Fetch(place, running = true)
                    fetch.value =
                        try {
                            refreshForecast(place, force)
                            Fetch(place)
                        } catch (e: WeatherException) {
                            // Which forecast was on screen when this failed, so
                            // the failure is not told against a newer one that
                            // the background refresh brings later.
                            val held = observeForecast(place, Units.Metric).first()?.fetchedAt
                            Fetch(place, failure = e.failure, heldAt = held)
                        }
                }
        }

        private fun idle(units: Units) = HomeState(units = UnitsUiModel.from(units))

        private fun showing(
            place: Place,
            units: Units,
        ): Flow<HomeState> =
            combine(observeForecast(place, units), savedPlaces.isSaved(place), fetch) { cached, saved, fetch ->
                HomeState(
                    units = UnitsUiModel.from(units),
                    place = PlaceUiModel.from(place),
                    saved = saved,
                    // How a fetch for some earlier place went says nothing about this one.
                    forecast = forecastState(cached, fetch?.takeIf { it.place == place }, units),
                )
            }

        private fun forecastState(
            cached: CachedForecast?,
            fetch: Fetch?,
            units: Units,
        ): ForecastState =
            when {
                cached != null ->
                    ForecastState.Loaded(
                        forecast = ForecastUiModel.from(cached.forecast, units),
                        fetchedAt = cached.fetchedAt,
                        refreshing = fetch?.running == true,
                        problem = fetch?.failure?.takeIf { fetch.heldAt == cached.fetchedAt }?.message,
                    )
                fetch?.failure != null -> ForecastState.Failed(fetch.failure.message)
                else -> ForecastState.Loading
            }

        /** How the latest fetch for [place] is going, or went. */
        private data class Fetch(
            val place: Place,
            val running: Boolean = false,
            val failure: WeatherFailure? = null,
            /** When the forecast held at the time of [failure] was fetched; null if none was held. */
            val heldAt: Instant? = null,
        )

        private companion object {
            /** Long enough to outlast a rotation, so turning the device does not start everything again. */
            const val STOP_AFTER_MILLIS = 5_000L
        }
    }
