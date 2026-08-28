# Data Layer Decoupling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the two coupling points that make adding features 6-15 progressively more expensive: the seven-method `MoviesRepository` god interface (Part A) and the single `:core:data` module every feature depends on (Part B).

**Architecture:** Part A splits `MoviesRepository` into three consumer-shaped interfaces (`MovieListRepository`, `MovieDetailRepository`, `MovieSearchRepository`) implemented by the same `OfflineFirstMoviesRepository` class — three `@Binds` instead of one, and three small test fakes instead of one seven-method fake. Part B splits `:core:data` into `:core:data:movies`, `:core:data:auth`, `:core:data:watchlist`, plus a new `:core:connectivity` for the platform network monitor, so a feature compiles against only the domain it uses. Both parts are behavior-preserving refactors: the existing unit test suite is the regression check, and no test assertion changes meaning.

**Tech Stack:** Kotlin, Gradle with `build-logic` convention plugins, typesafe project accessors, Hilt, Room, Paging 3, JUnit4 + Turbine.

---

## Scope and sequencing

**Part A is ready to execute now.** It is ~10 files, three green commits, and it pays off the moment a fifth movies method is added.

**Part B is trigger-gated. Do not execute it yet.** `:core:data` is 990 lines of main source across 3 domains. Splitting a 990-line module into four is not yet worth the Gradle configuration cost. Execute Part B when *either* trigger fires:

- a fourth domain repository lands in `:core:data` (e.g. profile, recommendations, offline downloads), **or**
- a `./gradlew :feature:<x>:impl:compileDebugKotlin` on a warm daemon takes more than ~20s because it waits on `:core:data`.

Part B assumes Part A has landed — it moves the three interfaces Part A creates.

---

# PART A — Split `MoviesRepository` by consumer

## Current state

`core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MoviesRepository.kt` declares 7 methods. Four consumers each use a disjoint slice:

| Consumer | Methods actually used |
|---|---|
| `HomeViewModel` | `getMovies`, `refresh` |
| `SearchViewModel` | `searchMoviesPaged` |
| `DetailViewModel` | `refreshDetail` |
| `GetMovieDetailUseCase` | `getMovieDetail`, `getCast`, `getReviews` |

`TestMoviesRepository` (`core/testing`) must stub all 7 for every test, including `SearchViewModelTest`, which exercises exactly one.

## Target state

```
MovieListRepository    getMovies(category), refresh(category)
MovieDetailRepository  getMovieDetail(id), getCast(id), getReviews(id), refreshDetail(id)
MovieSearchRepository  searchMoviesPaged(query)
```

`OfflineFirstMoviesRepository` implements all three, unchanged apart from its supertype list. `MoviesRepository` is deleted.

## File Structure

- Create `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieListRepository.kt` — list reads + category refresh.
- Create `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieDetailRepository.kt` — detail/cast/reviews reads + detail refresh.
- Create `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieSearchRepository.kt` — network-backed paged search.
- Delete `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MoviesRepository.kt`.
- Modify `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/OfflineFirstMoviesRepository.kt` — supertype list only.
- Modify `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt` — one `@Binds` becomes three.
- Create `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieListRepository.kt`
- Create `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieDetailRepository.kt`
- Create `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieSearchRepository.kt`
- Delete `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMoviesRepository.kt`.
- Modify the four production consumers and the four test files that name them (exact paths in each task).

Task A1 introduces the interfaces while keeping `MoviesRepository` as their union, so A1 and A2 both build and both leave the suite green. A3 removes the union.

Every new `.kt` file fails `spotlessCheck` until the license header is inserted, so every task runs `./gradlew spotlessApply` before its verification step.

---

### Task A1: Introduce the three interfaces as supertypes of `MoviesRepository`

**Files:**
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieListRepository.kt`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieDetailRepository.kt`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MovieSearchRepository.kt`
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MoviesRepository.kt`
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt:35-38`

- [x] **Step 1: Create `MovieListRepository.kt`**

Write the file with this body (Spotless adds the license header in Step 5):

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import kotlinx.coroutines.flow.Flow

/**
 * Category listings. Reads come from local storage, so every screen works offline; [refresh] is
 * the only network access and no-ops inside the sync TTL.
 */
interface MovieListRepository {

    fun getMovies(category: MovieCategory): Flow<List<Movie>>

    /** Returns false when the network call failed; cached data is still readable. */
    suspend fun refresh(category: MovieCategory): Boolean
}
```

- [x] **Step 2: Create `MovieDetailRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * One movie's detail, cast and reviews. Reads come from local storage; [refreshDetail] is the
 * only network access and no-ops inside the sync TTL.
 */
interface MovieDetailRepository {

    fun getMovieDetail(movieId: Int): Flow<MovieDetail?>

    fun getCast(movieId: Int): Flow<List<Cast>>

    fun getReviews(movieId: Int): Flow<List<Review>>

    /** Returns false when the network call failed; cached data is still readable. */
    suspend fun refreshDetail(movieId: Int): Boolean
}
```

