plugins {
    // `id`, not `alias`: :core:data still has sources and applies these plugins itself, so a
    // nested project cannot re-resolve them by version. Reverts to `alias` in the task that
    // empties :core:data.
    id("themovies.android.library")
    id("themovies.android.hilt")
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
