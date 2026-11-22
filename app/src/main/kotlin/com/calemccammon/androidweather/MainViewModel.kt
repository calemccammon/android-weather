package com.calemccammon.androidweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles what the activity is opened with. */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val savedPlaces: SavedPlacesRepository,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        /**
         * Makes the saved place numbered [id] the one the home screen shows, as
         * when an alert about it is pressed. A place removed since is ignored.
         */
        fun openSavedPlace(id: Long) {
            viewModelScope.launch {
                val saved = savedPlaces.places.first().firstOrNull { it.id == id } ?: return@launch
                settings.setSelectedPlace(saved.place)
            }
        }
    }
