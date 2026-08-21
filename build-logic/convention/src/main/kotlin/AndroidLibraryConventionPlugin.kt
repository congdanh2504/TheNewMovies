import com.android.build.gradle.LibraryExtension
import com.practice.thenewmovies.buildlogic.configureKotlinAndroid
import com.practice.thenewmovies.buildlogic.configureSpotless
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import com.practice.thenewmovies.buildlogic.resourcePrefixFromPath
import com.practice.thenewmovies.buildlogic.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("kotlin.android").pluginId)
        pluginManager.apply(libs.plugin("android.library").pluginId)

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = ProjectConfigure.TARGET_SDK
            resourcePrefix = resourcePrefixFromPath()
            testOptions.animationsDisabled = true
        }

        dependencies {
            testImplementation(libs["junit"])
            testImplementation(libs["kotlinx.coroutines.test"])
            testImplementation(libs["turbine"])
        }

        configureSpotless()
    }
}
