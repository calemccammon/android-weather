package com.calemccammon.androidweather

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.Units
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals

/**
 * The whole app on a device, as a user goes through it: the real screens,
 * view models, database, settings file and HTTP client, against a stand-in
 * for the weather service.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppJourneyTest {
    private val openMeteo = FakeOpenMeteo()
    private val hilt = HiltAndroidRule(this)
    private val compose = createEmptyComposeRule()

    // The server is up before Hilt builds the app, and the app before the screen is looked at.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(openMeteo).around(hilt).around(compose)

    @Inject lateinit var settings: SettingsRepository

    @Before
    fun chooseUnits() {
        hilt.inject()
        // Otherwise the units follow the country the device is set to.
        runBlocking { settings.setUnits(Units.Metric) }
    }

    private fun launch(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    private fun waitFor(text: String) = compose.waitUntil(TIMEOUT) { visible(text) }

    private fun waitForText(text: String) =
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun visible(text: String) =
        compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    /** Opens the search bar, types [query] and chooses the row named [result]. */
    private fun search(
        query: String,
        result: String,
    ) {
        compose.onNodeWithText("Search for a place").performClick()
        compose.onAllNodesWithContentDescription("Search").onLast().performTextInput(query)
        waitForText(result)
        compose.onNodeWithText(result).performClick()
    }

    @Test
    fun aPlaceIsSearchedForSelectedSavedAndFoundAmongTheSavedPlaces() {
        launch().use {
            compose
                .onNodeWithText("Search for a place, or press and hold on the map, to see its forecast.")
                .assertIsDisplayed()

            search("ashe", result = "North Carolina, United States")

            // The forecast the server gave, after its trip through the database.
            waitFor("Now: Overcast, 7 degrees Celsius")
            compose.onNodeWithText("North Carolina, United States").assertIsDisplayed()

            // The sheet drags up to show the whole week.
            compose.onNodeWithContentDescription("Drag handle").performTouchInput { swipeUp() }
            // On a small screen the end of the forecast is further down still.
            compose.onNodeWithText("Weather data by Open-Meteo.com").performScrollTo().assertIsDisplayed()

            // Saving and removing go through the database, so what follows
            // each is waited for and not expected at once.
            // Back at the top of the forecast, where the bookmark is.
            compose.onNodeWithContentDescription("Save place").performScrollTo().performClick()
            waitFor("Remove from saved places")
            compose.onNodeWithContentDescription("Remove from saved places").assertIsOn()

            compose.onNodeWithContentDescription("Saved places").performClick()
            waitFor("Asheville, North Carolina, United States: Overcast, 7 degrees Celsius")

            compose.onNodeWithContentDescription("Remove Asheville, North Carolina, United States").performClick()
            waitForText("Removed Asheville")
            compose.onNodeWithText("Undo").performClick()
            waitFor("Asheville, North Carolina, United States: Overcast, 7 degrees Celsius")
        }
    }

    @Test
    fun theAppReopensOnItsLastPlaceWithTheServiceDown() {
        launch().use {
            search("lis", result = "Lisbon, Portugal")
            waitFor("Now: Clear, 18 degrees Celsius")
        }
        val fetchedBefore = openMeteo.requests.count { it.startsWith("/v1/forecast") }
        openMeteo.down = true

        // A new activity and new view models: nothing on screen is left over
        // from before, so what shows was read from the device.
        launch().use {
            waitFor("Now: Clear, 18 degrees Celsius")
            compose.onNodeWithText("Lisbon, Portugal").assertIsDisplayed()
            // How long ago depends on how fast the device is.
            waitForText("Updated ")

            // Changing units needs no connection either.
            compose.onNodeWithContentDescription("Fahrenheit and mph").performScrollTo().performClick()
            waitFor("Now: Clear, 64 degrees Fahrenheit")
        }
        // The forecast was fresh, so the service was not even asked.
        assertEquals(fetchedBefore, openMeteo.requests.count { it.startsWith("/v1/forecast") })
    }

    @Test
    fun aSearchWithNoMatchesSaysSoAndOneThatFailsSaysThat() {
        launch().use {
            compose.onNodeWithText("Search for a place").performClick()
            compose.onAllNodesWithContentDescription("Search").onLast().performTextInput("zzzz")
            waitForText("Nothing found")

            openMeteo.down = true
            compose.onAllNodesWithContentDescription("Search").onLast().performTextInput("z")
            waitForText("The weather service is having trouble")
        }
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
