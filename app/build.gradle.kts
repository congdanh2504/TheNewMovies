plugins {
    alias(libs.plugins.themovies.android.application)
    alias(libs.plugins.themovies.android.compose)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.practice.thenewmovies"

    defaultConfig {
        applicationId = "com.practice.thenewmovies"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.data.auth)
    implementation(projects.core.data.watchlist)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.feature.auth.api)
    implementation(projects.feature.auth.impl)
    implementation(projects.feature.detail.api)
    implementation(projects.feature.detail.impl)
    implementation(projects.feature.home.api)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.search.api)
    implementation(projects.feature.search.impl)
    implementation(projects.feature.watchlist.api)
    implementation(projects.feature.watchlist.impl)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    // Applies the packaged baseline profile on first run for devices that do not get it from
    // the Play install. Without this the profile ships but is never installed.
    implementation(libs.androidx.profileinstaller)

    baselineProfile(projects.benchmark)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // :app declares a testInstrumentationRunner, so AGP builds and launches an instrumentation
    // APK even though this module has no androidTest sources. Without the runner artifact that
    // APK dies with ClassNotFoundException, failing the whole-project connectedDebugAndroidTest.
    androidTestImplementation(libs.androidx.test.runner)
}
