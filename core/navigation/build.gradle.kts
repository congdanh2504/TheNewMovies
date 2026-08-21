plugins {
    alias(libs.plugins.themovies.android.library)
}

android {
    namespace = "com.practice.thenewmovies.core.navigation"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.navigation3.runtime)

    // The BOM must be repeated for the test configuration: compose-runtime has no version
    // of its own in the catalog, and `api(platform(...))` does not reach testImplementation.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.runtime)
}
