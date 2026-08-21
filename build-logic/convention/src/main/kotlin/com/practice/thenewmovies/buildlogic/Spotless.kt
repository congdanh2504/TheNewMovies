package com.practice.thenewmovies.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.kotlin.dsl.configure

/**
 * Source files matching [includes], with `build/` pruned from the walk.
 *
 * Passing a bare Ant pattern to Spotless filters the *results* but still traverses `build/`, and a
 * parallel build deleting intermediates mid-walk fails the task with "Could not read path".
 */
private fun Project.sourceFiles(vararg includes: String): FileTree =
    fileTree(projectDir) {
        setIncludes(includes.toList())
        exclude("build/**", "**/build/**", ".gradle/**", ".kotlin/**")
    }

/** Applies Spotless with ktlint and the shared license headers. */
internal fun Project.configureSpotless() {
    pluginManager.apply(libs.plugin("spotless").pluginId)

    val ktlintVersion = libs.version("ktlint").requiredVersion
    val headers = rootProject.layout.projectDirectory.dir("spotless")

    extensions.configure<SpotlessExtension> {
        kotlin {
            target(sourceFiles("src/**/*.kt"))
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            licenseHeaderFile(headers.file("copyright.kt").asFile)
            endWithNewline()
        }
        kotlinGradle {
            target(sourceFiles("*.kts"))
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            licenseHeaderFile(headers.file("copyright.kts").asFile, "(^|^[^/ ][^*].*)")
            endWithNewline()
        }
        format("xml") {
            target(sourceFiles("src/**/*.xml"))
            licenseHeaderFile(headers.file("copyright.xml").asFile, "(<[^!?])")
            endWithNewline()
        }
    }
}
