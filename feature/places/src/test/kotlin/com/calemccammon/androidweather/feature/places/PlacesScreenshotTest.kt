package com.calemccammon.androidweather.feature.places

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.domain.usecase.SavedPlaceForecast
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.Samples
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import kotlin.test.Test

/** Pictures of the saved places screen, compared on every run of `verifyRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PlacesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val rows =
        listOf(Samples.asheville to 7.0, Samples.lisbon to 18.0).mapIndexed { i, (place, temperature) ->
            SavedPlaceUiModel.from(
                SavedPlaceForecast(
                    SavedPlace(i + 1L, place),
                    CachedForecast(Samples.forecast(temperature), Instant.EPOCH),
                ),
                Units.Metric,
            )
        } + SavedPlaceUiModel.from(SavedPlaceForecast(SavedPlace(3, Samples.point), cached = null), Units.Metric)

    private fun capture(
        name: String,
        state: PlacesState,
        dark: Boolean = false,
    ) {
        compose.setContent {
            WeatherTheme(dark = dark) {
                PlacesScreen(state = state, onOpen = {}, onRemove = {}, onUndoRemove = {}, onRefresh = {}, onBack = {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    @Test
    fun `saved places`() =
        capture("places", PlacesState(ready = true, places = rows.toImmutableList(), alertsEnabled = true))

    @Test
    fun `saved places in the dark theme`() =
        capture("places_dark", PlacesState(ready = true, places = rows.toImmutableList()), dark = true)

    @Test
    fun `nothing saved`() = capture("places_empty", PlacesState(ready = true))
}
