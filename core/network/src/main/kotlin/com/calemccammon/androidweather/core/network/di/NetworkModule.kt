package com.calemccammon.androidweather.core.network.di

import com.calemccammon.androidweather.core.network.OpenMeteoNetworkDataSource
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
    fun weatherNetworkDataSource(client: OkHttpClient): WeatherNetworkDataSource = OpenMeteoNetworkDataSource(client)
}
