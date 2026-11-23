package com.calemccammon.androidweather.core.network

import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.network.model.PlaceDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.io.IOException
import java.io.InterruptedIOException
import java.time.DateTimeException
import java.time.Duration

/**
 * Places and forecasts from Open-Meteo. No API key is needed.
 *
 * Asks the service, reads its answers into the wire-shaped models in `model`,
 * and hands back the app's own. Every way this can go wrong leaves as a
 * [WeatherException].
 */
internal class OpenMeteoNetworkDataSource(
    client: OkHttpClient,
    urls: OpenMeteoUrls = OpenMeteoUrls(),
) : WeatherNetworkDataSource {
    private val converter = json.asConverterFactory("application/json".toMediaType())
    private val geocoding: GeocodingApi = retrofit(client, urls.geocoding).create()
    private val forecasts: ForecastApi = retrofit(client, urls.forecast).create()

    override suspend fun searchPlaces(query: String): List<Place> =
        ask { geocoding.search(query, MAX_RESULTS).results.map(PlaceDto::toDomain) }

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Forecast = ask { forecasts.forecast(latitude, longitude, FORECAST_DAYS).toDomain() }

    private fun retrofit(
        client: OkHttpClient,
        url: String,
    ) = Retrofit
        .Builder()
        .baseUrl(url)
        .client(client)
        .addConverterFactory(converter)
        .build()

    /** Runs [request], turning each way it can fail into the app's own word for it. */
    private suspend fun <T> ask(request: suspend () -> T): T =
        try {
            request()
        } catch (e: CancellationException) {
            // The caller gave up waiting; that is not a failure to report.
            throw e
        } catch (e: HttpException) {
            throw e.toWeatherException()
        } catch (e: InterruptedIOException) {
            // OkHttp's timeouts, the socket's among them, are all of this kind.
            throw WeatherException(WeatherFailure.Timeout, e.message, e)
        } catch (e: IOException) {
            // A dropped socket, a failed TLS handshake, a refused connection.
            throw WeatherException(WeatherFailure.NoConnection, e.message, e)
        } catch (e: IllegalArgumentException) {
            // Not JSON, or not the expected shape.
            throw WeatherException(WeatherFailure.BadResponse, e.message, e)
        } catch (e: DateTimeException) {
            throw WeatherException(WeatherFailure.BadResponse, e.message, e)
        }

    private fun HttpException.toWeatherException(): WeatherException {
        val status = code()
        // The service's own trouble, or too many requests: its answer, often a
        // proxy's error page, is not read at all.
        if (status >= FIRST_SERVER_ERROR || status == TOO_MANY_REQUESTS) {
            return WeatherException(WeatherFailure.Unavailable, "HTTP $status", this)
        }
        // Bad input comes back as HTTP 400 with {"error": true, "reason": "..."}.
        val reason =
            runCatching {
                response()?.errorBody()?.string()?.let { json.decodeFromString<ErrorDto>(it).reason }
            }.getOrNull()
        return WeatherException(WeatherFailure.Rejected, reason ?: "HTTP $status", this)
    }

    /** What the service sends with an error status. */
    @Serializable
    private data class ErrorDto(
        val reason: String? = null,
    )

    companion object {
        /** How long to wait for an answer before giving up. */
        val defaultTimeout: Duration = Duration.ofSeconds(15)

        private const val MAX_RESULTS = 8
        private const val FORECAST_DAYS = 7
        private const val FIRST_SERVER_ERROR = 500
        private const val TOO_MANY_REQUESTS = 429

        private val json = Json { ignoreUnknownKeys = true }

        /** The HTTP client the service is asked through: one that gives up after [timeout]. */
        fun httpClient(timeout: Duration = defaultTimeout): OkHttpClient =
            OkHttpClient
                .Builder()
                .callTimeout(timeout)
                .build()
    }
}
