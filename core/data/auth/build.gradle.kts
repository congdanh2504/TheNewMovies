plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.data.auth"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.supabase)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk)
}
