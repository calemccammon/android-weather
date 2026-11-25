package com.calemccammon.androidweather.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.uiText
import com.calemccammon.androidweather.feature.home.model.ForecastState
import com.calemccammon.androidweather.feature.home.model.ForecastUiModel
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.SearchState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import com.calemccammon.androidweather.core.ui.R as CoreR

/**
 * Pictures of the home screen, kept in `src/test/snapshots` and compared on
 * every run of `verifyRoborazziDebug`, so a change to how the screen looks is
 * seen in review and not by accident. The map is a flat colour: its tiles
 * come from the network.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val fetchedAt = Instant.parse("2026-11-18T12:00:00Z")
    private val loaded =
        HomeState(
            units = UnitsUiModel.Celsius,
            place = PlaceUiModel.from(Samples.asheville),
            saved = true,
            forecast =
                ForecastState.Loaded(
                    ForecastUiModel.from(Samples.forecast(days = 7), Units.Metric),
                    fetchedAt = fetchedAt,
                ),
        )

    private fun capture(
        name: String,
        home: HomeState,
        dark: Boolean = false,
    ) {
        compose.setContent {
            WeatherTheme(dark = dark) {
                HomeScreen(
                    home = home,
                    query = TextFieldState(),
                    search = SearchState.Idle,
                    now = fetchedAt + Duration.ofMinutes(5),
                    actions = noActions,
                    map = { _, _, _ -> Box(Modifier.fillMaxSize().background(if (dark) darkSea else lightSea)) },
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    @Test
    fun `no place selected`() = capture("home_idle", HomeState(units = UnitsUiModel.Celsius))

    @Test
    fun `a forecast`() = capture("home_forecast", loaded)

    @Test
    fun `a forecast in the dark theme`() = capture("home_forecast_dark", loaded, dark = true)

    @Test
    fun `an old forecast with no connection`() =
        capture(
            "home_forecast_offline",
            loaded.copy(
                forecast =
                    (loaded.forecast as ForecastState.Loaded).copy(
                        problem = uiText(CoreR.string.core_ui_failure_no_connection),
                    ),
            ),
        )

    @Test
    @Config(qualifiers = "w1100dp-h800dp-xhdpi")
    fun `a forecast on a wide window`() = capture("home_forecast_wide", loaded)

    private companion object {
        val lightSea = Color(0xFFAAD3DF)
        val darkSea = Color(0xFF1B2A38)

        val noActions =
            HomeActions(
                onSearchOpened = {},
                onChooseResult = {},
                onRetrySearch = {},
                onSelectPoint = { _, _ -> },
                onClearPlace = {},
                onToggleSaved = {},
                onUnitsChange = {},
                onRetryForecast = {},
                onOpenSavedPlaces = {},
                onMyLocation = {},
            )
    }
}
