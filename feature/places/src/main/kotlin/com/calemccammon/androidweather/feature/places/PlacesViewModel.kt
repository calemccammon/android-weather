package com.calemccammon.androidweather.feature.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.domain.usecase.ObserveSavedPlaceForecasts
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The saved places and the weather the device holds for each.
 *
 * Like the home screen's, this view model shows only what the database
 * holds: the rows change when a place is saved or removed anywhere, or when
 * the background refresh brings newer forecasts.
 */
@HiltViewModel
class PlacesViewModel
    @Inject
    constructor(
        observeUnits: ObserveUnits,
        observeSavedPlaceForecasts: ObserveSavedPlaceForecasts,
        private val refreshSavedPlaces: RefreshSavedPlaces,
        private val savedPlaces: SavedPlacesRepository,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        /** The places behind the rows on screen, by row id. */
        private var shown: Map<Long, Place> = emptyMap()

        /** Places removed and not put back, by the number their removal was given, for [undoRemove]. */
        private val removed = mutableMapOf<Long, Place>()
        private var removals = 0L

        private val refreshing = MutableStateFlow(false)

        private val eventChannel = Channel<PlacesEvent>(Channel.BUFFERED)

        /** Each event is delivered once, to whoever is collecting. */
        val events: Flow<PlacesEvent> = eventChannel.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<PlacesState> =
            observeUnits()
                .flatMapLatest { units ->
                    observeSavedPlaceForecasts(units).map { items ->
                        shown = items.associate { it.saved.id to it.saved.place }
                        items.map { SavedPlaceUiModel.from(it, units) }.toImmutableList()
                    }
                }.combine(refreshing) { places, refreshing -> PlacesState(ready = true, places, refreshing) }
                .combine(settings.settings.map { it.alertsEnabled }.distinctUntilChanged()) { state, alerts ->
                    state.copy(alertsEnabled = alerts)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), PlacesState())

        /** Makes the place in row [id] the one the home screen shows. */
        fun open(id: Long) {
            val place = shown[id] ?: return
            viewModelScope.launch { settings.setSelectedPlace(place) }
        }

        fun remove(id: Long) {
            val place = shown[id] ?: return
            viewModelScope.launch {
                savedPlaces.remove(place)
                val removal = ++removals
                removed[removal] = place
                eventChannel.send(PlacesEvent.Removed(PlaceUiModel.from(place).title, removal))
            }
        }

        /**
         * Puts back the place that [removal] took away. It returns at the end
         * of the list. Each removal can be undone once, and undoing one leaves
         * the others as they were.
         */
        fun undoRemove(removal: Long) {
            val place = removed.remove(removal) ?: return
            viewModelScope.launch { savedPlaces.save(place) }
        }

        /**
         * Turns alerts of severe weather on or off. The screen turns them on
         * only once the system will let the app notify.
         */
        fun setAlertsEnabled(enabled: Boolean) {
            viewModelScope.launch { settings.setAlertsEnabled(enabled) }
        }

        /** Fetches every saved place's forecast now. */
        fun refresh() {
            if (refreshing.value) return
            refreshing.value = true
            viewModelScope.launch {
                try {
                    // Whatever kept one place from updating most likely kept the rest.
                    refreshSavedPlaces().failures.firstOrNull()?.let {
                        eventChannel.send(PlacesEvent.RefreshFailed(it.message))
                    }
                } finally {
                    refreshing.value = false
                }
            }
        }

        private companion object {
            /** Long enough to outlast a rotation. */
            const val STOP_AFTER_MILLIS = 5_000L
        }
    }
