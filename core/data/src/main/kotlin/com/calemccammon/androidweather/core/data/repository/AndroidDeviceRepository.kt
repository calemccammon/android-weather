package com.calemccammon.androidweather.core.data.repository

import android.content.Context
import com.calemccammon.androidweather.core.domain.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidDeviceRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : DeviceRepository {
        override val country: String?
            get() =
                context.resources.configuration.locales[0]
                    .country
                    .ifEmpty { null }
    }
