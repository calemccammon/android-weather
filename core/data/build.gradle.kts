plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.hilt)
}

dependencies {
    api(projects.core.domain)

    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.location)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
