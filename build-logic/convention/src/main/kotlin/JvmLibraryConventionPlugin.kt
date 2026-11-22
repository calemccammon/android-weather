import com.calemccammon.androidweather.buildlogic.configureKotlin
import com.calemccammon.androidweather.buildlogic.configureStaticAnalysis
import com.calemccammon.androidweather.buildlogic.configureUnitTests
import com.calemccammon.androidweather.buildlogic.javaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/** A plain Kotlin module with no Android in it: the model and the domain. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
            configureKotlin()
            configureStaticAnalysis()
            configureUnitTests()
        }
}
