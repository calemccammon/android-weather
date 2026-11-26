package com.calemccammon.androidweather.feature.places

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.domain.usecase.SavedPlaceForecast
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PlacesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val rows =
        listOf(
            SavedPlaceUiModel.from(
                SavedPlaceForecast(SavedPlace(1, Samples.asheville), CachedForecast(Samples.forecast(), Instant.EPOCH)),
                Units.Metric,
            ),
            SavedPlaceUiModel.from(SavedPlaceForecast(SavedPlace(2, Samples.point), cached = null), Units.Metric),
        )

    private var state by mutableStateOf(PlacesState(ready = true, places = rows.toImmutableList()))
    private val events = MutableSharedFlow<PlacesEvent>(extraBufferCapacity = 1)
    private val asked = mutableListOf<String>()

    private fun show() {
        compose.setContent {
            WeatherTheme {
                PlacesScreen(
                    state = state,
                    events = events,
                    onOpen = { asked += "open:$it" },
                    onRemove = { asked += "remove:$it" },
                    onUndoRemove = { asked += "undo:$it" },
                    onRefresh = { asked += "refresh" },
                    onBack = { asked += "back" },
                    onAlertsChange = { asked += "alerts:$it" },
                )
            }
        }
    }

    @Test
    fun `each place is a row read out as one sentence, with its weather if held`() {
        show()

        compose.onNodeWithText("Saved places").assertIsDisplayed()
        compose
            .onNodeWithContentDescription(
                "Asheville, North Carolina, United States: Overcast, 10 degrees Celsius",
            ).assertIsDisplayed()
        // A place with no forecast held is just its name.
        compose.onNodeWithContentDescription("33.86°S, 151.21°E").assertIsDisplayed()
    }

    @Test
    fun `two places of the same name are told apart by where they are`() {
        val portugal = Samples.lisbon
        val ohio =
            Place(latitude = 40.77, longitude = -80.77, name = "Lisbon", region = "Ohio", country = "United States")
        state =
            PlacesState(
                ready = true,
                places =
                    listOf(portugal, ohio)
                        .mapIndexed { i, place ->
                            SavedPlaceUiModel.from(
                                SavedPlaceForecast(SavedPlace(i + 1L, place), cached = null),
                                Units.Metric,
                            )
                        }.toImmutableList(),
            )
        show()

        compose.onNodeWithContentDescription("Lisbon, Lisbon, Portugal").assertIsDisplayed()
        compose.onNodeWithContentDescription("Lisbon, Ohio, United States").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Lisbon, Ohio, United States").performClick()
        assertEquals(listOf("remove:2"), asked)
    }

    @Test
    fun `pressing a row opens it and pressing its bin removes it`() {
        show()

        compose
            .onNodeWithContentDescription(
                "Asheville, North Carolina, United States: Overcast, 10 degrees Celsius",
            ).performClick()
        compose.onNodeWithContentDescription("Remove 33.86°S, 151.21°E").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("open:1", "remove:2", "back"), asked)
    }

    @Test
    fun `with nothing saved it says how to save a place, but not before it knows`() {
        state = PlacesState(ready = false)
        show()

        compose.onNodeWithText("No saved places yet", substring = true).assertDoesNotExist()

        state = PlacesState(ready = true)

        compose.onNodeWithText("No saved places yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a removal is told with a way to undo it`() {
        show()

        events.tryEmit(PlacesEvent.Removed(UiText.Verbatim("Asheville"), removal = 7))

        compose.onNodeWithText("Removed Asheville").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        assertEquals(listOf("undo:7"), asked)
    }

    @Test
    fun `a failed refresh is told`() {
        show()

        events.tryEmit(PlacesEvent.RefreshFailed(UiText.Verbatim("The service is down.")))

        compose.onNodeWithText("Could not update every place. The service is down.").assertIsDisplayed()
    }

    @Test
    fun `the bell says what pressing it will do and reports the press`() {
        show()

        compose.onNodeWithContentDescription("Turn on severe weather alerts").assertIsOff().performClick()
        assertEquals(listOf("alerts:true"), asked)

        state = state.copy(alertsEnabled = true)

        compose.onNodeWithContentDescription("Turn off severe weather alerts").assertIsOn().performClick()
        assertEquals(listOf("alerts:true", "alerts:false"), asked)
    }
}
