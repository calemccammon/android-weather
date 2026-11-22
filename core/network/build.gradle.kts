plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(libs.okhttp.mockwebserver)
}
