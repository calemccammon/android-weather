plugins {
    alias(libs.plugins.weather.jvm.library)
}

// What tests in other modules share: stand-ins for the repositories, sample
// data, and a rule that gives view models a main dispatcher.
dependencies {
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
