plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.compose)
}

android {
    namespace = "com.practice.thenewmovies.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
}
