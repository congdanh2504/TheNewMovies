import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.implementation
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureImplConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("themovies.android.library")
        pluginManager.apply("themovies.android.compose")
        pluginManager.apply("themovies.android.hilt")

        dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:model"))

            implementation(libs["androidx.lifecycle.runtime.compose"])
            implementation(libs["androidx.lifecycle.viewmodel.compose"])
            implementation(libs["androidx.hilt.navigation.compose"])
            implementation(libs["androidx.navigation3.runtime"])
            implementation(libs["coil.compose"])

            testImplementation(project(":core:testing"))
        }
    }
}
