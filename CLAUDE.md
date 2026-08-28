# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Setup

`core:network/build.gradle.kts` reads `TMDB_API_KEY` (a TMDB **v4 read access token**) from
`local.properties` at configure time. Without it, *every* Gradle invocation fails — including
`./gradlew tasks`. The file is git-ignored; never commit the token.

## Commands

```bash
./gradlew assembleDebug                       # build
./gradlew installDebug                        # build and install on a connected device
./gradlew spotlessApply                       # format (required — spotlessCheck runs in `build`)
./gradlew testDebugUnitTest                   # all unit tests
./gradlew build                               # everything: assemble + unit tests + spotlessCheck + lint

# one module / one class / one method
./gradlew :feature:home:impl:testDebugUnitTest
./gradlew :core:data:movies:testDebugUnitTest --tests '*OfflineFirstMoviesRepositoryTest'
./gradlew :core:data:movies:testDebugUnitTest --tests '*OfflineFirstMoviesRepositoryTest.refresh skips the network while the cache is fresh'

# instrumented (needs a device/emulator): Room DAO tests + Compose UI tests
./gradlew connectedDebugAndroidTest
./gradlew :core:database:connectedDebugAndroidTest
```

Spotless enforces a license header (`spotless/copyright.*`) on every `.kt`, `.kts`, and `.xml`.
New files fail `build` until `spotlessApply` inserts it.

## Architecture

Read `README.md` for the module map and data flow. What matters when editing:

- **Features are split `api` / `impl`.** `api` holds only the `NavKey` and a navigate extension;
  `impl` holds the ViewModel, screen, and entry function. A feature depends on another feature's
  `api` only — never its `impl`. `:app` is the sole module that sees the whole graph, and it
  contains exactly one composable file (`ui/MoviesApp.kt`); do not add screens there.
- **Room is the single source of truth.** All reads are `Flow`s from DAOs. The network is touched
  only by `refresh(category)` / `refreshDetail(id)`, which no-op inside a 24h TTL
  (`OfflineFirstMoviesRepository`), plus search paging (not persisted). `MovieEntity` is keyed on
  `(id, category)` so one movie can sit in several categories.
- **Repository interfaces are public, implementations `internal` + `@Binds`.** Same for
  `UserPreferencesRepository`. Tests use the fakes in `core:testing`, already on the test
  classpath of every feature via the convention plugin.
- **`core:designsystem` knows no app model**; `core:ui` holds composites shared by 2+ features.
  A component used by one screen belongs in that feature.
- **Navigation 3 with one back stack per tab** (`core:navigation/NavigationState`). `NavDisplay`
  dispatches back only when its own stack has >1 entry, so `:app` adds a `BackHandler` for the
  tab-root case. Test `Navigator` directly, but remember unit tests can't catch app-level
  wiring gaps like that one.
- **`core:data` is split by domain** (`movies`, `auth`, `watchlist`) and holds no sources itself —
  it is only the container project, so it has no `build.gradle.kts` and no `include` line. A
  feature depends on the domains it names and no others; `core:connectivity` holds `NetworkMonitor`.
  Adding a repository means picking a domain module, not growing a shared one.
- **Thin domain layer.** `core:domain` holds `GetMovieDetailUseCase` only — the one place several
  repositories combine. Do not add pass-through use cases; ViewModels call repositories.

## Build logic

Convention plugins live in the included build `build-logic` (ids prefixed `themovies.`):
`android.application`, `android.library`, `android.compose`, `android.hilt`, `android.room`,
`android.feature.api`, `android.feature.impl`, `jvm.library`. Module build files should apply a
plugin and declare dependencies — nothing else. A dependency every feature needs goes in
`AndroidFeatureImplConventionPlugin`, not in each module.

- Uses typesafe project accessors (`projects.core.data`) — enabled in `settings.gradle.kts`.
- `resourcePrefix` is derived from the module path, so `core:ui` resources must start `core_ui_`.
- SDKs and Java version are in `ProjectConfigure.kt` (compile/target 36, min 24, Java 17).
- Pure-JVM modules need `hilt-core`, not `hilt-android` (AAR, won't resolve), **and**
  `ksp(libs.hilt.compiler)` — without KSP the module's `@Module` produces no aggregating metadata
  and Hilt reports `MissingBinding` from `:app`.

## Gotchas that already bit this repo

`docs/superpowers/plans/README.md` records all 14 with full diagnosis. The ones likely to recur:

- **Moshi must use codegen, never reflection.** DTOs carry `@JsonClass(generateAdapter = true)`
  and `core:network` declares `ksp(libs.moshi.kotlin.codegen)`. Adding
  `KotlinJsonAdapterFactory` back makes release builds silently parse nothing (R8 renames the
  fields) while debug keeps working. A green `assembleRelease` proves nothing — install and run it.
- **`runCatching` in the repositories swallows parse errors**, so a broken API surfaces only as
  "Couldn't load movies". Add logging before assuming the network is at fault.
- **Instrumented tests can report a green zero.** `tests="0" failures="0"` means the runner never
  found them. Check the XML in `build/outputs/androidTest-results/`, not just the exit code.
- **A test APK has no INTERNET permission**, so Coil inside any list row kills the instrumentation
  process. See `feature/search/impl/src/androidTest/AndroidManifest.xml`.
- **`PagingData.empty()` reports `refresh = Loading`**; empty-state tests must pass explicit
  terminal `LoadStates`.
- **Edge-to-edge is app-wide.** The Scaffold in `MoviesApp` uses zeroed `contentWindowInsets`, so
  each screen root applies its own `statusBarsPadding()`. A new screen without it draws under the
  status bar.
- **Networks that block TMDB** (DNS to `127.0.0.1` plus TLS reset on the SNI host) look exactly
  like an app bug. Check `dig api.themoviedb.org` first; `image.tmdb.org` is usually not blocked,
  so posters can load while the API cannot.

## Docs

- Design spec: `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`
- Implementation plans, spec divergences, and executed-fix log: `docs/superpowers/plans/`
- Ported from the reference app at `~/android/TheMovies`
