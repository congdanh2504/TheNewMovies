plugins {
    alias(libs.plugins.themovies.android.library)
}

android {
    namespace = "com.practice.thenewmovies.core.testing"
}

dependencies {
    api(projects.core.connectivity)
    api(projects.core.data.movies)
    api(projects.core.data.auth)
    api(projects.core.data.watchlist)
    api(projects.core.datastore)
    api(projects.core.model)

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.paging.runtime)
}
