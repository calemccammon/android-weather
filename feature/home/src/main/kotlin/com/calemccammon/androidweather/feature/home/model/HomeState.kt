package com.calemccammon.androidweather.feature.home.model

import androidx.compose.runtime.Immutable
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.UiText
import java.time.Instant

/** What the forecast area is showing. */
@Immutable
sealed interface ForecastState {
    /** No place is selected, so there is nothing to forecast. */
    data object Idle : ForecastState

    /** Nothing is held for the place yet, and it is being fetched. */
    data object Loading : ForecastState

    /**
     * A forecast held on the device. It may be an old one: [fetchedAt] says
     * how old, and the other two say what is being done about it.
     */
    data class Loaded(
        val forecast: ForecastUiModel,
        val fetchedAt: Instant,
        /** A newer forecast is on its way. */
        val refreshing: Boolean = false,
        /** Why a newer forecast could not be fetched, if one could not. */
        val problem: UiText? = null,
    ) : ForecastState

    /** Nothing is held for the place, and fetching it failed. */
    data class Failed(
        val message: UiText,
    ) : ForecastState
}

/**
 * What the home screen is showing: the selected place, if any, whether it is
 * one of the saved ones, where its forecast has got to, and the units it is in.
 */
@Immutable
data class HomeState(
    val units: UnitsUiModel,
    val place: PlaceUiModel? = null,
    val saved: Boolean = false,
    val forecast: ForecastState = ForecastState.Idle,
    /** The device is being asked where it is. */
    val locating: Boolean = false,
)

/** Something that happened once and is to be told once. */
sealed interface HomeEvent {
    /** The device could not say where it is. */
    data object LocationUnavailable : HomeEvent
}
