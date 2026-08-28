plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.detail.impl"
}

dependencies {
    implementation(projects.core.connectivity)
    implementation(projects.core.data)
    implementation(projects.core.data.watchlist)
    implementation(projects.core.domain)
    implementation(projects.feature.detail.api)
}
