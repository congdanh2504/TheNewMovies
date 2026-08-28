plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.home.impl"
}

dependencies {
    implementation(projects.core.connectivity)
    implementation(projects.core.data)
    implementation(projects.core.datastore)
    implementation(projects.feature.home.api)
    implementation(projects.feature.detail.api)
    implementation(projects.feature.search.api)
}
