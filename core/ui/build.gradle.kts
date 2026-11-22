plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.android.compose)
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.designsystem)
}
