package com.practice.thenewmovies.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Applies Spotless with ktlint and the shared license headers. */
internal fun Project.configureSpotless() {
    pluginManager.apply(libs.plugin("spotless").pluginId)

    val ktlintVersion = libs.version("ktlint").requiredVersion
    val headers = rootProject.layout.projectDirectory.dir("spotless")

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            licenseHeaderFile(headers.file("copyright.kt").asFile)
            endWithNewline()
        }
        kotlinGradle {
            target("*.kts")
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            licenseHeaderFile(headers.file("copyright.kts").asFile, "(^|^[^/ ][^*].*)")
            endWithNewline()
        }
        format("xml") {
            target("src/**/*.xml")
            targetExclude("**/build/**/*.xml")
            licenseHeaderFile(headers.file("copyright.xml").asFile, "(<[^!?])")
            endWithNewline()
        }
    }
}
