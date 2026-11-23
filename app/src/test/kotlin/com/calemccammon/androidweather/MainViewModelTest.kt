package com.calemccammon.androidweather

import com.calemccammon.androidweather.core.testing.FakeSavedPlacesRepository
import com.calemccammon.androidweather.core.testing.FakeSettingsRepository
import com.calemccammon.androidweather.core.testing.MainDispatcherRule
import com.calemccammon.androidweather.core.testing.Samples
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val saved = FakeSavedPlacesRepository(listOf(Samples.asheville, Samples.lisbon))
    private val settings = FakeSettingsRepository()
    private val viewModel = MainViewModel(saved, settings)

    @Test
    fun `an alert pressed makes the saved place it was about the one shown`() =
        runTest {
            viewModel.openSavedPlaceAt(Samples.lisbon.latitude, Samples.lisbon.longitude)
            advanceUntilIdle()

            assertEquals(Samples.lisbon, settings.current.selectedPlace)
        }

    @Test
    fun `an alert about a place removed since changes nothing`() =
        runTest {
            saved.remove(Samples.lisbon)

            viewModel.openSavedPlaceAt(Samples.lisbon.latitude, Samples.lisbon.longitude)
            advanceUntilIdle()

            assertNull(settings.current.selectedPlace)
        }
}
