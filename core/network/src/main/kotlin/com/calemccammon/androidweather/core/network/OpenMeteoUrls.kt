package com.calemccammon.androidweather.core.network

/**
 * Where the weather service is. The real addresses unless a test that runs
 * the whole app points it at a server of its own.
 */
data class OpenMeteoUrls(
    val geocoding: String = "https://geocoding-api.open-meteo.com/",
    val forecast: String = "https://api.open-meteo.com/",
)
