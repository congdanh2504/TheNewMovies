import com.practice.thenewmovies.buildlogic.api
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.implementation
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureApiConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("themovies.android.library")
        pluginManager.apply(libs.plugin("kotlin.serialization").pluginId)

        dependencies {
            api(project(":core:navigation"))
            implementation(libs["kotlinx.serialization.json"])
        }
    }
}
