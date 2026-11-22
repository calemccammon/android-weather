package com.calemccammon.androidweather.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.PlaceUiModel
import com.calemccammon.androidweather.core.ui.uiText
import com.calemccammon.androidweather.feature.home.component.MapPin
import com.calemccammon.androidweather.feature.home.model.ForecastState
import com.calemccammon.androidweather.feature.home.model.ForecastUiModel
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.SearchState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import com.calemccammon.androidweather.core.ui.R as CoreR

/**
 * The whole home screen, drawn from plain values with a stand-in for the map,
 * and pressed as a user would press it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val fetchedAt = Instant.parse("2026-11-18T12:00:00Z")
    private val asheville = PlaceUiModel.from(Samples.asheville)
    private val loaded =
        ForecastState.Loaded(ForecastUiModel.from(Samples.forecast(), Units.Metric), fetchedAt = fetchedAt)

    private var home by mutableStateOf(HomeState(units = UnitsUiModel.Celsius))
    private var search by mutableStateOf<SearchState>(SearchState.Idle)

    /** What the screen asked for, in order. */
    private val asked = mutableListOf<String>()
    private var pinDrawn: MapPin? = null

    private val actions =
        HomeActions(
            onQueryChange = { asked += "query:$it" },
            onSearchOpened = { asked += "search opened" },
            onChooseResult = { asked += "choose:$it" },
            onRetrySearch = { asked += "retry search" },
            onSelectPoint = { latitude, longitude -> asked += "point:$latitude,$longitude" },
            onClearPlace = { asked += "clear" },
            onToggleSaved = { asked += "toggle saved" },
            onUnitsChange = { asked += "units:$it" },
            onRetryForecast = { asked += "retry forecast" },
            onOpenSavedPlaces = { asked += "open saved places" },
            onMyLocation = { asked += "my location" },
        )

    private fun show() {
        compose.setContent {
            WeatherTheme {
                HomeScreen(
                    home = home,
                    search = search,
                    now = fetchedAt + Duration.ofMinutes(5),
                    actions = actions,
                    map = { pin, _, onLongPress ->
                        pinDrawn = pin
                        Box(Modifier.fillMaxSize().testTag("map"))
                        // The real map reports a press-and-hold; the stand-in is told to.
                        longPress = onLongPress
                    },
                )
            }
        }
    }

    private var longPress: (Double, Double) -> Unit = { _, _ -> }

    @Test
    fun `with no place it shows the map, the hint and no pin`() {
        show()

        compose.onNodeWithTag("map").assertIsDisplayed()
        compose
            .onNodeWithText("Search for a place, or press and hold on the map, to see its forecast.")
            .assertIsDisplayed()
        compose.onNodeWithText("Search for a place").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear place").assertDoesNotExist()
        assertEquals(null, pinDrawn)
    }

    @Test
    fun `a selected place is named, pinned and forecast`() {
        home = home.copy(place = asheville, forecast = loaded)
        show()

        compose.onNodeWithText("North Carolina, United States").assertIsDisplayed()
        compose
            .onNodeWithContentDescription("Now: Overcast, 10 degrees Celsius, feels like 8, wind 20 km/h")
            .assertIsDisplayed()
        compose.onNodeWithText("Updated 5 minutes ago").assertExists()
        compose.onNodeWithText("Weather data by Open-Meteo.com").assertExists()
        compose
            .onNodeWithContentDescription("Thursday: Rain, low 6°, high 16°, 41% chance of precipitation")
            .assertExists()
        assertEquals(MapPin(35.6, -82.55), pinDrawn)
    }

    @Test
    fun `the save button says which way it will go and reports a press`() {
        home = home.copy(place = asheville, forecast = loaded)
        show()

        compose.onNodeWithContentDescription("Save place").assertIsOff().performClick()
        assertEquals(listOf("toggle saved"), asked)

        home = home.copy(saved = true)

        compose.onNodeWithContentDescription("Remove from saved places").assertIsOn()
    }

    @Test
    fun `the units choice shows the current one and reports the other when pressed`() {
        home = home.copy(place = asheville, forecast = loaded)
        show()

        compose.onNodeWithContentDescription("Celsius and km/h").assertIsSelected()
        compose.onNodeWithContentDescription("Fahrenheit and mph").performScrollTo().performClick()

        assertEquals(listOf("units:Fahrenheit"), asked)
    }

    @Test
    fun `a forecast on its way and a failed one are both told, the failure with another try`() {
        home = home.copy(place = asheville, forecast = ForecastState.Loading)
        show()

        compose.onNodeWithContentDescription("Loading the forecast").assertIsDisplayed()

        home = home.copy(forecast = ForecastState.Failed(uiText(CoreR.string.core_ui_failure_timeout)))

        compose.onNodeWithText("The weather service took too long to answer.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(listOf("retry forecast"), asked)
    }

    @Test
    fun `an old forecast is shown with word of why there is no newer one`() {
        home =
            home.copy(
                place = asheville,
                forecast = loaded.copy(problem = uiText(CoreR.string.core_ui_failure_no_connection)),
            )
        show()

        compose
            .onNodeWithText(
                "Could not reach the weather service. Check your connection. Showing the last forecast fetched.",
            ).assertIsDisplayed()
        compose
            .onNodeWithContentDescription("Now: Overcast, 10 degrees Celsius, feels like 8, wind 20 km/h")
            .assertExists()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(listOf("retry forecast"), asked)
    }

    @Test
    fun `an old forecast being refreshed shows progress over it`() {
        home = home.copy(place = asheville, forecast = loaded.copy(refreshing = true))
        show()

        compose.onNodeWithContentDescription("Loading the forecast").assertIsDisplayed()
        compose.onNodeWithText("Updated 5 minutes ago").assertExists()
    }

    @Test
    fun `the closed bar names the place and offers to clear it and to open the saved places`() {
        home = home.copy(place = asheville, forecast = loaded)
        show()

        compose.onNodeWithContentDescription("Clear place").performClick()
        compose.onNodeWithContentDescription("Saved places").performClick()

        assertEquals(listOf("clear", "open saved places"), asked)
    }

    @Test
    fun `the location button reports a press and shows when the device is finding itself`() {
        show()

        compose.onNodeWithContentDescription("Show the weather where I am").performClick()
        assertEquals(listOf("my location"), asked)

        home = home.copy(locating = true)

        compose.onNodeWithContentDescription("Finding where you are").assertIsDisplayed()
    }

    @Test
    fun `a press-and-hold on the map reports the point`() {
        show()

        compose.runOnIdle { longPress(10.0, 20.0) }

        assertEquals(listOf("point:10.0,20.0"), asked)
    }

    @Test
    fun `opening the bar, typing and choosing a result are each reported`() {
        show()

        compose.onNodeWithText("Search for a place").performClick()
        compose.onNodeWithText("Type a city, town or postcode").assertIsDisplayed()
        assertEquals(listOf("search opened"), asked)

        // The open bar's field is in a window over the closed one's.
        compose.onAllNodesWithContentDescription("Search").onLast().performTextInput("ash")
        compose.waitForIdle()
        assertEquals("query:ash", asked.last())

        search = SearchState.Results("ash", listOf(asheville, PlaceUiModel.from(Samples.lisbon)))
        compose.onNodeWithText("Lisbon, Portugal").assertIsDisplayed()
        compose.onNodeWithText("Asheville").performClick()

        assertEquals("choose:0", asked.last())
        // Choosing closes the bar.
        compose.onNodeWithText("Lisbon, Portugal").assertDoesNotExist()
    }

    @Test
    fun `a search under way, with nothing found, and failed are each told`() {
        show()
        compose.onNodeWithText("Search for a place").performClick()

        search = SearchState.Loading("zz")
        compose.onNodeWithContentDescription("Searching").assertIsDisplayed()

        search = SearchState.Empty("zz")
        compose.onNodeWithText("Nothing found for \"zz\"").assertIsDisplayed()

        search = SearchState.Failed("zz", uiText(CoreR.string.core_ui_failure_unavailable))
        compose.onNodeWithText("The weather service is having trouble. Try again in a moment.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals("retry search", asked.last())
    }

    @Test
    @Config(qualifiers = "w1100dp-h800dp")
    fun `on a wide window the forecast is a panel beside the map`() {
        home = home.copy(place = asheville, forecast = loaded)
        show()

        compose.onNodeWithContentDescription("Drag handle").assertDoesNotExist()
        compose.onNodeWithText("This week").assertIsDisplayed()
        compose.onNodeWithContentDescription("Show the weather where I am").assertIsDisplayed()
    }

    @Test
    fun `on a phone the forecast is a sheet with a handle`() {
        show()

        compose.onNodeWithContentDescription("Drag handle").assertExists()
    }
}
