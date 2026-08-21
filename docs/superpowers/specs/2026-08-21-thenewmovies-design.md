# TheNewMovies — Design

Date: 2026-08-21

## Purpose

Rebuild the TMDB movie browser currently living at `~/android/TheMovies` as a new project
structured according to `~/android/nia-blueprint.md` (the distilled Now in Android structure).
Feature parity with the existing app: Home, Search, Detail, Watchlist.

The existing app works but carries decisions the new structure replaces: monolithic feature
modules with no `api`/`impl` split, navigation mechanics living in `:app`, thirteen
pass-through use cases, cache-aside reads that let network shape leak into the read path, and a
single `core:ui` module that mixes themed primitives with model-rendering composites.

## Decisions

| Question | Decision |
| --- | --- |
| Scope | Full parity — all four features |
| Data layer | Offline-first, Room as single source of truth |
| Flavors | None; TMDB key from `local.properties` into `BuildConfig` |
| Navigation | Navigation 3 per blueprint (`NavDisplay`, `entryProvider`, `NavKey`) |
| Domain layer | Thin — pass-through use cases dropped |
| UI modules | `core:designsystem` / `core:ui` split; screen layouts ported visually as-is |
| Tooling | Spotless + `core:testing` fakes + unit tests |
| Build order | Vertical slices — app runs after every slice |

Excluded deliberately: `sync/` module and WorkManager sync, `demo`/`prod` flavors, `lint`
module, Jacoco, Roborazzi screenshot tests, dependency-guard, Gradle-managed devices,
macrobenchmark and baseline profiles. Each is CI machinery that adds setup cost without
changing app behavior; any of them can be added later without restructuring.

## Naming and identity

- Base package: `com.practice.thenewmovies`. Namespace matches module path exactly —
  `com.practice.thenewmovies.core.data`, `com.practice.thenewmovies.feature.home.impl`,
  `com.practice.thenewmovies.feature.home.api`.
- `applicationId = "com.practice.thenewmovies"`, so it installs alongside the existing app.
- Design system prefix `Movies`: `MoviesTheme`, `MoviesTopAppBar`, `MoviesIcons`.
- Convention plugin prefix `themovies`: `themovies.android.library`,
  `themovies.android.feature.impl`.
- Source roots are `src/main/kotlin`, `src/test/kotlin`, `src/androidTest/kotlin` — not `java`,
  which is what the old repo uses.
- Resource prefix derived from module path, as in the blueprint, so resources in `:core:ui` are
  named `core_ui_*`.

## Module inventory

```
build-logic/convention   convention plugins (included build)
core/model      (JVM)    Movie MovieDetail Genre Cast Review WatchlistMovie
                         MovieCategory MoviesPage
core/common     (JVM)    @Dispatcher qualifier, MoviesDispatchers, Result, asResult()
core/designsystem        MoviesTheme Color Type Font MoviesTopAppBar MoviesSearchBar
                         MoviesBottomBar MoviesIcons — knows no app models
core/navigation          NavigationState, Navigator
core/network             MoviesNetworkDataSource + internal RetrofitMoviesNetwork,
                         Network* DTOs, asEntity()
core/database            MoviesDatabase, entities, DAOs, asExternalModel()
core/datastore           UserPreferencesRepository (Preferences DataStore)
core/data                MoviesRepository + internal OfflineFirstMoviesRepository
                         WatchlistRepository + internal DefaultWatchlistRepository
                         NetworkMonitor + ConnectivityManagerNetworkMonitor
                         MoviePagingSource, DataModule (@Binds)
core/domain              GetMovieDetailUseCase
core/ui                  MovieCard MoviePosterCard MovieFeed OfflineBanner @DevicePreviews
core/testing             MainDispatcherRule, Test* fakes, static test data
feature/home/{api,impl}
feature/search/{api,impl}
feature/detail/{api,impl}
feature/watchlist/{api,impl}
app                      MainActivity, MoviesApplication, MoviesApp, MoviesAppState,
                         NavDisplay, TopLevelNavItem, entryProvider {}
```

### Module rules

| Type | Plugin | May depend on |
| --- | --- | --- |
| `app` | `themovies.android.application` | every feature `api` + `impl`, any `core` |
| `feature:x:api` | `themovies.android.feature.api` | `core:navigation` (as `api`). Never another feature |
| `feature:x:impl` | `themovies.android.feature.impl` | other features' `api` only, plus `core:*` |
| `core:*` | `themovies.android.library` / `themovies.jvm.library` | other `core` modules only |

A class used by one feature stays in that feature. Used by two or more, it moves to `core`.

