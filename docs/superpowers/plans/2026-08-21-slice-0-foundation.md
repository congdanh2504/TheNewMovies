# TheNewMovies Slice 0 — Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the Gradle build — included build-logic with convention plugins, version catalog, Spotless — plus the two dependency-free modules `core:model` and `core:common`.

**Architecture:** All Gradle configuration lives in convention plugins inside an included build (`build-logic`), so every module build file is a dozen lines. Typesafe project accessors are on, so modules reference each other as `projects.core.model`. `core:model` and `core:common` are pure-Kotlin JVM libraries with no Android dependency.

**Tech Stack:** Gradle 8.11.1, AGP 8.10.0, Kotlin 2.1.20, JDK 17, Spotless 8.10.0 + ktlint 1.8.0.

**Reference repo:** `/Users/danhtruong/android/TheMovies` — a working app with the same versions. Its `build-logic` is the base for the convention plugins here.

**Spec:** `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`

---

## File Structure

| File | Responsibility |
| --- | --- |
| `settings.gradle.kts` | Included build, repository filters, module list, JDK check |
| `gradle.properties` | Daemon memory, caching, AndroidX flags |
| `gradle/libs.versions.toml` | Single source of dependency versions |
| `build.gradle.kts` | Every plugin declared `apply false` |
| `build-logic/settings.gradle.kts` | Re-imports the version catalog for plugin code |
| `build-logic/convention/build.gradle.kts` | Registers each convention plugin id |
| `build-logic/convention/src/main/kotlin/ProjectConfigure.kt` | SDK levels and Java version constants |
| `build-logic/convention/src/main/kotlin/com/practice/thenewmovies/buildlogic/libs.kt` | Version catalog accessors for plugin code |
| `.../buildlogic/DependencyHandlerExt.kt` | `implementation(...)`, `ksp(...)` helpers |
| `.../buildlogic/KotlinAndroid.kt` | Shared Kotlin/Android compile config |
| `.../buildlogic/Spotless.kt` | Shared Spotless config |
| `.../kotlin/AndroidApplicationConventionPlugin.kt` | `themovies.android.application` |
| `.../kotlin/AndroidLibraryConventionPlugin.kt` | `themovies.android.library` |
| `.../kotlin/AndroidComposeConventionPlugin.kt` | `themovies.android.compose` |
| `.../kotlin/AndroidHiltConventionPlugin.kt` | `themovies.android.hilt` |
| `.../kotlin/AndroidRoomConventionPlugin.kt` | `themovies.android.room` |
| `.../kotlin/JvmLibraryConventionPlugin.kt` | `themovies.jvm.library` |
| `.../kotlin/AndroidFeatureApiConventionPlugin.kt` | `themovies.android.feature.api` |
| `.../kotlin/AndroidFeatureImplConventionPlugin.kt` | `themovies.android.feature.impl` |
| `spotless/copyright.kt`, `copyright.kts`, `copyright.xml` | License header templates |
| `.editorconfig` | ktlint rule overrides |
| `core/model/src/main/kotlin/.../core/model/*.kt` | Domain models, one file per concept |
| `core/common/src/main/kotlin/.../core/common/network/Dispatchers.kt` | Dispatcher qualifier + enum |
| `core/common/src/main/kotlin/.../core/common/network/di/DispatchersModule.kt` | Binds real dispatchers |

Base package is `com.practice.thenewmovies`. Source roots are `src/main/kotlin`, never `src/main/java`.

---

### Task 1: Repository skeleton and Gradle wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`
- Create: `gradle.properties`
- Modify: `.gitignore`

- [ ] **Step 1: Copy the Gradle wrapper from the reference repo**

There is no `gradle` CLI on this machine, so the wrapper must be copied rather than generated.

```bash
cd /Users/danhtruong/android/TheNewMovies
cp -R /Users/danhtruong/android/TheMovies/gradle/wrapper gradle/wrapper
cp /Users/danhtruong/android/TheMovies/gradlew .
cp /Users/danhtruong/android/TheMovies/gradlew.bat .
chmod +x gradlew
```

