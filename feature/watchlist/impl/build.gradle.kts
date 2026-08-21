plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.watchlist.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.watchlist.api)
    implementation(projects.feature.detail.api)
}
