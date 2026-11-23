package com.calemccammon.androidweather.feature.places

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.calemccammon.androidweather.core.domain.usecase.SavedPlaceForecast
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.icon
import com.calemccammon.androidweather.core.ui.label
import com.calemccammon.androidweather.core.ui.spokenTemperature
import com.calemccammon.androidweather.core.ui.temperature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

/** What the saved places screen is showing. */
@Immutable
data class PlacesState(
    /** False until the saved places have been read, so "none yet" is not shown in passing. */
    val ready: Boolean = false,
    val places: ImmutableList<SavedPlaceUiModel> = persistentListOf(),
    /** Every place's forecast is being fetched, at the user's asking. */
    val refreshing: Boolean = false,
    /** The user has asked to be told of severe weather at these places. */
    val alertsEnabled: Boolean = false,
)

/** One row: a saved place and, if the device holds its forecast, the weather there. */
@Immutable
data class SavedPlaceUiModel(
    val id: Long,
    val place: PlaceUiModel,
    val weather: SavedPlaceWeatherUiModel?,
) {
    companion object {
        fun from(
            item: SavedPlaceForecast,
            units: Units,
        ) = SavedPlaceUiModel(
            id = item.saved.id,
            place = PlaceUiModel.from(item.saved.place),
            weather =
                item.cached?.forecast?.current?.let { current ->
                    val degrees = current.temperature.roundToInt()
                    SavedPlaceWeatherUiModel(
                        icon = current.condition.icon(isDay = current.isDay),
                        temperature = units.temperature(degrees),
                        condition = current.condition.label,
                        spokenTemperature = units.spokenTemperature(degrees),
                    )
                },
        )
    }
}

@Immutable
data class SavedPlaceWeatherUiModel(
    val icon: ImageVector,
    /** "12°C". */
    val temperature: UiText,
    /** "Overcast". */
    val condition: UiText,
    /** "12 degrees Celsius", for a screen reader. */
    val spokenTemperature: UiText,
)

/** Something that happened once and is to be told once, in a snackbar. */
sealed interface PlacesEvent {
    /**
     * A place was removed; [title] is its name. Handing [removal] to
     * [PlacesViewModel.undoRemove] puts that place, and no other, back.
     */
    data class Removed(
        val title: UiText,
        val removal: Long,
    ) : PlacesEvent

    /** Pulling to refresh could not update every place; [reason] is why, as a sentence. */
    data class RefreshFailed(
        val reason: UiText,
    ) : PlacesEvent
}
