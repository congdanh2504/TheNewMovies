plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.room)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies.core.database"
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
