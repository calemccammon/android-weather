import com.android.build.api.dsl.TestExtension
import com.calemccammon.androidweather.buildlogic.configureAndroid
import com.calemccammon.androidweather.buildlogic.configureKotlin
import com.calemccammon.androidweather.buildlogic.configureStaticAnalysis
import com.calemccammon.androidweather.buildlogic.libs
import com.calemccammon.androidweather.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** A module of tests that drive another module's app from outside, as the benchmarks do. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.test")

            extensions.configure<TestExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = libs.version("android-targetSdk").toInt()
            }
            configureKotlin()
            configureStaticAnalysis()
        }
}
