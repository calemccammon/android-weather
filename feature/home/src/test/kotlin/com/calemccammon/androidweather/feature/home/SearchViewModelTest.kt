package com.calemccammon.androidweather.feature.home

import android.content.Context
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
    private val viewModel by lazy { SearchViewModel(SearchPlaces(places)) }

    private val shown: SearchState
        get() = viewModel.state.value

    @Test
    fun `it starts with nothing to show`() {
        assertEquals(SearchState.Idle, shown)
    }

    @Test
    fun `too little to search for is not searched for`() =
        runTest {
            viewModel.queryChanged(" a ")
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertEquals(emptyList(), places.queries)
        }

    @Test
    fun `it waits for a pause in typing before it searches`() =
        runTest {
            viewModel.queryChanged("ashev")

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
            viewModel.queryChanged("as")
            advanceTimeBy(300.milliseconds)
            viewModel.queryChanged("ashev")
            advanceTimeBy(300.milliseconds)

            assertEquals(emptyList(), places.queries)

            advanceUntilIdle()

            assertEquals(listOf("ashev"), places.queries)
        }

    @Test
    fun `the places found are shown as rows and each row gives its place back`() =
        runTest {
            viewModel.queryChanged("a place")
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

            viewModel.queryChanged("zzzz")
            advanceUntilIdle()

            assertEquals(SearchState.Empty("zzzz"), shown)
            assertNull(viewModel.resultAt(0))
        }

    @Test
    fun `a failure is worded for the user and another try searches at once`() =
        runTest {
            places.failure = WeatherFailure.NoConnection

            viewModel.queryChanged("ashev")
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
            viewModel.queryChanged("ashev")
            advanceUntilIdle()

            val second = CompletableDeferred<Unit>()
            places.gate = second
            viewModel.queryChanged("lisbon")
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
            viewModel.queryChanged("ashev")
            advanceUntilIdle()
            viewModel.queryChanged(" ashev ")
            advanceUntilIdle()

            assertEquals(listOf("ashev"), places.queries)
        }

    @Test
    fun `resetting forgets the text and the results and drops a search on its way`() =
        runTest {
            viewModel.queryChanged("ashev")
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            places.gate = gate
            viewModel.queryChanged("lisbon")
            advanceUntilIdle()

            viewModel.reset()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertNull(viewModel.resultAt(0))

            // The text that was there before can be searched for again.
            places.gate = null
            viewModel.queryChanged("lisbon")
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
