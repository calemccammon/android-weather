plugins {
    alias(libs.plugins.weather.android.application)
    alias(libs.plugins.weather.android.compose)
    alias(libs.plugins.weather.hilt)
}

android {
    namespace = "com.calemccammon.androidweather"

    defaultConfig {
        applicationId = "com.calemccammon.androidweather"
        versionCode = 1
        versionName = "1.0"
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
    implementation(projects.sync)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)
    ksp(libs.androidx.hilt.compiler)
}
