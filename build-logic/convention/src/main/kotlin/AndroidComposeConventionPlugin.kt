import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.practice.thenewmovies.buildlogic.debugImplementation
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.implementation
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("kotlin.compose").pluginId)

        when {
            pluginManager.hasPlugin("com.android.application") ->
                extensions.getByType<ApplicationExtension>().buildFeatures.compose = true

            pluginManager.hasPlugin("com.android.library") ->
                extensions.getByType<LibraryExtension>().buildFeatures.compose = true

            else -> error("themovies.android.compose requires the application or library plugin")
        }

        dependencies {
            val bom = platform(libs["androidx.compose.bom"])
            implementation(bom)
            implementation(libs["androidx.ui"])
            implementation(libs["androidx.ui.graphics"])
            implementation(libs["androidx.ui.tooling.preview"])
            implementation(libs["androidx.material3"])
            debugImplementation(libs["androidx.ui.tooling"])
        }
    }
}
