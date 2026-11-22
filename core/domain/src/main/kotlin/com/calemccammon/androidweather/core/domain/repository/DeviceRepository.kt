package com.calemccammon.androidweather.core.domain.repository

/** What the app needs to know about the device it runs on. */
interface DeviceRepository {
    /** The country the device is set to, such as "US", if it says. */
    val country: String?
}
