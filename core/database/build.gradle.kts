plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.android.room)
    alias(libs.plugins.weather.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
