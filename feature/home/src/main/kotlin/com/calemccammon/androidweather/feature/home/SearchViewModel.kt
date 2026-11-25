package com.calemccammon.androidweather.feature.home

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.calemccammon.androidweather.core.domain.usecase.SearchPlaces
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.message
import com.calemccammon.androidweather.feature.home.model.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Holds what the user types into the search bar, and turns it into search
 * results.
 *
 * It waits for a pause in typing before searching, and a search is given up
 * when a newer one replaces it, so an old answer never shows over a new one.
 */
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchPlaces: SearchPlaces,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        /**
         * What is typed in the open search bar: the one copy of it, which the
         * bar edits and the searches follow. It is kept in the saved state, so
         * it outlives a rotation and the process being killed.
         */
        val query: TextFieldState by savedState.saveable(saver = TextFieldState.Saver) { TextFieldState() }

        /** The text last asked about; what [retry] asks about again. */
        private var latestQuery = ""

        /** The places behind the rows of the current [SearchState.Results], in order. */
        private var results: List<Place> = emptyList()

        /** The wait for a pause in typing, and the search after it. */
        private var searching: Job? = null

        private val mutableState = MutableStateFlow<SearchState>(SearchState.Idle)
        val state: StateFlow<SearchState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch { snapshotFlow { query.text.toString() }.collect(::queryChanged) }
        }

        private fun queryChanged(text: String) {
            val query = text.trim()
            if (query == latestQuery) return
            latestQuery = query
            searching?.cancel()

            if (!SearchPlaces.isSearchable(query)) {
                mutableState.value = SearchState.Idle
                return
            }
            mutableState.value = SearchState.Loading(query)
            searching =
                viewModelScope.launch {
                    delay(DEBOUNCE)
                    search(query)
                }
        }

        /** Forgets the current query and results, as when the search bar opens. */
        fun reset() {
            query.clearText()
            searching?.cancel()
            latestQuery = ""
            results = emptyList()
            mutableState.value = SearchState.Idle
        }

        /**
         * The place shown in row [index] of the results, or null if there is no
         * such row. The screen hands it on to be selected.
         */
        fun resultAt(index: Int): Place? = if (state.value is SearchState.Results) results.getOrNull(index) else null

        /** Runs the last search again after a failure. */
        fun retry() {
            val query = latestQuery
            if (!SearchPlaces.isSearchable(query)) return
            searching?.cancel()
            mutableState.value = SearchState.Loading(query)
            searching = viewModelScope.launch { search(query) }
        }

        private suspend fun search(query: String) {
            mutableState.value =
                try {
                    val places = searchPlaces(query)
                    results = places
                    if (places.isEmpty()) {
                        SearchState.Empty(query)
                    } else {
                        SearchState.Results(query, places.map(PlaceUiModel::from).toImmutableList())
                    }
                } catch (e: WeatherException) {
                    SearchState.Failed(query, e.failure.message)
                }
        }

        companion object {
            /** How long typing must pause before a search is sent. */
            val DEBOUNCE = 350.milliseconds
        }
    }
