plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.datastore)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
