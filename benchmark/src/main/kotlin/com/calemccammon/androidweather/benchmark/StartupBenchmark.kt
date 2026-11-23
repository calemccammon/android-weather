package com.calemccammon.androidweather.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Times a cold start of the release build, with and without the baseline
 * profile, so what the profile is worth is a number and not a hope.
 *
 * Run with `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`. Times
 * from an emulator say which way the difference goes and not how big it is
 * on a phone.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** As installed with nothing compiled ahead of time: the worst case. */
    @Test
    fun coldStartWithNothingCompiled() = coldStart(CompilationMode.None())

    /** As installed from a store, with the baseline profile compiled. */
    @Test
    fun coldStartWithBaselineProfile() = coldStart(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun coldStart(compilation: CompilationMode) =
        rule.measureRepeated(
            packageName = APP,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            iterations = ITERATIONS,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
        }

    private companion object {
        const val ITERATIONS = 5
    }
}
