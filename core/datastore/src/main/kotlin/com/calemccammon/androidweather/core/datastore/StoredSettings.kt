package com.calemccammon.androidweather.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * The settings as they are written to the device. The words and field names
 * here are the file's own, so renaming something in the app's model cannot
 * orphan a saved choice.
 */
@Serializable
data class StoredSettings(
    /** [METRIC], [IMPERIAL], or null if the user has never chosen. */
    val units: String? = null,
    val selectedPlace: StoredPlace? = null,
) {
    companion object {
        const val METRIC = "metric"
        const val IMPERIAL = "imperial"
    }
}

@Serializable
data class StoredPlace(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val region: String? = null,
    val country: String? = null,
)

/** Reads and writes [StoredSettings] as JSON. */
internal object StoredSettingsSerializer : Serializer<StoredSettings> {
    // A field added by a later version is ignored by an earlier one.
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = StoredSettings()

    override suspend fun readFrom(input: InputStream): StoredSettings =
        try {
            json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            // DataStore answers this by starting the file afresh.
            throw CorruptionException("The settings file is not readable", e)
        }

    override suspend fun writeTo(
        t: StoredSettings,
        output: OutputStream,
    ) = output.write(json.encodeToString(t).encodeToByteArray())
}