- [x] **Step 3: Create `MovieSearchRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.data.repository

import androidx.paging.PagingData
import com.practice.thenewmovies.core.model.Movie
import kotlinx.coroutines.flow.Flow

/** Network-backed and not persisted: page numbering belongs to the server. */
interface MovieSearchRepository {

    fun searchMoviesPaged(query: String): Flow<PagingData<Movie>>
}
```

- [x] **Step 4: Replace the body of `MoviesRepository.kt` with the union**

Keep the license header already in the file; replace everything from `package` down with:

```kotlin
package com.practice.thenewmovies.core.data.repository

/**
 * Transitional union of the three movie repositories, kept only so consumers can be narrowed one
 * at a time. Deleted in Task A3 — do not add methods here.
 */
interface MoviesRepository :
    MovieListRepository,
    MovieDetailRepository,
    MovieSearchRepository
```

`OfflineFirstMoviesRepository` needs no edit in this task: it still declares `: MoviesRepository`, which now transitively implements all three, and its seven `override` members satisfy them.

- [x] **Step 5: Add the three bindings to `DataModule.kt`**

Insert after the existing `bindsMoviesRepository` function (`core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt:35-38`), and add the three imports alongside the existing `MoviesRepository` import:

```kotlin
    @Binds
    internal abstract fun bindsMovieListRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieListRepository

    @Binds
    internal abstract fun bindsMovieDetailRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieDetailRepository

    @Binds
    internal abstract fun bindsMovieSearchRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieSearchRepository
```

Imports to add at the top of the file:

```kotlin
import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
import com.practice.thenewmovies.core.data.repository.MovieListRepository
import com.practice.thenewmovies.core.data.repository.MovieSearchRepository
```

- [x] **Step 6: Format and verify the whole graph builds**

Hilt binding errors surface only at `:app`, never at `:core:data`, so `:app:compileDebugKotlin` is the real check.

