plugins {
    alias(libs.plugins.weather.jvm.library)
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)
    // Only the annotations: use cases say how they are built, and Hilt, which
    // this module does not know about, reads that.
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}