- [ ] **Step 2: Verify the wrapper resolves**

Run: `./gradlew --version`
Expected: prints `Gradle 8.11.1` and `JVM: 17` or higher. If the JVM line shows 11 or lower, stop and fix `JAVA_HOME` — nothing later in this plan will work.

- [ ] **Step 3: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn

android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.resvalues=false
android.defaults.buildfeatures.shaders=false

kotlin.code.style=official
kotlin.daemon.jvmargs=-Xmx4096m

# Set to true in CI to fail the build on compiler warnings.
warningsAsErrors=false
```

- [ ] **Step 4: Extend `.gitignore`**

The repo already has a `.gitignore` from the spec commit. Replace it with:

```gitignore
build/
.gradle/
local.properties
.idea/
*.iml
.DS_Store
.kotlin/
captures/
```

- [ ] **Step 5: Commit**

```bash
git add gradle gradlew gradlew.bat gradle.properties .gitignore
git commit -m "build: add Gradle wrapper 8.11.1 and gradle.properties"
```

---

### Task 2: Version catalog

**Files:**
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: Write the catalog**

Seeded from the reference repo's catalog, minus WorkManager and benchmark entries (no sync module, no benchmarks), plus the entries this project needs that the reference lacks: `navigation3-runtime`, `lifecycle-viewmodel-compose`, `coroutines-core`, `javax-inject`, `room-testing`, `androidx-test-core`, and Spotless.

```toml
[versions]
agp = "8.10.0"
kotlin = "2.1.20"
ksp = "2.1.20-2.0.1"
hilt = "2.56.2"
hiltNavigationCompose = "1.2.0"
room = "2.7.1"
retrofit = "2.11.0"
moshi = "1.15.2"
okhttp = "4.12.0"
coilCompose = "2.7.0"
paging = "3.3.6"
datastore = "1.1.4"
navigation3 = "1.0.0"
lifecycle = "2.9.0"
lifecycleViewmodelNavigation3 = "2.10.0"
composeBom = "2025.05.00"
activityCompose = "1.10.1"
coreKtx = "1.16.0"
coreSplashscreen = "1.0.1"
appcompat = "1.7.0"
material = "1.12.0"
accompanistSystemuicontroller = "0.32.0"
kotlinxSerializationJson = "1.7.3"
coroutines = "1.10.2"
timber = "5.0.1"
javaxInject = "1"
androidDesugarJdkLibs = "2.1.5"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
androidxTestRunner = "1.6.2"
androidxTestCore = "1.6.1"
turbine = "1.2.0"
mockk = "1.13.17"
spotless = "8.10.0"
ktlint = "1.8.0"

[libraries]
# Kotlin / coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
javax-inject = { module = "javax.inject:javax.inject", version.ref = "javaxInject" }

# AndroidX core
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "coreSplashscreen" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
material = { module = "com.google.android.material:material", version.ref = "material" }

# Compose
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-runtime = { module = "androidx.compose.runtime:runtime" }
androidx-compose-runtime-saveable = { module = "androidx.compose.runtime:runtime-saveable" }
androidx-ui = { module = "androidx.compose.ui:ui" }
androidx-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }

# Lifecycle
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Navigation 3
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNavigation3" }

