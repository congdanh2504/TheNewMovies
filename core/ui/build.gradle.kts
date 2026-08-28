plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.compose)
}

android {
    namespace = "com.practice.thenewmovies.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
