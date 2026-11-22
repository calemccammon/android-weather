package com.calemccammon.androidweather.core.ui

import androidx.compose.runtime.Immutable
import com.calemccammon.androidweather.core.model.Place
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A place as the screen shows it: in a search result, in the search bar, as
 * the forecast's heading and as the pin on the map.
 */
@Immutable
data class PlaceUiModel(
    /** "Asheville", or "35.60°N, 82.55°W" for a place with no name. */
    val title: UiText,
    /** "North Carolina, United States", or empty when nothing more is known. */
    val subtitle: String,
    /** Where the pin goes. */
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun from(place: Place) =
            PlaceUiModel(
                title = place.name?.let(UiText::Verbatim) ?: coordinates(place.latitude, place.longitude),
                subtitle = listOfNotNull(place.region, place.country).joinToString(", "),
                latitude = place.latitude,
                longitude = place.longitude,
            )

        /** Coordinates are shown to two decimal places of a degree: about a kilometre. */
        private const val HUNDREDTHS = 100

        /** "35.60°N, 82.55°W": what a place with no name is called. */
        private fun coordinates(
            latitude: Double,
            longitude: Double,
        ): UiText {
            val north = (latitude * HUNDREDTHS).roundToLong()
            val east = (longitude * HUNDREDTHS).roundToLong()
            // The side is read off the rounded figure, so a point a hair south
            // of the equator is "0.00°N" like the equator itself.
            return uiText(
                R.string.core_ui_place_coordinates,
                uiText(if (north >= 0) R.string.core_ui_place_north else R.string.core_ui_place_south, degrees(north)),
                uiText(if (east >= 0) R.string.core_ui_place_east else R.string.core_ui_place_west, degrees(east)),
            )
        }

        /** "35.60" for 3560 hundredths of a degree, either side of zero. */
        private fun degrees(hundredths: Long): String {
            val size = abs(hundredths)
            return "${size / HUNDREDTHS}.${(size % HUNDREDTHS).toString().padStart(2, '0')}"
        }
    }
}
