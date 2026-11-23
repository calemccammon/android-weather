import com.android.build.api.dsl.LibraryExtension
import com.calemccammon.androidweather.buildlogic.configureAndroid
import com.calemccammon.androidweather.buildlogic.configureKotlin
import com.calemccammon.androidweather.buildlogic.configureStaticAnalysis
import com.calemccammon.androidweather.buildlogic.configureUnitTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * An Android library module. Its namespace follows from its path:
 * `:core:database` is `com.calemccammon.androidweather.core.database`.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                namespace = "com.calemccammon.androidweather" + path.replace(':', '.')
                // Resources of different modules cannot collide: each is prefixed
                // with its module's name, such as `feature_home_`.
                resourcePrefix = path.removePrefix(":").replace(':', '_') + "_"
            }
            configureKotlin()
            configureStaticAnalysis()
            configureUnitTests()
        }
}
