plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)

    api(libs.paging.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit)

    testImplementation(libs.mockk)
    testImplementation(projects.core.testing)
}
