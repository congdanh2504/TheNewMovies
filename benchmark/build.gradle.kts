plugins {
    // `id`, not `alias`: AGP and the Kotlin plugin already sit on the build classpath through
    // the build-logic included build, so they cannot be re-resolved by version here.
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.practice.thenewmovies.benchmark"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Macrobenchmark measures a real, non-debuggable process; minSdk 24 is not enough
        // because the profile installer and the shell APIs it drives need 28.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

// The baseline-profile plugin wires on-device profile collection into :benchmark:assemble, which
// the root `./gradlew build` reaches. That made a plain build take 11 minutes with a device
// attached and fail outright without one. These tasks run only when the invocation actually names
// a benchmark or baseline-profile task, so `build` stays device-free:
//     ./gradlew :app:generateBaselineProfile          refresh the checked-in profile
//     ./gradlew :benchmark:connectedBenchmarkAndroidTest   run the startup benchmarks
val onDeviceRequested = gradle.startParameter.taskNames.any { name ->
    listOf("BaselineProfile", "connected", "benchmark").any { name.contains(it, ignoreCase = true) }
}
tasks.matching {
    it.name.startsWith("connected") || (it.name.startsWith("collect") && it.name.endsWith("BaselineProfile"))
}.configureEach { enabled = onDeviceRequested }

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.junit)
}
