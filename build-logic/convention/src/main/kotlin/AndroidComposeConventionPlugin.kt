import com.android.build.api.dsl.CommonExtension
import com.calemccammon.androidweather.buildlogic.library
import com.calemccammon.androidweather.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Jetpack Compose for an Android module that already applies the application
 * or library convention: the compiler plugin, the libraries (versions from
 * the Compose BOM) and what its UI tests need.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            dependencies {
                val bom = platform(libs.library("compose-bom"))
                "implementation"(bom)
                "implementation"(libs.library("compose-runtime"))
                "implementation"(libs.library("compose-foundation"))
                "implementation"(libs.library("compose-material3"))
                "implementation"(libs.library("compose-material-icons-extended"))
                "implementation"(libs.library("compose-ui"))
                "implementation"(libs.library("compose-ui-tooling-preview"))
                "debugImplementation"(libs.library("compose-ui-tooling"))

                // Compose tests run on the JVM, drawn by Robolectric.
                "testImplementation"(bom)
                "testImplementation"(libs.library("compose-ui-test-junit4"))
                "testImplementation"(libs.library("robolectric"))
                "testImplementation"(libs.library("androidx-test-core"))
                "testImplementation"(libs.library("androidx-test-ext-junit"))
                "debugImplementation"(libs.library("compose-ui-test-manifest"))
            }
        }
}
