package com.calemccammon.androidweather.widget

import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.domain.usecase.ObserveForecast
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.label
import com.calemccammon.androidweather.core.ui.uiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.roundToInt
import com.calemccammon.androidweather.core.ui.R as CoreR

/** What the widget is showing. */
sealed interface WidgetState {
    /** No place has been chosen in the app. */
    data object NoPlace : WidgetState

    /** A place is chosen, but the device holds no forecast for it yet. */
    data class Waiting(
        val place: UiText,
    ) : WidgetState

    data class Weather(
        val place: UiText,
        /** "12°C". */
        val temperature: UiText,
        /** "Overcast". */
        val condition: UiText,
        /** Today's low and high, "5° / 14°"; null if the forecast has no day for today. */
        val range: UiText?,
        /** The whole widget as one sentence for a screen reader. */
        val spoken: UiText,
    ) : WidgetState
}

/**
 * The widget's state, from the same places the home screen's comes from: the
 * selected place and the units are settings, and the weather is whatever the
 * database holds. The widget fetches nothing; the background refresh keeps
 * the selected place's forecast recent.
 */
class ObserveWidgetState
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val observeUnits: ObserveUnits,
        private val observeForecast: ObserveForecast,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<WidgetState> =
            combine(settings.settings.map { it.selectedPlace }.distinctUntilChanged(), observeUnits(), ::Pair)
                .flatMapLatest { (place, units) ->
                    if (place == null) {
                        flowOf(WidgetState.NoPlace)
                    } else {
                        observeForecast(place, units).map { cached -> state(place, cached?.forecast, units) }
                    }
                }.distinctUntilChanged()

        private fun state(
            place: Place,
            forecast: Forecast?,
            units: Units,
        ): WidgetState {
            val title = PlaceUiModel.from(place).title
            if (forecast == null) return WidgetState.Waiting(title)

            val degrees = forecast.current.temperature.roundToInt()
            val (shown, spoken) =
                when (units) {
                    Units.Metric -> CoreR.string.core_ui_temperature_celsius to CoreR.plurals.core_ui_spoken_celsius
                    Units.Imperial ->
                        CoreR.string.core_ui_temperature_fahrenheit to CoreR.plurals.core_ui_spoken_fahrenheit
                }
            val condition = forecast.current.condition.label
            val today = forecast.days.firstOrNull(forecast::isToday)

            return WidgetState.Weather(
                place = title,
                temperature = uiText(shown, degrees),
                condition = condition,
                range = today?.let { uiText(R.string.widget_range, it.low.roundToInt(), it.high.roundToInt()) },
                spoken = uiText(R.string.widget_spoken, title, condition, UiText.Plural(spoken, degrees)),
            )
        }
    }
