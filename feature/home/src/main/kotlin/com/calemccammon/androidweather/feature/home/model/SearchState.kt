package com.calemccammon.androidweather.feature.home.model

import androidx.compose.runtime.Immutable
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.UiText
import kotlinx.collections.immutable.ImmutableList

/** What the open search bar is showing under itself. */
@Immutable
sealed interface SearchState {
    /** Nothing typed yet, or too little to search for. */
    data object Idle : SearchState

    /** Waiting, either for the user to stop typing or for the answer. */
    data class Loading(
        val query: String,
    ) : SearchState

    data class Results(
        val query: String,
        val places: ImmutableList<PlaceUiModel>,
    ) : SearchState

    data class Empty(
        val query: String,
    ) : SearchState

    data class Failed(
        val query: String,
        val message: UiText,
    ) : SearchState
}
