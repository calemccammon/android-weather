package com.calemccammon.androidweather.core.notifications.di

import com.calemccammon.androidweather.core.domain.repository.AlertNotifier
import com.calemccammon.androidweather.core.notifications.SystemAlertNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    abstract fun alertNotifier(notifier: SystemAlertNotifier): AlertNotifier
}