## Dependency direction

```
app ──> feature:*:impl ──> feature:*:api ──> core:navigation
             │
             └──> core:{data, domain, ui, designsystem, model, common}

core:data ──> core:{network, database, datastore, model, common}
core:ui   ──> core:{designsystem, model}
```

`:feature:home:impl` calls `navigator.navigateToDetail(movieId)` from
`:feature:detail:api` and never compiles against `:feature:detail:impl`.

## Convention plugins

`build-logic` is an included build (`includeBuild("build-logic")` in `settings.gradle.kts`),
re-importing `gradle/libs.versions.toml` so plugin code and app code share one version source.
`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` is on, so module files use
`projects.core.data`. Root `build.gradle.kts` declares every plugin with `apply false`.

| Plugin id | Does |
| --- | --- |
| `themovies.root` | Root Spotless |
| `themovies.android.application` | `com.android.application`, `targetSdk`, Spotless, lint |
| `themovies.android.application.compose` | Compose for the app module |
| `themovies.android.library` | Kotlin config, resource prefix, Spotless, `kotlin.test` + JUnit4 |
| `themovies.android.library.compose` | Compose for library modules |
| `themovies.android.feature.api` | `android.library` + kotlinx.serialization + `api(projects.core.navigation)` |
| `themovies.android.feature.impl` | `android.library` + hilt + `core:ui`, `core:designsystem`, lifecycle-viewmodel-compose, navigation3-runtime |
| `themovies.hilt` | KSP + Hilt compiler; `hilt.core` for JVM modules, `hilt.android` + Hilt Gradle plugin for Android |
| `themovies.android.room` | Room plugin + KSP, `generateKotlin=true`, `schemaDirectory("$projectDir/schemas")` |
| `themovies.jvm.library` | Pure-Kotlin modules (`core:model`, `core:common`) |

Shared helpers: `KotlinAndroid.kt`, `AndroidCompose.kt`, `Spotless.kt`, `ProjectExtensions.kt`
(the `Project.libs` accessor). `build-logic` compiles against JDK 17 with
`validatePlugins { enableStricterValidation = true; failOnWarning = true }`.

Build config: `compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`, Java and Kotlin target 17 —
matching the existing repo rather than the blueprint's 11, since `minSdk = 24` plus JDK 17
removes the need for core library desugaring. `allWarningsAsErrors` reads the
`warningsAsErrors` Gradle property.
`gradle.properties`: configuration cache and build cache on, `org.gradle.parallel=true`,
4 GB heap for Gradle and the Kotlin daemon.

The version catalog is seeded from the existing repo's `gradle/libs.versions.toml`, which
already carries navigation3, Room, Hilt, Retrofit/Moshi, Coil, Paging, DataStore, Turbine and
MockK at current versions.

## Data layer

Three model tiers, never leaking into each other, mapped by extension functions living beside
the type being converted:

```
NetworkMovie ──asEntity()──> MovieEntity ──asExternalModel()──> Movie
 (core:network)               (core:database)                   (core:model)
```

Reads come exclusively from Room and are always `Flow`, so the app works offline by
construction. Writes come from explicit refresh calls that fetch TMDB and upsert.

```kotlin
interface MoviesRepository {
    fun getMovies(category: MovieCategory): Flow<List<Movie>>
    fun getMovieDetail(id: Int): Flow<MovieDetail?>
    fun getCast(id: Int): Flow<List<Cast>>
    fun getReviews(id: Int): Flow<List<Review>>
    fun searchMoviesPaged(query: String): Flow<PagingData<Movie>>
    suspend fun refresh(category: MovieCategory): Boolean
    suspend fun refreshDetail(id: Int): Boolean
}

interface WatchlistRepository {
    fun getWatchlist(): Flow<List<WatchlistMovie>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    fun getRating(movieId: Int): Flow<Float?>
    suspend fun addToWatchlist(movie: WatchlistMovie)
    suspend fun removeFromWatchlist(movieId: Int)
    suspend fun setRating(movieId: Int, rating: Float)
}
```

`getMovies(category)` replaces the four `getPopularMovies()` / `getTopRatedMovies()` /
`getUpcomingMovies()` / `getNowPlayingMovies()` methods — category is already a column on the
entity, so it belongs as a parameter.

Implementations are `internal`, named after their strategy (`OfflineFirstMoviesRepository`,
`DefaultWatchlistRepository`, `RetrofitMoviesNetwork`), and bound through one `@Binds` module
per concern in `core/data/src/main/kotlin/.../data/di/`.

