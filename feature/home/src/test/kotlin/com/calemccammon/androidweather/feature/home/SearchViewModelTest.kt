package com.calemccammon.androidweather.feature.home

import android.content.Context
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.calemccammon.androidweather.core.domain.usecase.SearchPlaces
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.testing.FakePlaceSearchRepository
import com.calemccammon.androidweather.core.testing.MainDispatcherRule
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.feature.home.model.SearchState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val UiText.words get() = asString(resources)

    private val places = FakePlaceSearchRepository().apply { results = listOf(Samples.asheville, Samples.lisbon) }
    private val viewModel by lazy { SearchViewModel(SearchPlaces(places), SavedStateHandle()) }

    /**
     * Types [text] into the search bar's query, replacing what was there, as a
     * user would, then lets the view model see it, as the next frame would.
     */
    private fun TestScope.type(text: String) {
        viewModel.query.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
        runCurrent()
    }

    private val shown: SearchState
        get() = viewModel.state.value

    @Test
    fun `it starts with nothing to show`() {
        assertEquals(SearchState.Idle, shown)
    }

    @Test
    fun `too little to search for is not searched for`() =
        runTest {
            type(" a ")
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertEquals(emptyList(), places.queries)
        }

    @Test
    fun `it waits for a pause in typing before it searches`() =
        runTest {
            type("ashev")

            assertEquals(SearchState.Loading("ashev"), shown)
            advanceTimeBy(SearchViewModel.DEBOUNCE - 1.milliseconds)
            assertEquals(emptyList(), places.queries)

            advanceTimeBy(1.milliseconds)
            runCurrent()

            assertEquals(listOf("ashev"), places.queries)
        }

    @Test
    fun `typing on starts the wait again so only the last text is searched for`() =
        runTest {
            type("as")
            advanceTimeBy(300.milliseconds)
            type("ashev")
            advanceTimeBy(300.milliseconds)

            assertEquals(emptyList(), places.queries)

            advanceUntilIdle()

            assertEquals(listOf("ashev"), places.queries)
        }

    @Test
    fun `the places found are shown as rows and each row gives its place back`() =
        runTest {
            type("a place")
            advanceUntilIdle()

            val results = assertIs<SearchState.Results>(shown)
            assertEquals("a place", results.query)
            assertEquals(listOf("Asheville", "Lisbon"), results.places.map { it.title.words })
            assertEquals("North Carolina, United States", results.places.first().subtitle)
            assertEquals(Samples.lisbon, viewModel.resultAt(1))
            assertNull(viewModel.resultAt(2))
            assertNull(viewModel.resultAt(-1))
        }

    @Test
    fun `finding nothing is said as such`() =
        runTest {
            places.results = emptyList()

            type("zzzz")
            advanceUntilIdle()

            assertEquals(SearchState.Empty("zzzz"), shown)
            assertNull(viewModel.resultAt(0))
        }

    @Test
    fun `a failure is worded for the user and another try searches at once`() =
        runTest {
            places.failure = WeatherFailure.NoConnection

            type("ashev")
            advanceUntilIdle()

            val failed = assertIs<SearchState.Failed>(shown)
            assertEquals("Could not reach the weather service. Check your connection.", failed.message.words)

            places.failure = null
            viewModel.retry()

            assertEquals(SearchState.Loading("ashev"), shown)
            runCurrent()
            assertIs<SearchState.Results>(shown)
            assertEquals(listOf("ashev", "ashev"), places.queries)
        }

    @Test
    fun `the answer to an older search never replaces a newer one`() =
        runTest {
            val first = CompletableDeferred<Unit>()
            places.gate = first
            type("ashev")
            advanceUntilIdle()

            val second = CompletableDeferred<Unit>()
            places.gate = second
            type("lisbon")
            advanceUntilIdle()
            first.complete(Unit)
            advanceUntilIdle()

            assertEquals(SearchState.Loading("lisbon"), shown)

            places.results = listOf(Samples.lisbon)
            second.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("Lisbon"), assertIs<SearchState.Results>(shown).places.map { it.title.words })
            assertEquals(Samples.lisbon, viewModel.resultAt(0))
        }

    @Test
    fun `the same text typed again is not searched for twice`() =
        runTest {
            type("ashev")
            advanceUntilIdle()
            type(" ashev ")
            advanceUntilIdle()

            assertEquals(listOf("ashev"), places.queries)
        }

    @Test
    fun `resetting forgets the text and the results and drops a search on its way`() =
        runTest {
            type("ashev")
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            places.gate = gate
            type("lisbon")
            advanceUntilIdle()

            viewModel.reset()
            Snapshot.sendApplyNotifications() // the frames between opening the bar and typing in it
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals("", viewModel.query.text.toString())
            assertEquals(SearchState.Idle, shown)
            assertNull(viewModel.resultAt(0))

            // The text that was there before can be searched for again.
            places.gate = null
            type("lisbon")
            advanceUntilIdle()

            assertIs<SearchState.Results>(shown)
        }

    @Test
    fun `there is nothing to try again before anything is typed`() =
        runTest {
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertEquals(emptyList(), places.queries)
        }
}