```bash
./gradlew spotlessApply
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. A `MissingBinding` or `DuplicateBindings` failure here means Step 5 is wrong — fix before continuing.

- [x] **Step 7: Run the full unit test suite**

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. No test file changed in this task, so any failure is a real regression.

- [x] **Step 8: Commit**

```bash
git add core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt
git commit -m "refactor(data): split MoviesRepository into three consumer interfaces"
```

---

### Task A2: Narrow the four production consumers

Each consumer swaps `MoviesRepository` for the one interface it uses. `TestMoviesRepository` still implements the union, so every test file compiles unchanged and the suite stays green.

**Files:**
- Modify: `core/domain/src/main/kotlin/com/practice/thenewmovies/core/domain/GetMovieDetailUseCase.kt:18,25`
- Modify: `feature/home/impl/src/main/kotlin/com/practice/thenewmovies/feature/home/impl/HomeViewModel.kt:20,39`
- Modify: `feature/search/impl/src/main/kotlin/com/practice/thenewmovies/feature/search/impl/SearchViewModel.kt:22,41`
- Modify: `feature/detail/impl/src/main/kotlin/com/practice/thenewmovies/feature/detail/impl/DetailViewModel.kt:20,45`

- [x] **Step 1: Narrow `GetMovieDetailUseCase`**

Change the import and the constructor parameter type. The parameter name stays `moviesRepository` so no call site moves.

```kotlin
import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
```

```kotlin
class GetMovieDetailUseCase @Inject constructor(
    private val moviesRepository: MovieDetailRepository,
    private val watchlistRepository: WatchlistRepository,
) {
```

- [x] **Step 2: Narrow `HomeViewModel`**

```kotlin
import com.practice.thenewmovies.core.data.repository.MovieListRepository
```

```kotlin
internal class HomeViewModel @Inject constructor(
    private val moviesRepository: MovieListRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
```

- [x] **Step 3: Narrow `SearchViewModel`**

```kotlin
import com.practice.thenewmovies.core.data.repository.MovieSearchRepository
```

```kotlin
internal class SearchViewModel @Inject constructor(
    moviesRepository: MovieSearchRepository,
) : ViewModel() {
```

- [x] **Step 4: Narrow `DetailViewModel`**

`DetailViewModel` reads detail data through `GetMovieDetailUseCase` and touches the repository only for `refreshDetail`.

```kotlin
import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
```

```kotlin
internal class DetailViewModel @AssistedInject constructor(
    @Assisted private val movieId: Int,
    getMovieDetail: GetMovieDetailUseCase,
    private val moviesRepository: MovieDetailRepository,
    private val watchlistRepository: WatchlistRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
```

- [x] **Step 5: Verify build and tests**

```bash
./gradlew spotlessApply
./gradlew :app:compileDebugKotlin testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`. No test file changed; `TestMoviesRepository` is still a subtype of all three narrowed parameter types.

- [x] **Step 6: Commit**

```bash
git add core/domain feature/home/impl feature/search/impl feature/detail/impl
git commit -m "refactor: inject the narrowed movie repositories into each consumer"
```

---

### Task A3: Split the test fake and delete the union interface

**Files:**
- Create: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieListRepository.kt`
- Create: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieDetailRepository.kt`
- Create: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMovieSearchRepository.kt`
- Delete: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMoviesRepository.kt`
- Delete: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MoviesRepository.kt`
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt` (drop `bindsMoviesRepository`)
- Modify: `feature/home/impl/src/test/kotlin/com/practice/thenewmovies/feature/home/impl/HomeViewModelTest.kt:22,36`
- Modify: `feature/search/impl/src/test/kotlin/com/practice/thenewmovies/feature/search/impl/SearchViewModelTest.kt:20,31`
- Modify: `feature/detail/impl/src/test/kotlin/com/practice/thenewmovies/feature/detail/impl/DetailViewModelTest.kt:23,39`
- Modify: `core/domain/src/test/kotlin/com/practice/thenewmovies/core/domain/GetMovieDetailUseCaseTest.kt:22,34`

Note: `TestMoviesRepository.emitSearchResults` has no caller anywhere in the repo — it is dropped rather than carried into `TestMovieSearchRepository`. Add it back the day a search test needs it.

- [x] **Step 1: Create `TestMovieListRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.repository.MovieListRepository
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestMovieListRepository : MovieListRepository {

    private val moviesByCategory =
        MovieCategory.entries.associateWith { MutableStateFlow(emptyList<Movie>()) }

    var refreshSucceeds: Boolean = true
    val refreshedCategories = mutableListOf<MovieCategory>()

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        moviesByCategory.getValue(category).asStateFlow()

    override suspend fun refresh(category: MovieCategory): Boolean {
        refreshedCategories += category
        return refreshSucceeds
    }

    fun emitMovies(category: MovieCategory, movies: List<Movie>) {
        moviesByCategory.getValue(category).value = movies
    }
}
```

- [x] **Step 2: Create `TestMovieDetailRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestMovieDetailRepository : MovieDetailRepository {

    private val detail = MutableStateFlow<MovieDetail?>(null)
    private val cast = MutableStateFlow(emptyList<Cast>())
    private val reviews = MutableStateFlow(emptyList<Review>())

    var refreshSucceeds: Boolean = true
    val refreshedDetailIds = mutableListOf<Int>()

    override fun getMovieDetail(movieId: Int): Flow<MovieDetail?> = detail.asStateFlow()

    override fun getCast(movieId: Int): Flow<List<Cast>> = cast.asStateFlow()

    override fun getReviews(movieId: Int): Flow<List<Review>> = reviews.asStateFlow()

    override suspend fun refreshDetail(movieId: Int): Boolean {
        refreshedDetailIds += movieId
        return refreshSucceeds
    }

    fun emitDetail(movieDetail: MovieDetail?) {
        detail.value = movieDetail
    }

    fun emitCast(value: List<Cast>) {
        cast.value = value
    }

    fun emitReviews(value: List<Review>) {
        reviews.value = value
    }
}
```

- [x] **Step 3: Create `TestMovieSearchRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import androidx.paging.PagingData
import com.practice.thenewmovies.core.data.repository.MovieSearchRepository
import com.practice.thenewmovies.core.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TestMovieSearchRepository : MovieSearchRepository {

    override fun searchMoviesPaged(query: String): Flow<PagingData<Movie>> =
        flowOf(PagingData.empty())
}
```

- [x] **Step 4: Repoint the four test files**

`feature/home/impl/.../HomeViewModelTest.kt` — change the import and the field. All existing assertions (`emitMovies`, `refreshedCategories`, `refreshSucceeds`) exist on the new fake unchanged.

```kotlin
import com.practice.thenewmovies.core.testing.repository.TestMovieListRepository
```
```kotlin
    private val moviesRepository = TestMovieListRepository()
```

`feature/search/impl/.../SearchViewModelTest.kt`:

```kotlin
import com.practice.thenewmovies.core.testing.repository.TestMovieSearchRepository
```
```kotlin
    private val moviesRepository = TestMovieSearchRepository()
```

`feature/detail/impl/.../DetailViewModelTest.kt` — the fake is passed both to `DetailViewModel` and to `GetMovieDetailUseCase`, and both now take `MovieDetailRepository`, so one field still serves:

```kotlin
import com.practice.thenewmovies.core.testing.repository.TestMovieDetailRepository
```
```kotlin
    private val moviesRepository = TestMovieDetailRepository()
```

`core/domain/.../GetMovieDetailUseCaseTest.kt`:

```kotlin
import com.practice.thenewmovies.core.testing.repository.TestMovieDetailRepository
```
```kotlin
    private val moviesRepository = TestMovieDetailRepository()
```

- [x] **Step 5: Delete the union interface, its fake, and its binding**

```bash
git rm core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/MoviesRepository.kt
git rm core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestMoviesRepository.kt
```

In `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt`, delete the `bindsMoviesRepository` function and its `import com.practice.thenewmovies.core.data.repository.MoviesRepository` line.

In `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/OfflineFirstMoviesRepository.kt:55`, change the supertype:

```kotlin
) : MovieListRepository, MovieDetailRepository, MovieSearchRepository {
```

- [x] **Step 6: Confirm nothing still names the deleted types**

```bash
grep -rn "MoviesRepository" app core feature --include="*.kt" | grep -v "/build/" | grep -v OfflineFirstMoviesRepository
```

Expected: no output. (`OfflineFirstMoviesRepository` and `OfflineFirstMoviesRepositoryTest` keep their names — the impl class is not being renamed.)

- [x] **Step 7: Full build**

```bash
./gradlew spotlessApply
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. This runs assemble + all unit tests + `spotlessCheck` + lint.

- [x] **Step 8: Commit**

```bash
git add -A core/data core/testing core/domain feature
git commit -m "refactor(testing): split TestMoviesRepository and drop the union interface"
```

---

## Part A completion check

- [x] `./gradlew build` passes.
- [x] `grep -rn "interface MoviesRepository" core` returns nothing.
- [x] `SearchViewModelTest` compiles against a fake with exactly one method.

---

# PART B — Split `:core:data` by domain

**Do not start until a Part B trigger fires (see "Scope and sequencing").**

## Current state

Every feature declares `implementation(projects.core.data)`. One module holds three unrelated domains, so a change to auth recompiles the watchlist feature, and `feature/watchlist/impl` compiles against `MovieListRepository` and `AuthRepository` it never names.

## Target module graph

```
:core:connectivity        NetworkMonitor, ConnectivityManagerNetworkMonitor, ConnectivityModule
:core:data:movies         MovieList/Detail/SearchRepository, OfflineFirstMoviesRepository,
                          MoviePagingSource, NetworkEntity, NetworkModel, Clock, MoviesDataModule
:core:data:auth           AuthRepository, SupabaseAuthRepository, AuthErrorMapping,
                          SessionStateMapping, AuthDataModule
:core:data:watchlist      WatchlistRepository, DefaultWatchlistRepository,
                          WatchlistRemoteDataSource, WatchlistRow, WatchlistDataModule
```

`:core:data` keeps no sources and is dropped from `settings.gradle.kts` — Gradle creates it implicitly as the container of the three child projects.

Dependency edges:

| Module | Depends on |
|---|---|
| `:core:connectivity` | (nothing but AndroidX) |
| `:core:data:movies` | `:core:model` (api), `:core:common`, `:core:database`, `:core:network` |
| `:core:data:auth` | `:core:model` (api), `:core:supabase` |
| `:core:data:watchlist` | `:core:model` (api), `:core:database`, `:core:supabase`, `:core:data:auth` |

`DefaultWatchlistRepository` injects `AuthRepository` to scope rows by user id, so `:core:data:watchlist` → `:core:data:auth` is a real edge. It stays `implementation`: `AuthRepository` does not appear in `WatchlistRepository`'s signatures.

Consumer edges after the split:

| Consumer | Replaces `projects.core.data` with |
|---|---|
| `:core:domain` | `api(projects.core.data.movies)`, `api(projects.core.data.watchlist)` |
| `:core:testing` | `api` on all three data modules + `:core:connectivity` |
| `:app` | `projects.core.data.auth`, `projects.core.data.watchlist` |
| `feature:home:impl` | `projects.core.data.movies`, `projects.core.connectivity` |
| `feature:search:impl` | `projects.core.data.movies` |
| `feature:detail:impl` | `projects.core.data.movies`, `projects.core.data.watchlist`, `projects.core.connectivity` |
| `feature:watchlist:impl` | `projects.core.data.watchlist` |
| `feature:auth:impl` | `projects.core.data.auth` |

### Accepted, not fixed

- **`:core:testing` still fans in to all three.** It is a `testImplementation` dependency, so it only serializes *test* compilation, not the main source sets where the build time is. Split it only if test compile becomes the bottleneck.
- **`:core:database` stays one module with one `MoviesDatabase`.** One Room database per app is correct; the shared cost is version-bump merge conflicts, not compile coupling.

### Package renames

Each moved file gets a package matching its new module, so there are no split packages across modules:

| Old package | New package |
|---|---|
| `...core.data.util` (NetworkMonitor, ConnectivityManagerNetworkMonitor) | `...core.connectivity` |
| `...core.data.repository` (movies) | `...core.data.movies` |
| `...core.data.model`, `...core.data.paging`, `...core.data.util.Clock` | `...core.data.movies` |
| `...core.data.repository` (auth) | `...core.data.auth` |
| `...core.data.repository` + `...core.data.remote` (watchlist) | `...core.data.watchlist` |

The renames are the bulk of the diff and are mechanical — each task gives the exact `sed` command.

## File Structure

New build files, one per module, each ~15 lines: apply a convention plugin, set `namespace`, declare dependencies. Nothing else, per `CLAUDE.md`.

Tasks B1–B4 each move one domain out and repoint its consumers in the same commit, so every commit builds. `:core:data` shrinks with each one and disappears in B4.

---

### Task B1: Extract `:core:connectivity`

`NetworkMonitor` is a platform utility injected straight into `HomeViewModel` and `DetailViewModel` — it is not part of any data domain, and pulling it out first means later tasks do not have to think about it.

**Files:**
- Create: `core/connectivity/build.gradle.kts`
- Create: `core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/NetworkMonitor.kt` (moved)
- Create: `core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/ConnectivityManagerNetworkMonitor.kt` (moved)
- Create: `core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/di/ConnectivityModule.kt`
- Move: `core/data/src/main/AndroidManifest.xml` → `core/connectivity/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts`
- Modify: `core/data/build.gradle.kts`, `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt`
- Modify: `core/testing/build.gradle.kts`, `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestNetworkMonitor.kt`
- Modify: `feature/home/impl/build.gradle.kts`, `feature/detail/impl/build.gradle.kts`

- [ ] **Step 1: Move the files and rewrite their packages**

```bash
mkdir -p core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/di
git mv core/data/src/main/kotlin/com/practice/thenewmovies/core/data/util/NetworkMonitor.kt \
       core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/NetworkMonitor.kt
git mv core/data/src/main/kotlin/com/practice/thenewmovies/core/data/util/ConnectivityManagerNetworkMonitor.kt \
       core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/ConnectivityManagerNetworkMonitor.kt
sed -i '' 's/^package com\.practice\.thenewmovies\.core\.data\.util$/package com.practice.thenewmovies.core.connectivity/' \
       core/connectivity/src/main/kotlin/com/practice/thenewmovies/core/connectivity/*.kt
git mv core/data/src/main/AndroidManifest.xml core/connectivity/src/main/AndroidManifest.xml
```

`core/data/src/main/AndroidManifest.xml` exists only to declare `ACCESS_NETWORK_STATE` for
`ConnectivityManagerNetworkMonitor` — its own comment says so — so the whole file moves with the
monitor. `:core:data` is left with no manifest, which is fine: AGP generates one for a library
module that does not declare anything. `:app` declares the same permission independently, so a
missed move would not show up at runtime — only the library's self-containment would quietly rot.

- [ ] **Step 2: Create `core/connectivity/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.connectivity"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 3: Create `ConnectivityModule.kt` with the binding moved out of `DataModule`**

```kotlin
package com.practice.thenewmovies.core.connectivity.di

import com.practice.thenewmovies.core.connectivity.ConnectivityManagerNetworkMonitor
import com.practice.thenewmovies.core.connectivity.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConnectivityModule {

    @Binds
    internal abstract fun bindsNetworkMonitor(
        monitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor
}
```

Then delete `bindsNetworkMonitor` and its two `NetworkMonitor`/`ConnectivityManagerNetworkMonitor` imports from `core/data/.../di/DataModule.kt`.

`ConnectivityManagerNetworkMonitor` is `internal`; it must stay `internal` and now lives in the same module as its `@Binds`, so no visibility change is needed.

- [ ] **Step 4: Register the module in `settings.gradle.kts`**

Add in alphabetical position, after `include(":core:common")`:

```kotlin
include(":core:connectivity")
```

- [ ] **Step 5: Repoint consumers**

In `core/testing/build.gradle.kts` add `api(projects.core.connectivity)`.

In `feature/home/impl/build.gradle.kts` and `feature/detail/impl/build.gradle.kts` add:

```kotlin
    implementation(projects.core.connectivity)
```

Rewrite the import in every file that names `NetworkMonitor`:

```bash
grep -rl "core\.data\.util\.NetworkMonitor" app core feature --include="*.kt" \
  | grep -v "/build/" \
  | xargs sed -i '' 's/core\.data\.util\.NetworkMonitor/core.connectivity.NetworkMonitor/'
```

- [ ] **Step 6: Verify**

```bash
./gradlew spotlessApply
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. A `MissingBinding ... NetworkMonitor` at `:app` means Step 3 dropped the binding without adding the new module to the graph — check that `:core:connectivity` is reachable from `:app` (it is, transitively through the two features).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: extract :core:connectivity from :core:data"
```

---

### Task B2: Extract `:core:data:auth`

**Files:**
- Create: `core/data/auth/build.gradle.kts`
- Move: `AuthRepository.kt`, `SupabaseAuthRepository.kt`, `AuthErrorMapping.kt`, `SessionStateMapping.kt` and their two tests
- Create: `core/data/auth/src/main/kotlin/com/practice/thenewmovies/core/data/auth/di/AuthDataModule.kt`
- Modify: `settings.gradle.kts`, `core/data/build.gradle.kts`, `core/data/.../di/DataModule.kt`
- Modify: `core/testing/build.gradle.kts`, `app/build.gradle.kts`, `feature/auth/impl/build.gradle.kts`

- [ ] **Step 1: Move the sources and tests**

```bash
mkdir -p core/data/auth/src/main/kotlin/com/practice/thenewmovies/core/data/auth/di
mkdir -p core/data/auth/src/test/kotlin/com/practice/thenewmovies/core/data/auth
D=core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository
T=core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository
N=core/data/auth/src/main/kotlin/com/practice/thenewmovies/core/data/auth
NT=core/data/auth/src/test/kotlin/com/practice/thenewmovies/core/data/auth
git mv $D/AuthRepository.kt $D/SupabaseAuthRepository.kt $D/AuthErrorMapping.kt $D/SessionStateMapping.kt $N/
git mv $T/AuthErrorMappingTest.kt $T/SessionStateMappingTest.kt $NT/
sed -i '' 's/^package com\.practice\.thenewmovies\.core\.data\.repository$/package com.practice.thenewmovies.core.data.auth/' $N/*.kt $NT/*.kt
```

- [ ] **Step 2: Create `core/data/auth/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.data.auth"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.supabase)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
```

`testImplementation(projects.core.testing)` alongside `:core:testing`'s `api` on this module is not a cycle and is exactly what `core/data/build.gradle.kts` does today: only `compileTestKotlin` reaches `:core:testing`, and `:core:testing`'s main source set reaches back to this module's main jar — the task graph stays acyclic.

- [ ] **Step 3: Create `AuthDataModule.kt`**

```kotlin
package com.practice.thenewmovies.core.data.auth.di

import com.practice.thenewmovies.core.data.auth.AuthRepository
import com.practice.thenewmovies.core.data.auth.SupabaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {

    @Binds
    internal abstract fun bindsAuthRepository(
        repository: SupabaseAuthRepository,
    ): AuthRepository
}
```

Delete `bindsAuthRepository` and its two imports from `core/data/.../di/DataModule.kt`.

- [ ] **Step 4: Register and repoint**

`settings.gradle.kts` — add after `include(":core:data")`:

```kotlin
include(":core:data:auth")
```

`core/data/build.gradle.kts` — add `implementation(projects.core.data.auth)` (still needed: `DefaultWatchlistRepository` injects `AuthRepository`).

`core/testing/build.gradle.kts` — add `api(projects.core.data.auth)`.

`app/build.gradle.kts` and `feature/auth/impl/build.gradle.kts` — add `implementation(projects.core.data.auth)`.

Rewrite imports across the repo:

```bash
grep -rlE "core\.data\.repository\.(AuthRepository|AuthError|SessionState)" app core feature --include="*.kt" \
  | grep -v "/build/" \
  | xargs sed -i '' 's/core\.data\.repository\.\(AuthRepository\|AuthErrorMapping\|SessionStateMapping\)/core.data.auth.\1/g'
```

Then check for any straggler by hand:

```bash
grep -rn "core\.data\.repository\." app core feature --include="*.kt" | grep -v "/build/" | grep -iE "auth|session"
```

Expected: no output.

- [ ] **Step 5: Verify and commit**

```bash
./gradlew spotlessApply
./gradlew build
git add -A
git commit -m "refactor: extract :core:data:auth from :core:data"
```

Expected: `BUILD SUCCESSFUL`. `SupabaseAuthRepository` and the mapping functions are `internal`; they moved together with `AuthDataModule` and the tests that exercise them, so visibility is unchanged. If a mapping function turns out to be referenced from `DefaultWatchlistRepository`, promote it to `public` in this task rather than duplicating it.

---

### Task B3: Extract `:core:data:watchlist`

**Files:**
- Create: `core/data/watchlist/build.gradle.kts`
- Move: `WatchlistRepository.kt`, `DefaultWatchlistRepository.kt`, `remote/WatchlistRemoteDataSource.kt`, `remote/WatchlistRow.kt` and the tests `DefaultWatchlistRepositoryTest.kt`, `WatchlistRowMappingTest.kt`
- Create: `core/data/watchlist/src/main/kotlin/com/practice/thenewmovies/core/data/watchlist/di/WatchlistDataModule.kt`
- Modify: `settings.gradle.kts`, `core/data/build.gradle.kts`, `core/data/.../di/DataModule.kt`
- Modify: `core/testing/build.gradle.kts`, `core/domain/build.gradle.kts`, `app/build.gradle.kts`, `feature/watchlist/impl/build.gradle.kts`, `feature/detail/impl/build.gradle.kts`

- [ ] **Step 1: Move the sources and tests**

```bash
mkdir -p core/data/watchlist/src/main/kotlin/com/practice/thenewmovies/core/data/watchlist/di
mkdir -p core/data/watchlist/src/test/kotlin/com/practice/thenewmovies/core/data/watchlist
D=core/data/src/main/kotlin/com/practice/thenewmovies/core/data
T=core/data/src/test/kotlin/com/practice/thenewmovies/core/data
N=core/data/watchlist/src/main/kotlin/com/practice/thenewmovies/core/data/watchlist
NT=core/data/watchlist/src/test/kotlin/com/practice/thenewmovies/core/data/watchlist
git mv $D/repository/WatchlistRepository.kt $D/repository/DefaultWatchlistRepository.kt $N/
git mv $D/remote/WatchlistRemoteDataSource.kt $D/remote/WatchlistRow.kt $N/
git mv $T/repository/DefaultWatchlistRepositoryTest.kt $T/remote/WatchlistRowMappingTest.kt $NT/
sed -i '' -E 's/^package com\.practice\.thenewmovies\.core\.data\.(repository|remote)$/package com.practice.thenewmovies.core.data.watchlist/' $N/*.kt $NT/*.kt
```

`DefaultWatchlistRepository` currently imports `...core.data.remote.WatchlistRemoteDataSource`, `...asEntity` and `...asRow`; all three now share its package, so delete those three import lines. Same for the two moved tests.

- [ ] **Step 2: Create `core/data/watchlist/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies.core.data.watchlist"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.data.auth)
    implementation(projects.core.database)
    implementation(projects.core.supabase)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk)
}
```

`kotlin.serialization` is required: `WatchlistRow` is a `@Serializable` Postgrest row.

`DefaultWatchlistRepositoryTest` constructs `TestAuthRepository` from `:core:testing`, so the `testImplementation(projects.core.testing)` above is load-bearing, not boilerplate. See the note in Task B2 Step 2 for why it is not a cycle.

- [ ] **Step 3: Create `WatchlistDataModule.kt`**

```kotlin
package com.practice.thenewmovies.core.data.watchlist.di

import com.practice.thenewmovies.core.data.watchlist.DefaultWatchlistRepository
import com.practice.thenewmovies.core.data.watchlist.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WatchlistDataModule {

    @Binds
    internal abstract fun bindsWatchlistRepository(
        repository: DefaultWatchlistRepository,
    ): WatchlistRepository
}
```

Delete `bindsWatchlistRepository` and its two imports from `core/data/.../di/DataModule.kt`.

- [ ] **Step 4: Register and repoint**

`settings.gradle.kts`:

```kotlin
include(":core:data:watchlist")
```

`core/data/build.gradle.kts` — remove `implementation(projects.core.data.auth)` and `implementation(projects.core.supabase)`; neither is used by what remains (movies only).

Add `projects.core.data.watchlist` to: `core/testing/build.gradle.kts` (`api`), `core/domain/build.gradle.kts` (`api`), `app/build.gradle.kts`, `feature/watchlist/impl/build.gradle.kts`, `feature/detail/impl/build.gradle.kts` (all `implementation`).

```bash
grep -rl "core\.data\.repository\.WatchlistRepository" app core feature --include="*.kt" \
  | grep -v "/build/" \
  | xargs sed -i '' 's/core\.data\.repository\.WatchlistRepository/core.data.watchlist.WatchlistRepository/'
```

- [ ] **Step 5: Verify and commit**

```bash
./gradlew spotlessApply
./gradlew build
git add -A
git commit -m "refactor: extract :core:data:watchlist from :core:data"
```

---

### Task B4: Rename the remainder to `:core:data:movies` and retire `:core:data`

What is left in `:core:data` after B1–B3 is exactly the movies domain: the three interfaces from Part A, `OfflineFirstMoviesRepository`, `MoviePagingSource`, `NetworkEntity`, `NetworkModel`, `Clock`, `DataModule`, `ClockModule`, and three tests.

**Files:**
- Create: `core/data/movies/build.gradle.kts`
- Move: every remaining file under `core/data/src` into `core/data/movies/src` with package `...core.data.movies`
- Delete: `core/data/build.gradle.kts`, `core/data/src`
- Modify: `settings.gradle.kts`, and every consumer build file
- Modify: `README.md`, `CLAUDE.md`

- [ ] **Step 1: Move everything remaining**

```bash
mkdir -p core/data/movies/src/main/kotlin/com/practice/thenewmovies/core/data/movies/di
mkdir -p core/data/movies/src/test/kotlin/com/practice/thenewmovies/core/data/movies
git mv core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/*.kt \
       core/data/src/main/kotlin/com/practice/thenewmovies/core/data/model/*.kt \
       core/data/src/main/kotlin/com/practice/thenewmovies/core/data/paging/*.kt \
       core/data/src/main/kotlin/com/practice/thenewmovies/core/data/util/Clock.kt \
       core/data/movies/src/main/kotlin/com/practice/thenewmovies/core/data/movies/
git mv core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt \
       core/data/movies/src/main/kotlin/com/practice/thenewmovies/core/data/movies/di/MoviesDataModule.kt
git mv core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository/*.kt \
       core/data/src/test/kotlin/com/practice/thenewmovies/core/data/model/*.kt \
       core/data/movies/src/test/kotlin/com/practice/thenewmovies/core/data/movies/
sed -i '' -E 's/^package com\.practice\.thenewmovies\.core\.data\.(repository|model|paging|util|di)$/package com.practice.thenewmovies.core.data.movies/' \
       core/data/movies/src/main/kotlin/com/practice/thenewmovies/core/data/movies/*.kt \
       core/data/movies/src/test/kotlin/com/practice/thenewmovies/core/data/movies/*.kt
sed -i '' 's/^package com\.practice\.thenewmovies\.core\.data\.di$/package com.practice.thenewmovies.core.data.movies.di/' \
       core/data/movies/src/main/kotlin/com/practice/thenewmovies/core/data/movies/di/MoviesDataModule.kt
git rm -r core/data/src
```

Rename the class inside `MoviesDataModule.kt` from `DataModule` to `MoviesDataModule` (Kotlin requires no filename/class match, but the mismatch is confusing), and delete its now-same-package imports for `MovieListRepository`, `MovieDetailRepository`, `MovieSearchRepository`, `OfflineFirstMoviesRepository`. `ClockModule` stays in the same file.

- [ ] **Step 2: Create `core/data/movies/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.data.movies"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)

    api(libs.paging.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit)

    testImplementation(projects.core.testing)
    testImplementation(libs.mockk)
}
```

This is `core/data/build.gradle.kts` as it stands today, minus `kotlin.serialization` and `:core:supabase`. `kotlin.serialization` is dropped because the movies DTOs use Moshi codegen in `:core:network`; it was only there for `WatchlistRow`, which left in B3.

- [ ] **Step 3: Delete `:core:data` and register the new module**

```bash
git rm core/data/build.gradle.kts
```

In `settings.gradle.kts`, delete `include(":core:data")` and add `include(":core:data:movies")`. Gradle creates the `:core:data` container project implicitly from its children — an explicit include with no build file would fail configuration.

- [ ] **Step 4: Repoint every remaining consumer**

```bash
grep -rn "projects\.core\.data\b" --include="*.kts" . | grep -v "/build/"
```

Replace each hit per the "Consumer edges" table above. Concretely: `core/domain`, `core/testing` → `projects.core.data.movies`; `feature/home/impl`, `feature/search/impl`, `feature/detail/impl` → `projects.core.data.movies`; `app` drops `projects.core.data` entirely (it uses only auth and watchlist, both added in B2/B3).

Rewrite the remaining imports:

```bash
grep -rl "core\.data\.\(repository\|model\|paging\|util\)\." app core feature --include="*.kt" \
  | grep -v "/build/" \
  | xargs sed -i '' -E 's/core\.data\.(repository|model|paging|util)\./core.data.movies./g'
```

- [ ] **Step 5: Confirm no reference to the old module or packages survives**

```bash
grep -rn "projects\.core\.data\b" --include="*.kts" . | grep -v "/build/"
grep -rn "core\.data\.\(repository\|remote\|paging\|util\)\." app core feature --include="*.kt" | grep -v "/build/"
```

Expected: no output from either.

- [ ] **Step 6: Full build, including instrumented-test compilation**

```bash
./gradlew spotlessApply
./gradlew build
./gradlew compileDebugAndroidTestSources
```

Expected: `BUILD SUCCESSFUL` from all three. The third catches androidTest sources, which `build` does not compile.

- [ ] **Step 7: Update the docs**

In `README.md`, replace `core:data` in the module map with the four modules and their one-line responsibilities from the "Target module graph" table above.

In `CLAUDE.md`, under **Architecture**, replace the `:core:data` mentions in the "Room is the single source of truth" and "Repository interfaces are public" bullets, and add one bullet:

```markdown
- **`core:data` is split by domain** (`movies`, `auth`, `watchlist`) and holds no sources itself.
  A feature depends on the domains it names and no others; `core:connectivity` holds
  `NetworkMonitor`. Adding a repository means picking a domain module, not growing a shared one.
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: rename the remainder of :core:data to :core:data:movies"
```

---

## Part B completion check

- [ ] `./gradlew build` and `./gradlew compileDebugAndroidTestSources` both pass.
- [ ] `grep -rn "projects.core.data\b" --include="*.kts" .` returns nothing.
- [ ] `feature/watchlist/impl/build.gradle.kts` names `projects.core.data.watchlist` and no other data module.
- [ ] Touching `core/data/auth` and running `./gradlew :feature:watchlist:impl:compileDebugKotlin` reports the task `UP-TO-DATE` — this is the whole point of Part B, so verify it explicitly.
- [ ] `README.md` and `CLAUDE.md` describe the new module map.

---

## Known risks

- **Hilt failures surface only at `:app`.** Every task compiles `:app`, never just the module it edited. A `MissingBinding` means a `@Binds` moved out of `DataModule` without its new `@Module` reaching the graph.
- **A green `assembleRelease` proves nothing** for the Moshi codegen path (`CLAUDE.md`). Neither part touches DTOs or R8 rules, but if `:core:network` is edited in passing, install and run a release build before believing it.
- **`sed -i ''` is BSD/macOS syntax.** On Linux use `sed -i` with no argument.
- **`internal` visibility follows the module, not the package.** Anything `internal` that ends up in a different module from its only caller becomes a compile error naming the exact symbol — promote it to `public` in the same task, do not duplicate it.
