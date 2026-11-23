package com.calemccammon.androidweather.core.network.di

import com.calemccammon.androidweather.core.network.OpenMeteoNetworkDataSource
import com.calemccammon.androidweather.core.network.OpenMeteoUrls
import com.calemccammon.androidweather.core.network.WeatherNetworkDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OpenMeteoNetworkDataSource.httpClient()

    @Provides
    @Singleton
    fun weatherNetworkDataSource(
        client: OkHttpClient,
        urls: OpenMeteoUrls,
    ): WeatherNetworkDataSource = OpenMeteoNetworkDataSource(client, urls)
}

/**
 * The service's real addresses. A module of its own, and public, so that a
 * test of the whole app can put another in its place and leave the rest of
 * the network code as shipped.
 */
@Module
@InstallIn(SingletonComponent::class)
object OpenMeteoUrlsModule {
    @Provides
    fun openMeteoUrls(): OpenMeteoUrls = OpenMeteoUrls()
}
