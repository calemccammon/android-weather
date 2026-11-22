package com.calemccammon.androidweather.feature.places

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.calemccammon.androidweather.core.designsystem.WeatherTheme
import com.calemccammon.androidweather.core.domain.usecase.SavedPlaceForecast
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
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

    private var state by mutableStateOf(PlacesState(ready = true, places = rows))
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
                    onUndoRemove = { asked += "undo" },
                    onRefresh = { asked += "refresh" },
                    onBack = { asked += "back" },
                )
            }
        }
    }

    @Test
    fun `each place is a row read out as one sentence, with its weather if held`() {
        show()

        compose.onNodeWithText("Saved places").assertIsDisplayed()
        compose.onNodeWithContentDescription("Asheville: Overcast, 10 degrees Celsius").assertIsDisplayed()
        // A place with no forecast held is just its name.
        compose.onNodeWithContentDescription("33.86°S, 151.21°E").assertIsDisplayed()
    }

    @Test
    fun `pressing a row opens it and pressing its bin removes it`() {
        show()

        compose.onNodeWithContentDescription("Asheville: Overcast, 10 degrees Celsius").performClick()
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

        events.tryEmit(PlacesEvent.Removed(UiText.Verbatim("Asheville")))

        compose.onNodeWithText("Removed Asheville").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        assertEquals(listOf("undo"), asked)
    }

    @Test
    fun `a failed refresh is told`() {
        show()

        events.tryEmit(PlacesEvent.RefreshFailed)

        compose.onNodeWithText("Could not update every place. Check your connection.").assertIsDisplayed()
    }
}
