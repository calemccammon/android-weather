plugins {
    alias(libs.plugins.weather.android.feature)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.maplibre.compose)
    runtimeOnly(libs.maplibre.compose.runtime.android)
}
