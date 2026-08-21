import androidx.room.gradle.RoomExtension
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.implementation
import com.practice.thenewmovies.buildlogic.ksp
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("ksp").pluginId)
        pluginManager.apply(libs.plugin("room").pluginId)

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies {
            implementation(libs["room.runtime"])
            implementation(libs["room.ktx"])
            ksp(libs["room.compiler"])
        }
    }
}
