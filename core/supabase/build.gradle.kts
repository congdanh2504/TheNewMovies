import java.util.Properties

plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    require(file.exists()) {
        "local.properties is missing; add SUPABASE_URL=<project url> and SUPABASE_ANON_KEY=<anon key>"
    }
    file.inputStream().use { load(it) }
}

val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL").orEmpty()
val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY").orEmpty()

require(supabaseUrl.isNotBlank()) { "SUPABASE_URL is missing from local.properties" }
require(supabaseAnonKey.isNotBlank()) { "SUPABASE_ANON_KEY is missing from local.properties" }

android {
    namespace = "com.practice.thenewmovies.core.supabase"

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // `api`, not `implementation`: core:data calls client.auth / client.from() directly and
    // catches supabase exception types. See the deviation note in the plan.
    api(platform(libs.supabase.bom))
    api(libs.supabase.auth)
    api(libs.supabase.postgrest)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.core)
}
