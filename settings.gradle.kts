@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "TheNewMovies"

include(":app")
include(":core:common")
include(":core:connectivity")
include(":core:data")
include(":core:data:auth")
include(":core:data:watchlist")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:navigation")
include(":core:network")
include(":core:supabase")
include(":core:testing")
include(":core:ui")
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:detail:api")
include(":feature:detail:impl")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:watchlist:api")
include(":feature:watchlist:impl")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Requires JDK 17+ but is using JDK ${JavaVersion.current()}"
}