Staleness replaces the removed `SyncWorker`: `MovieEntity` carries `syncedAt`, and `refresh`
is a no-op when the newest row for the category is younger than a 24-hour TTL. ViewModels call
`refresh` on start, so opening a screen brings data up to date without a background worker. The
TTL is a constant in `core:data`, not a build config value.

Search is the one exception to Room-as-source-of-truth: `searchMoviesPaged` returns
`Flow<PagingData<Movie>>` fed by a network-backed `MoviePagingSource`. Paged search results are
transient and page numbering is server-owned; persisting them buys nothing.

Dispatchers are injected through the qualifier enum from `core:common`, never referenced
directly:

```kotlin
@Qualifier @Retention(RUNTIME) annotation class Dispatcher(val dispatcher: MoviesDispatchers)
enum class MoviesDispatchers { Default, IO }
```

The TMDB key is read from `local.properties` in the application module's build script and
exposed as a `BuildConfig` field, consumed by an OkHttp interceptor in `core:network`. A missing
key fails the build with a readable message rather than producing an app that 401s at runtime.

## Domain layer

One use case: `GetMovieDetailUseCase`. It composes `getMovieDetail`, `getCast`, `getReviews`,
`isInWatchlist` and `getRating` into the detail screen's state. Everything else is a single
repository call, so ViewModels call repositories directly.

## Navigation

`core:navigation` owns the mechanics and knows about no screens:

```kotlin
class Navigator(val state: NavigationState) {
    fun navigate(key: NavKey)
    fun goBack()
}
```

`NavigationState` holds a top-level stack of sub-stacks, persisted through `SavedStateHandle`.
This fixes a real bug in the current app: `navigateToTab` clears the whole back stack, so
switching tabs loses the previous tab's history. With sub-stacks, tapping the current tab clears
that tab's sub-stack, and switching tabs preserves each tab's own history.

Each feature `api` module is one or two files:

```kotlin
// feature/detail/api
@Serializable data class DetailNavKey(val movieId: Int) : NavKey
fun Navigator.navigateToDetail(movieId: Int) = navigate(DetailNavKey(movieId))
```

Each feature `impl` module contributes one entry function:

```kotlin
// feature/detail/impl
fun EntryProviderScope<NavKey>.detailEntry(navigator: Navigator) {
    entry<DetailNavKey> { key ->
        DetailScreen(movieId = key.movieId, onBackClick = navigator::goBack)
    }
}
```

`:app` is the only place that composes them, inside one `entryProvider {}` passed to
`NavDisplay`. `TopLevelNavItem` lists Home, Search and Watchlist with their `NavKey` and icon.

## Features

Every `impl` module follows the same shape:

- `XViewModel` — `@HiltViewModel`, constructor injection, state as
  `StateFlow` built with `.map(...).onStart { emit(Loading) }.stateIn(viewModelScope,
  WhileSubscribed(5_000), Loading)`, events exposed as plain functions.
- `XUiState` — sealed interface with `data object Loading` / `data class Success` /
  `data class Error`.
- `XScreen` — two overloads with the same name in one file: a stateful one that wires
  `hiltViewModel()` and `collectAsStateWithLifecycle()`, and a stateless one that is pure and
  previewable. Sub-composables are `private` in the same file. `modifier: Modifier = Modifier`
  is the last non-trailing parameter. Callbacks are named `onXClick`.
- Previews sit next to the composable they exercise, using `@DevicePreviews` from `core:ui`.
- Everything is `internal` except the entry function.

Layouts are ported from the existing app so the screens look the same; only the wiring is
rewritten. `DetailMovieScreen` is 594 lines today and gets decomposed as it moves: the
backdrop/poster header, the About/Reviews/Cast tab bodies, and the rating modal each become
`private` composables in the same file, and anything the watchlist screen also renders moves to
`core:ui`.

`core:designsystem` versus `core:ui` is a hard boundary. Designsystem holds themed primitives
and depends on no app model. `core:ui` holds composites that render models (`MovieCard`,
`MovieFeed`, `OfflineBanner`) and therefore depends on `core:model`. A feature that needs a
themed button reaches for designsystem; a feature that needs to render a `Movie` reaches for
`core:ui`.

The home screen's selected tab is remembered through `core:datastore`
(`UserPreferencesRepository`, Preferences DataStore — not Proto, since one integer does not
justify codegen).

## Testing

`core:testing` is a real module, not `src/test` fixtures, so any module can consume it: a
`MainDispatcherRule`, `TestDispatchersModule`, `TestMoviesRepository`,
`TestWatchlistRepository`, `TestNetworkMonitor`, and static test data. The library convention
plugin wires `kotlin.test` + JUnit4 into every module.

