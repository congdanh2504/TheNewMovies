import java.util.Properties

plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

val tmdbApiKey: String = Properties().apply {
    val file = rootProject.file("local.properties")
    require(file.exists()) { "local.properties is missing; add TMDB_API_KEY=<your token>" }
    file.inputStream().use { load(it) }
}.getProperty("TMDB_API_KEY").orEmpty()

require(tmdbApiKey.isNotBlank()) { "TMDB_API_KEY is missing from local.properties" }

android {
    namespace = "com.practice.thenewmovies.core.network"

    defaultConfig {
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
}
