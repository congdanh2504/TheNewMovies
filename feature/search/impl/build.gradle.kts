plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.search.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.search.api)
    implementation(projects.feature.detail.api)

    implementation(libs.paging.compose)

    // Search results come only from the network, so the results list cannot be exercised by
    // seeding Room. These tests render the stateless screen over fake PagingData instead.
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.bundles.androidx.compose.ui.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
}
