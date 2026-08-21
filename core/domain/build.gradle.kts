plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
