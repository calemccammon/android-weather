plugins {
    alias(libs.plugins.weather.android.application)
    alias(libs.plugins.weather.android.compose)
    alias(libs.plugins.weather.hilt)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.calemccammon.androidweather"

    defaultConfig {
        applicationId = "com.calemccammon.androidweather"
        versionCode = 1
        versionName = "1.0"

        // Starts the tests that run on a device with Hilt's test application
        // in place of the app's own. Each test runs in a fresh process with
        // the app's data cleared, so none sees what another saved.
        testInstrumentationRunner = "com.calemccammon.androidweather.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // There is no release key in this repository; the debug one lets
            // the release build be installed and measured.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // The app module puts the pieces together: it is the only one that sees
    // both the screens and where their data comes from.
    implementation(projects.feature.home)
    implementation(projects.feature.places)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.notifications)
    implementation(projects.sync)
    implementation(projects.widget)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    // Installs the baseline profile where the store that delivered the app does not.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.work.runtime)
    ksp(libs.androidx.hilt.compiler)

    // Where the baseline profile is generated: see the benchmark module.
    baselineProfile(projects.benchmark)

    // Tests of the whole app on a device.
    androidTestImplementation(projects.core.domain)
    androidTestImplementation(projects.core.model)
    androidTestImplementation(projects.core.network)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestUtil(libs.androidx.test.orchestrator)
    kspAndroidTest(libs.hilt.compiler)
}
