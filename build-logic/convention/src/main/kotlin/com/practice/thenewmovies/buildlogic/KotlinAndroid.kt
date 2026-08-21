package com.practice.thenewmovies.buildlogic

import ProjectConfigure
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Base Kotlin + Android config shared by the application and library plugins. */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = ProjectConfigure.COMPILE_SDK

        defaultConfig {
            minSdk = ProjectConfigure.MIN_SDK
        }

        compileOptions {
            sourceCompatibility = ProjectConfigure.javaVersion
            targetCompatibility = ProjectConfigure.javaVersion
        }

        sourceSets.configureEach {
            java.srcDirs("src/$name/kotlin")
        }
    }

    configureKotlin()
}

/** Base Kotlin config for pure-JVM modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = ProjectConfigure.javaVersion
        targetCompatibility = ProjectConfigure.javaVersion
    }
    configureKotlin()
}

private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)

            val warningsAsErrors: String? by project
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())

            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xconsistent-data-class-copy-visibility",
                ),
            )
        }
    }
}

/** Derive the AGP resource prefix from the module path: `:core:designsystem` -> `core_designsystem_`. */
internal fun Project.resourcePrefixFromPath(): String =
    path.split("""\W""".toRegex())
        .drop(1)
        .distinct()
        .joinToString(separator = "_")
        .lowercase() + "_"
