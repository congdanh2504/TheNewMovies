plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies.core.data.watchlist"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.data.auth)
    implementation(projects.core.database)
    implementation(projects.core.supabase)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk)
}
