package com.calemccammon.androidweather.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the app's baseline profile: the list of classes and methods used on
 * the way to the first screens, which Android compiles ahead of time when the
 * app is installed, in place of interpreting them on first run.
 *
 * Run with `./gradlew :app:generateBaselineProfile`. The result is committed
 * under `app/src/release/generated/baselineProfiles`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = APP, includeInStartupProfile = true) {
            startToHome()
            visitSavedPlaces()
        }
}
