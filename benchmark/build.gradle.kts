plugins {
    alias(libs.plugins.weather.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.calemccammon.androidweather.benchmark"

    defaultConfig {
        // Macrobenchmark's own floor.
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app these tests start and measure from outside, as a user's device would.
    targetProjectPath = ":app"
}

baselineProfile {
    // Whatever device or emulator is connected.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
