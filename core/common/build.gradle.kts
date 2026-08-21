plugins {
    alias(libs.plugins.themovies.jvm.library)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
    // hilt-core, NOT hilt-android: the latter is an AAR and cannot resolve for a JVM module.
    implementation(libs.hilt.core)
    // The Hilt compiler must run here too, or DispatchersModule produces no aggregating metadata
    // and :app fails with "MissingBinding ... @Dispatcher(IO) CoroutineDispatcher".
    // ponytail: declared per-module because core:common is the only JVM module needing Hilt;
    // move into a themovies.hilt plugin if a second one appears.
    ksp(libs.hilt.compiler)
}
