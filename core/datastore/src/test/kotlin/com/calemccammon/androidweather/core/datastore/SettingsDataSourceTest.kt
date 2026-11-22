package com.calemccammon.androidweather.core.datastore

import com.calemccammon.androidweather.core.datastore.di.settingsStore
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

/** The settings store over a real file in a temporary folder. */
class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { folder.newFile("settings.json").also { it.delete() } }

    // One store per file at a time, so each test makes its own when it is ready.
    private fun source() = SettingsDataSource(settingsStore { file })

    private val asheville = Place(35.6, -82.55, "Asheville", "North Carolina", "United States")

    @Test
    fun `nothing is chosen before anything is saved`() =
        runTest {
            assertEquals(UserSettings(), source().settings.first())
        }

    @Test
    fun `what is chosen is read back, and written in the file's own words`() =
        runTest {
            val source = source()

            source.setUnits(Units.Imperial)
            source.setSelectedPlace(asheville)

            assertEquals(UserSettings(Units.Imperial, asheville), source.settings.first())
            val written = file.readText()
            assertEquals(true, written.contains(""""units":"imperial""""), written)
            assertEquals(true, written.contains(""""name":"Asheville""""), written)
        }

    @Test
    fun `a place can be cleared and the units changed back`() =
        runTest {
            val source = source()
            source.setUnits(Units.Imperial)
            source.setSelectedPlace(Place(1.0, 2.0))

            source.setUnits(Units.Metric)
            source.setSelectedPlace(null)

            assertEquals(UserSettings(Units.Metric), source.settings.first())
        }

    @Test
    fun `a file from a later version is read for what this one knows`() =
        runTest {
            file.writeText("""{"units":"kelvin","theme":"dark","selectedPlace":{"latitude":1.0,"longitude":2.0}}""")

            assertEquals(UserSettings(units = null, selectedPlace = Place(1.0, 2.0)), source().settings.first())
        }

    @Test
    fun `a file that cannot be read is started afresh`() =
        runTest {
            file.writeText("{not json")
            val source = source()

            assertEquals(UserSettings(), source.settings.first())

            source.setUnits(Units.Metric)
            assertEquals(UserSettings(Units.Metric), source.settings.first())
        }
}
