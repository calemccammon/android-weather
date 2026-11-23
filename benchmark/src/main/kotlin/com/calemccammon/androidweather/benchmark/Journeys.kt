package com.calemccammon.androidweather.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val APP = "com.calemccammon.androidweather"

private const val TIMEOUT = 10_000L

/** Starts the app from cold and waits until the home screen can be used. */
internal fun MacrobenchmarkScope.startToHome() {
    pressHome()
    startActivityAndWait()
    device.wait(Until.hasObject(By.desc("Saved places")), TIMEOUT)
}

/** Opens the saved places and comes back, so both screens are drawn. */
internal fun MacrobenchmarkScope.visitSavedPlaces() {
    device.findObject(By.desc("Saved places")).click()
    device.wait(Until.hasObject(By.desc("Back")), TIMEOUT)
    device.findObject(By.desc("Back")).click()
    device.wait(Until.hasObject(By.desc("Saved places")), TIMEOUT)
}
