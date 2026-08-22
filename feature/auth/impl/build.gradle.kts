plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.auth.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.auth.api)
}
