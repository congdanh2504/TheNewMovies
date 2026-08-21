plugins {
    alias(libs.plugins.themovies.android.library)
}

android {
    namespace = "com.practice.thenewmovies.core.testing"
}

dependencies {
    api(projects.core.data)
    api(projects.core.datastore)
    api(projects.core.model)

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.paging.runtime)
}
