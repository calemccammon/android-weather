package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.DeviceRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.Units
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Decides which units to show a forecast in. */
class ObserveUnits
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val device: DeviceRepository,
    ) {
        /**
         * The user's own choice if they have made one; otherwise the units of
         * the country the device is set to.
         */
        operator fun invoke(): Flow<Units> =
            settings.settings
                .map { it.units ?: Units.forCountry(device.country) }
                .distinctUntilChanged()
    }
