import com.android.build.api.dsl.ApplicationExtension
import com.calemccammon.androidweather.buildlogic.configureAndroid
import com.calemccammon.androidweather.buildlogic.configureKotlin
import com.calemccammon.androidweather.buildlogic.configureStaticAnalysis
import com.calemccammon.androidweather.buildlogic.configureUnitTests
import com.calemccammon.androidweather.buildlogic.libs
import com.calemccammon.androidweather.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The app module: an Android application built the way every module is. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = libs.version("android-targetSdk").toInt()
            }
            configureKotlin()
            configureStaticAnalysis()
            configureUnitTests()
        }
}