Coverage:

- Four ViewModels, using Turbine on their state flows — loading, success, empty and error.
- `OfflineFirstMoviesRepository` — reads come from Room, refresh upserts, refresh is skipped
  inside the TTL and performed outside it.
- Mappers — `asEntity()` and `asExternalModel()` round trips, including nullable poster and
  backdrop paths.
- DAO instrumented tests for `MovieDao` and `WatchlistDao`, ported from the existing repo.
- `Navigator` / `NavigationState` — tab switching preserves per-tab history, re-tapping the
  current tab clears its sub-stack, `goBack` at the start key does not crash.

## Code style

Spotless + ktlint (`android = true`) over `src/**/*.kt` and `*.kts`, with a license header per
language from `spotless/copyright.{kt,kts,xml}`, wired into the library and application
convention plugins from the first commit. `.editorconfig` allows trailing commas and exempts
`@Composable` and `@Test` from function-naming rules. Trailing commas everywhere; named
arguments for anything non-obvious. `internal` is the default visibility for implementations.
KDoc on public interfaces; no comments restating code. `./gradlew spotlessApply` fixes
formatting.

## Build order — vertical slices

Each slice ends with a compiling, runnable app.

**Slice 0 — foundation.** `settings.gradle.kts` with the included build, repository filters,
typesafe project accessors and the JDK 17 check. `gradle/libs.versions.toml` seeded from the
existing repo. `build-logic/convention` with `android.library`, `android.application`,
`library.compose`, `application.compose`, `hilt`, `jvm.library`, `root`, plus `KotlinAndroid.kt`
and `ProjectExtensions.kt`. Root `build.gradle.kts` with every plugin `apply false`.
`spotless/copyright.*` and `.editorconfig`. Then `core:model` and `core:common`.
Verified by `./gradlew :core:model:build :core:common:build spotlessCheck`.

**Slice 1 — app shell.** `core:designsystem` (theme, typography, fonts, top app bar, bottom
bar, icons), `core:navigation` (`NavigationState`, `Navigator`), and `:app` with
`MoviesApplication`, `MainActivity`, `MoviesApp`, `MoviesAppState`, `TopLevelNavItem`, and a
`NavDisplay` over an empty `entryProvider {}`. Add the `feature.api` and `feature.impl`
convention plugins here, since slice 2 onward needs them. Verified by installing and seeing a
themed scaffold with a working bottom bar, plus `Navigator` unit tests.

**Slice 2 — data.** `core:network` (Retrofit, Moshi, DTOs, the key interceptor, `asEntity`),
`core:database` (Room database, entities, DAOs, `asExternalModel`), `core:datastore`,
`core:data` (both repositories, `NetworkMonitor`, `MoviePagingSource`, `@Binds` modules), and
`core:testing`. Verified by repository and mapper unit tests plus DAO instrumented tests — no
UI yet.

**Slice 3 — home.** `core:ui` (`MovieCard`, `MoviePosterCard`, `MovieFeed`, `OfflineBanner`,
`@DevicePreviews`), then `feature/home/api` and `feature/home/impl`, registered in `:app`.
First slice where the app shows real TMDB data.

**Slice 4 — detail.** `core:domain` with `GetMovieDetailUseCase`, then `feature/detail/api`
and `feature/detail/impl`. `:feature:home:impl` gains a dependency on `:feature:detail:api`
only — the first real exercise of the `api`/`impl` split.

**Slice 5 — search.** `feature/search/api` and `feature/search/impl` over the paged repository
method, navigating to detail through `:feature:detail:api`.

**Slice 6 — watchlist.** `feature/watchlist/api` and `feature/watchlist/impl` over
`WatchlistRepository`, including the rating modal and empty state.

**Slice 7 — README and module graphs.** Port the README with the new architecture diagram and
the build commands.

## Commands

```bash
./gradlew assembleDebug                      # build
./gradlew spotlessApply                      # format
./gradlew testDebugUnitTest                  # unit tests
./gradlew connectedDebugAndroidTest          # DAO instrumented tests
```

## Risks

- Navigation 3 APIs are young. The existing app already ships `NavDisplay` with the versions in
  the catalog, so the surface is known to work; `NavigationState` sub-stacks are the new part
  and are covered by unit tests in slice 1.
- `core:data` is written once in slice 2 but only exercised from slice 3 onward, so its first
  real consumer may force small interface changes. Acceptable — the interface is `internal`-
  backed and only two ViewModels bind to it before slice 5.
- TMDB rate limits during development can make refresh look broken. The TTL keeps repeat runs
  off the network.
