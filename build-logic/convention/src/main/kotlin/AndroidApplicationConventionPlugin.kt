import com.android.build.api.dsl.ApplicationExtension
import com.practice.thenewmovies.buildlogic.configureKotlinAndroid
import com.practice.thenewmovies.buildlogic.configureSpotless
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("kotlin.android").pluginId)
        pluginManager.apply(libs.plugin("android.application").pluginId)

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = ProjectConfigure.TARGET_SDK
            buildFeatures.buildConfig = true
            testOptions.animationsDisabled = true
        }

        configureSpotless()
    }
}
