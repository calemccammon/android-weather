package com.calemccammon.androidweather

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Runs the device tests with a Hilt test application, which lets each test
 * swap parts of the dependency graph, in place of [WeatherApplication].
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        loader: ClassLoader,
        className: String,
        context: Context,
    ): Application = super.newApplication(loader, WeatherTestApplication_Application::class.java.name, context)
}

/**
 * What the test application is built on. The app's manifest leaves setting
 * WorkManager up to the application class, so this one must be able to: the
 * system may start WorkManager's service for a job scheduled by an earlier
 * install before any test has run. A test that drives WorkManager sets up its
 * own in place of this.
 */
open class TestBaseApplication :
    Application(),
    Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

/** Hilt generates `WeatherTestApplication_Application` from this. */
@CustomTestApplication(TestBaseApplication::class)
interface WeatherTestApplication
