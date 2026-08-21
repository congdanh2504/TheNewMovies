import com.practice.thenewmovies.buildlogic.configureKotlinJvm
import com.practice.thenewmovies.buildlogic.configureSpotless
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import com.practice.thenewmovies.buildlogic.testImplementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("kotlin.jvm").pluginId)

        configureKotlinJvm()

        dependencies {
            testImplementation(libs["junit"])
            testImplementation(libs["kotlinx.coroutines.test"])
        }

        configureSpotless()
    }
}