# DI
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-core = { module = "com.google.dagger:hilt-core", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }
androidx-hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Data
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
room-gradlePlugin = { module = "androidx.room:room-gradle-plugin", version.ref = "room" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
moshi = { module = "com.squareup.moshi:moshi", version.ref = "moshi" }
moshi-kotlin = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
paging-runtime = { module = "androidx.paging:paging-runtime-ktx", version.ref = "paging" }
paging-compose = { module = "androidx.paging:paging-compose", version.ref = "paging" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# Misc
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coilCompose" }
accompanist-systemuicontroller = { module = "com.google.accompanist:accompanist-systemuicontroller", version.ref = "accompanistSystemuicontroller" }
timber = { module = "com.jakewharton.timber:timber", version.ref = "timber" }
android-desugarJdkLibs = { module = "com.android.tools:desugar_jdk_libs", version.ref = "androidDesugarJdkLibs" }

# Test
junit = { module = "junit:junit", version.ref = "junit" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
androidx-junit = { module = "androidx.test.ext:junit", version.ref = "junitVersion" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espressoCore" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
androidx-test-core = { module = "androidx.test:core-ktx", version.ref = "androidxTestCore" }

# Gradle plugins needed on the build-logic classpath
android-gradlePlugin = { module = "com.android.tools.build:gradle", version.ref = "agp" }
kotlin-gradlePlugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
spotless-gradlePlugin = { module = "com.diffplug.spotless:spotless-plugin-gradle", version.ref = "spotless" }

[bundles]
androidx-compose-ui-test = ["androidx-ui-test-junit4", "androidx-ui-test-manifest"]

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }

# Plugins defined by this project
themovies-android-application = { id = "themovies.android.application", version = "unspecified" }
themovies-android-library = { id = "themovies.android.library", version = "unspecified" }
themovies-android-compose = { id = "themovies.android.compose", version = "unspecified" }
themovies-android-hilt = { id = "themovies.android.hilt", version = "unspecified" }
themovies-android-room = { id = "themovies.android.room", version = "unspecified" }
themovies-android-feature-api = { id = "themovies.android.feature.api", version = "unspecified" }
themovies-android-feature-impl = { id = "themovies.android.feature.impl", version = "unspecified" }
themovies-jvm-library = { id = "themovies.jvm.library", version = "unspecified" }
```

- [ ] **Step 2: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add version catalog"
```

---

### Task 3: build-logic included build

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/convention/build.gradle.kts`

- [ ] **Step 1: Write `build-logic/settings.gradle.kts`**

```kotlin
@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

- [ ] **Step 2: Write `build-logic/convention/build.gradle.kts`**

Every plugin this project defines is registered here. `compileOnly` on the Gradle plugins keeps them off the runtime classpath.

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.practice.thenewmovies.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "themovies.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "themovies.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "themovies.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "themovies.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "themovies.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("androidFeatureApi") {
            id = "themovies.android.feature.api"
            implementationClass = "AndroidFeatureApiConventionPlugin"
        }
        register("androidFeatureImpl") {
            id = "themovies.android.feature.impl"
            implementationClass = "AndroidFeatureImplConventionPlugin"
        }
        register("jvmLibrary") {
            id = "themovies.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add build-logic
git commit -m "build: add build-logic included build"
```

---

### Task 4: Shared helpers for plugin code

**Files:**
- Create: `build-logic/convention/src/main/kotlin/ProjectConfigure.kt`
- Create: `build-logic/convention/src/main/kotlin/com/practice/thenewmovies/buildlogic/libs.kt`
- Create: `build-logic/convention/src/main/kotlin/com/practice/thenewmovies/buildlogic/DependencyHandlerExt.kt`
- Create: `build-logic/convention/src/main/kotlin/com/practice/thenewmovies/buildlogic/KotlinAndroid.kt`
- Create: `build-logic/convention/src/main/kotlin/com/practice/thenewmovies/buildlogic/Spotless.kt`

- [ ] **Step 1: Write `ProjectConfigure.kt`**

Deliberately in the root package so plugin classes (also root-package) can reference it without an import.

```kotlin
import org.gradle.api.JavaVersion

object ProjectConfigure {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val MIN_SDK = 24
    val javaVersion = JavaVersion.VERSION_17
}
```

- [ ] **Step 2: Write `libs.kt`**

```kotlin
package com.practice.thenewmovies.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

/** The `libs` version catalog, readable from plugin code. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal operator fun VersionCatalog.get(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun VersionCatalog.version(alias: String): VersionConstraint =
    findVersion(alias).get()

internal fun VersionCatalog.plugin(alias: String): PluginDependency =
    findPlugin(alias).get().get()
```

- [ ] **Step 3: Write `DependencyHandlerExt.kt`**

```kotlin
package com.practice.thenewmovies.buildlogic

import org.gradle.api.artifacts.dsl.DependencyHandler

internal fun DependencyHandler.implementation(dependency: Any) = add("implementation", dependency)
internal fun DependencyHandler.api(dependency: Any) = add("api", dependency)
internal fun DependencyHandler.ksp(dependency: Any) = add("ksp", dependency)
internal fun DependencyHandler.testImplementation(dependency: Any) = add("testImplementation", dependency)
internal fun DependencyHandler.androidTestImplementation(dependency: Any) =
    add("androidTestImplementation", dependency)
internal fun DependencyHandler.debugImplementation(dependency: Any) = add("debugImplementation", dependency)
```

- [ ] **Step 4: Write `KotlinAndroid.kt`**

`resourcePrefix` is derived from the module path, so a resource in `:core:designsystem` must be named `core_designsystem_*`. That rule is enforced by AGP, and later slices rename resources accordingly.

```kotlin
package com.practice.thenewmovies.buildlogic

import ProjectConfigure
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
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
```

- [ ] **Step 5: Write `Spotless.kt`**

```kotlin
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
```

- [ ] **Step 6: Commit**

```bash
git add build-logic/convention/src
git commit -m "build: add shared helpers for convention plugins"
```

---

### Task 5: Application, library, compose, hilt, room and jvm plugins

**Files:**
- Create: `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidRoomConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/JvmLibraryConventionPlugin.kt`

- [ ] **Step 1: Write `AndroidApplicationConventionPlugin.kt`**

```kotlin
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
```

- [ ] **Step 2: Write `AndroidLibraryConventionPlugin.kt`**

Every library gets the unit-test stack, so no module build file repeats it.

```kotlin
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
```

- [ ] **Step 3: Write `AndroidComposeConventionPlugin.kt`**

One plugin covers both application and library modules by checking which AGP plugin is present. (The spec listed separate `library.compose` and `application.compose` plugins; a single plugin with this branch does the same job in half the code.)

```kotlin
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
```

- [ ] **Step 4: Write `AndroidHiltConventionPlugin.kt`**

```kotlin
import com.practice.thenewmovies.buildlogic.get
import com.practice.thenewmovies.buildlogic.implementation
import com.practice.thenewmovies.buildlogic.ksp
import com.practice.thenewmovies.buildlogic.libs
import com.practice.thenewmovies.buildlogic.plugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugin("ksp").pluginId)
        pluginManager.apply(libs.plugin("hilt").pluginId)

        dependencies {
            implementation(libs["hilt.android"])
            ksp(libs["hilt.compiler"])
        }
    }
}
```

- [ ] **Step 5: Write `AndroidRoomConventionPlugin.kt`**

Uses the Room Gradle plugin so the schema directory is declared in Kotlin rather than as a raw KSP argument.

```kotlin
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
```

The Room Gradle plugin class is already on the build-logic classpath via
`compileOnly(libs.room.gradlePlugin)` in Task 3.

- [ ] **Step 6: Write `JvmLibraryConventionPlugin.kt`**

```kotlin
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
```

- [ ] **Step 7: Commit**

```bash
git add build-logic/convention
git commit -m "build: add application, library, compose, hilt, room and jvm convention plugins"
```

---

### Task 6: Feature api and impl convention plugins

**Files:**
- Create: `build-logic/convention/src/main/kotlin/AndroidFeatureApiConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidFeatureImplConventionPlugin.kt`

These reference `:core:navigation`, `:core:ui` and `:core:designsystem`, which do not exist yet. That is fine — the plugins are only resolved when a module applies them, and the first module to do so is created in slice 3, after those modules exist.

- [ ] **Step 1: Write `AndroidFeatureApiConventionPlugin.kt`**

An api module holds navigation keys and navigate extensions, nothing more. `api(project(":core:navigation"))` so consumers see `Navigator` without declaring it.

```kotlin
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
```

- [ ] **Step 2: Write `AndroidFeatureImplConventionPlugin.kt`**

An impl module holds screens, ViewModels and the entry function. Everything a feature always needs is declared here so the module build file only lists what is specific to it.

```kotlin
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
```

- [ ] **Step 3: Commit**

```bash
git add build-logic/convention/src/main/kotlin/AndroidFeature*.kt
git commit -m "build: add feature api and impl convention plugins"
```

---

### Task 7: Root build files, license headers and editorconfig

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `spotless/copyright.kt`, `spotless/copyright.kts`, `spotless/copyright.xml`
- Create: `.editorconfig`

- [ ] **Step 1: Write `settings.gradle.kts`**

Only the modules that exist so far are included. Later slices add their own `include` lines.

```kotlin
@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "TheNewMovies"

include(":core:model")
include(":core:common")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Requires JDK 17+ but is using JDK ${JavaVersion.current()}"
}
```

- [ ] **Step 2: Write the root `build.gradle.kts`**

Every plugin used anywhere is declared once here with `apply false`, so all subprojects share one buildscript classpath.

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless) apply false
}
```

- [ ] **Step 3: Write the license header templates**

`spotless/copyright.kt`:

```kotlin
/*
 * Copyright $YEAR TheNewMovies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

`spotless/copyright.kts` — identical content to `copyright.kt`. Copy it:

```bash
cp spotless/copyright.kt spotless/copyright.kts
```

`spotless/copyright.xml`:

```xml
<!--
  Copyright $YEAR TheNewMovies

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
```

- [ ] **Step 4: Write `.editorconfig`**

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 4
trim_trailing_whitespace = true

[*.{kt,kts}]
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
ktlint_function_naming_ignore_when_annotated_with = Composable, Test
ktlint_standard_function-signature = disabled
ktlint_standard_class-signature = disabled
ktlint_standard_chain-method-continuation = disabled
ktlint_standard_multiline-expression-wrapping = disabled
ktlint_standard_string-template-indent = disabled
ktlint_standard_function-expression-body = disabled

[*.{xml,json,yml,yaml,toml}]
indent_size = 2
```

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts spotless .editorconfig
git commit -m "build: add root build files, license headers and editorconfig"
```

---

### Task 8: `core:model`

**Files:**
- Create: `core/model/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/Movie.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/MovieDetail.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/Cast.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/Review.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/WatchlistMovie.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/MovieCategory.kt`

`MoviesPage` from the reference repo is **not** ported: paging is handled by `PagingData` in `core:data`, so nothing consumes a page wrapper. The reference `MovieDetail` and `Genre` share a file there; here `Genre` stays with `MovieDetail` because the two always change together.

- [ ] **Step 1: Write `core/model/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.jvm.library)
}
```

- [ ] **Step 2: Write the models**

`Movie.kt`:

```kotlin
package com.practice.thenewmovies.core.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
)
```

`MovieDetail.kt`:

```kotlin
package com.practice.thenewmovies.core.model

data class MovieDetail(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val originalLanguage: String,
    val overview: String?,
    val genres: List<Genre>,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val runtime: Long?,
    val status: String,
    val video: Boolean,
    val voteAverage: Double,
    val voteCount: Int,
)

data class Genre(
    val id: Int,
    val name: String,
)
```

`Cast.kt`:

```kotlin
package com.practice.thenewmovies.core.model

data class Cast(
    val castId: Int,
    val character: String,
    val name: String,
    val profilePath: String?,
)
```

`Review.kt`:

```kotlin
package com.practice.thenewmovies.core.model

data class Review(
    val author: String,
    val content: String,
    val createdAt: String,
    val avatarPath: String? = null,
    val rating: Float? = null,
)
```

`WatchlistMovie.kt`:

```kotlin
package com.practice.thenewmovies.core.model

data class WatchlistMovie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val runtime: Int,
    val genre: String,
    val userRating: Float? = null,
)
```

`MovieCategory.kt`:

```kotlin
package com.practice.thenewmovies.core.model

enum class MovieCategory {
    POPULAR,
    TOP_RATED,
    NOW_PLAYING,
    UPCOMING,
}
```

- [ ] **Step 3: Build the module**

Run: `./gradlew :core:model:build`
Expected: `BUILD SUCCESSFUL`. If Spotless complains about missing license headers, run `./gradlew :core:model:spotlessApply` and re-run.

- [ ] **Step 4: Commit**

```bash
git add core/model
git commit -m "feat(model): add domain models"
```

---

### Task 9: `core:common`

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/main/kotlin/com/practice/thenewmovies/core/common/network/MoviesDispatchers.kt`
- Create: `core/common/src/main/kotlin/com/practice/thenewmovies/core/common/network/di/DispatchersModule.kt`
- Test: `core/common/src/test/kotlin/com/practice/thenewmovies/core/common/network/MoviesDispatchersTest.kt`

`core:common` holds only the dispatcher qualifier. The spec also listed a generic `Result` + `asResult()`; it is **not** implemented, because with Room as the single source of truth no repository method returns a `Result` — refresh failures surface as a `Boolean` and are turned into an error message by the ViewModel. Add `Result` later if a second consumer appears.

`DispatchersModule` needs Hilt, and Hilt's Android plugin cannot apply to a JVM module, so the JVM module depends on `javax.inject` and `dagger` directly rather than applying `themovies.android.hilt`.

- [ ] **Step 1: Write `core/common/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
    // hilt-core, NOT hilt-android: the latter is an AAR and cannot resolve for a JVM module.
    implementation(libs.hilt.core)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.common.network

import org.junit.Assert.assertEquals
import org.junit.Test

class MoviesDispatchersTest {

    @Test
    fun `qualifier carries the dispatcher it annotates`() {
        val annotation = Holder::class.java
            .getDeclaredField("value")
            .getAnnotation(Dispatcher::class.java)

        assertEquals(MoviesDispatchers.IO, annotation?.dispatcher)
    }

    private class Holder {
        // `@field:` is required: a bare property annotation lands on the Kotlin property,
        // not the Java field, so reflection over the field would find nothing.
        @field:Dispatcher(MoviesDispatchers.IO)
        @JvmField
        var value: Int = 0
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :core:common:test`
Expected: compilation failure — `Unresolved reference: Dispatcher`.

- [ ] **Step 4: Write `MoviesDispatchers.kt`**

Retention must be `RUNTIME` for both Hilt and the test above to see the annotation.

```kotlin
package com.practice.thenewmovies.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val dispatcher: MoviesDispatchers)

enum class MoviesDispatchers {
    Default,
    IO,
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :core:common:test`
Expected: `BUILD SUCCESSFUL`, 1 test passed.

- [ ] **Step 6: Write `DispatchersModule.kt`**

```kotlin
package com.practice.thenewmovies.core.common.network.di

import com.practice.thenewmovies.core.common.network.Dispatcher
import com.practice.thenewmovies.core.common.network.MoviesDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {

    @Provides
    @Dispatcher(MoviesDispatchers.IO)
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(MoviesDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

- [ ] **Step 7: Build and commit**

Run: `./gradlew :core:common:build`
Expected: `BUILD SUCCESSFUL`

```bash
git add core/common
git commit -m "feat(common): add injected dispatcher qualifiers"
```

---

### Task 10: Verify the whole slice

- [ ] **Step 1: Run formatting**

Run: `./gradlew spotlessApply`
Expected: `BUILD SUCCESSFUL`. If Spotless fails to resolve `com.diffplug.spotless:8.10.0`, check that `gradlePluginPortal()` is in both repository blocks in `settings.gradle.kts` and in `build-logic/settings.gradle.kts`.

- [ ] **Step 2: Build everything**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`, with `:core:model` and `:core:common` compiled and tested.

- [ ] **Step 3: Confirm the convention plugins are registered**

Run: `./gradlew :build-logic:convention:validatePlugins`
Expected: `BUILD SUCCESSFUL` — this catches plugin-implementation mistakes that would otherwise only surface when a module applies the plugin.

- [ ] **Step 4: Commit any formatting fixes**

```bash
git add -A
git commit -m "style: apply spotless"
```

---

## Done when

- `./gradlew build` succeeds from a clean checkout.
- `./gradlew spotlessCheck` passes.
- `core:model` and `core:common` compile as JVM libraries with no Android dependency.
- Eight convention plugin ids are registered and validated.
