import com.calemccammon.androidweather.buildlogic.library
import com.calemccammon.androidweather.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: one screen and its view models.
 *
 * A feature sees the domain and the shared UI, and nothing of where data
 * comes from: it cannot name the database, the network or another feature
 * because it does not depend on them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("weather.android.library")
            pluginManager.apply("weather.android.compose")
            pluginManager.apply("weather.hilt")
            // Routes are serializable objects.
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:model"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))

                "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-navigation-compose"))
                "implementation"(libs.library("kotlinx-coroutines-android"))
                "implementation"(libs.library("kotlinx-serialization-json"))

                "testImplementation"(project(":core:testing"))
            }
        }
}
