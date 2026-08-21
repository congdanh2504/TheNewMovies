# TheNewMovies Slices 5-7 — Search, Watchlist and Wrap-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish feature parity with the reference app — paged search and the watchlist — then document the project and run the full verification pass.

**Architecture:** Same feature shape as slices 3 and 4: `api` module holds the key, `impl` module holds ViewModel, sealed state, screen pair and entry function, `:app` swaps out the last two placeholders. Search is the only screen backed by Paging rather than Room.

**Tech Stack:** Paging 3 (`paging-compose`), Compose Material 3, Hilt, Turbine.

**Depends on:** Slices 3-4 complete (Home and Detail working against real data).

**Spec:** `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`

---

## File Structure

| File | Responsibility |
| --- | --- |
| `feature/search/impl/.../SearchViewModel.kt` | Debounced query, paged results |
| `feature/search/impl/.../SearchScreen.kt` | Search bar, result list, load states |
| `feature/search/impl/.../navigation/SearchEntry.kt` | `searchEntry(navigator)` |
| `feature/watchlist/impl/.../WatchlistUiState.kt` | Sealed state |
| `feature/watchlist/impl/.../WatchlistViewModel.kt` | Reads the watchlist |
| `feature/watchlist/impl/.../WatchlistScreen.kt` | List, empty state |
| `feature/watchlist/impl/.../navigation/WatchlistEntry.kt` | `watchlistEntry(navigator)` |
| `README.md` | Architecture, module map, commands |

---

# Slice 5 — Search

### Task 1: Search ViewModel

**Files:**
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml` (no change needed if `paging-compose` is already present — it is)
- Create: `feature/search/impl/build.gradle.kts`
- Create: `feature/search/impl/src/main/kotlin/com/practice/thenewmovies/feature/search/impl/SearchViewModel.kt`
- Test: `feature/search/impl/src/test/kotlin/com/practice/thenewmovies/feature/search/impl/SearchViewModelTest.kt`

- [ ] **Step 1: Add the module to `settings.gradle.kts`**

```kotlin
include(":feature:search:impl")
```

- [ ] **Step 2: Write `feature/search/impl/build.gradle.kts`**

```kotlin
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
}
```

- [ ] **Step 3: Write the failing test**

Paging content is verified by hand on device; the unit test covers the query pipeline, which is where the logic is.

```kotlin
package com.practice.thenewmovies.feature.search.impl

import app.cash.turbine.test
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMoviesRepository()

    private fun viewModel() = SearchViewModel(moviesRepository)

    @Test
    fun `starts with an empty query`() = runTest {
        assertEquals("", viewModel().query.value)
    }

    @Test
    fun `query updates immediately`() = runTest {
        val viewModel = viewModel()

        viewModel.query.test {
            assertEquals("", awaitItem())
            viewModel.onQueryChange("dune")
            assertEquals("dune", awaitItem())
        }
    }

    @Test
    fun `clearing the query resets it`() = runTest {
        val viewModel = viewModel()
        viewModel.onQueryChange("dune")

        viewModel.onQueryChange("")

        assertEquals("", viewModel.query.value)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :feature:search:impl:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: SearchViewModel`.

- [ ] **Step 5: Write `SearchViewModel.kt`**

The debounce and `flatMapLatest` chain is the same as the reference implementation, with the use case replaced by a direct repository call.

```kotlin
package com.practice.thenewmovies.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 500L

@OptIn(FlowPreview::class)
@HiltViewModel
internal class SearchViewModel @Inject constructor(
    moviesRepository: MoviesRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: Flow<PagingData<Movie>> = _query
        .debounce(QUERY_DEBOUNCE_MS)
        .filter { it.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { query -> moviesRepository.searchMoviesPaged(query) }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _query.value = query
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :feature:search:impl:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add feature/search/impl settings.gradle.kts
git commit -m "feat(search): add SearchViewModel"
```

---

### Task 2: Search screen

**Files:**
- Create: `feature/search/impl/src/main/kotlin/com/practice/thenewmovies/feature/search/impl/SearchScreen.kt`
- Create: `.../navigation/SearchEntry.kt`
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`, `app/build.gradle.kts`

Ported from `TheMovies/feature/search/.../SearchScreen.kt`. Two changes: the empty state uses the shared `MoviesEmptyState` with a Material icon instead of a per-module drawable, and the result row uses the shared `MovieRow` instead of a search-local `SearchMovie` widget.

Because the screen renders `LazyPagingItems`, the stateless overload takes the paging items rather than a plain state object — a pure preview of paging content is not worth faking, so the previews cover the empty state.

- [ ] **Step 1: Write `SearchScreen.kt`**

```kotlin
package com.practice.thenewmovies.feature.search.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.practice.thenewmovies.core.designsystem.component.MoviesSearchBar
import com.practice.thenewmovies.core.designsystem.component.MoviesTopAppBar
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.ui.component.MetaLabel
import com.practice.thenewmovies.core.ui.component.MovieRow
import com.practice.thenewmovies.core.ui.component.MoviesEmptyState
import com.practice.thenewmovies.core.ui.component.RatingChip

@Composable
internal fun SearchScreen(
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    SearchScreen(
        query = query,
        searchResults = searchResults,
        onQueryChange = viewModel::onQueryChange,
        onBackClick = onBackClick,
        onMovieClick = onMovieClick,
        modifier = modifier,
    )
}

@Composable
internal fun SearchScreen(
    query: String,
    searchResults: LazyPagingItems<Movie>,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MoviesTopAppBar(
            title = "Search",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        MoviesSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val refreshState = searchResults.loadState.refresh

        when {
            query.isBlank() -> NoResultsState()

            refreshState is LoadState.Loading && searchResults.itemCount == 0 -> LoadingState()

            refreshState is LoadState.Error -> ErrorState(
                message = refreshState.error.message ?: "Something went wrong",
                onRetry = searchResults::retry,
            )

            searchResults.itemCount == 0 -> NoResultsState()

            else -> SearchResultsList(
                searchResults = searchResults,
                onMovieClick = onMovieClick,
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    searchResults: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(
            count = searchResults.itemCount,
            key = searchResults.itemKey { it.id },
        ) { index ->
            val movie = searchResults[index] ?: return@items
            MovieRow(
                posterPath = movie.posterPath,
                title = movie.title,
                onClick = { onMovieClick(movie.id) },
            ) {
                RatingChip(rating = movie.voteAverage.toFloat())
                Spacer(modifier = Modifier.height(4.dp))
                MetaLabel(
                    icon = Icons.Outlined.CalendarMonth,
                    label = movie.releaseDate.take(4).ifEmpty { "N/A" },
                )
            }
        }

        item {
            when (val appendState = searchResults.loadState.append) {
                is LoadState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is LoadState.Error -> ErrorState(
                    message = appendState.error.message ?: "Load failed",
                    onRetry = searchResults::retry,
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    MoviesEmptyState(
        icon = Icons.Outlined.SearchOff,
        title = "We are sorry, we can not find the movie :(",
        subtitle = "Find your movie by Type title, categories, years, etc",
        modifier = modifier.padding(horizontal = 40.dp),
    )
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun NoResultsStatePreview() {
    MoviesTheme { NoResultsState() }
}
```

`items(count = ..., key = ...)` needs `androidx.compose.foundation.lazy.items` — if the compiler reports an overload-resolution error on that call, add the explicit import:

```kotlin
import androidx.compose.foundation.lazy.items
```

- [ ] **Step 2: Write `navigation/SearchEntry.kt`**

```kotlin
package com.practice.thenewmovies.feature.search.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.api.navigateToDetail
import com.practice.thenewmovies.feature.search.api.SearchNavKey
import com.practice.thenewmovies.feature.search.impl.SearchScreen

fun EntryProviderScope<NavKey>.searchEntry(navigator: Navigator) {
    entry<SearchNavKey> {
        SearchScreen(
            onBackClick = { navigator.goBack() },
            onMovieClick = navigator::navigateToDetail,
        )
    }
}
```

- [ ] **Step 3: Wire the entry into `:app`**

Add to `app/build.gradle.kts` dependencies:

```kotlin
    implementation(projects.feature.search.impl)
```

In `MoviesApp.kt`, replace

```kotlin
                entry<SearchNavKey> { Placeholder("Search") }
```

with

```kotlin
                searchEntry(navigator)
```

and add the import:

```kotlin
import com.practice.thenewmovies.feature.search.impl.navigation.searchEntry
```

- [ ] **Step 4: Install and check by hand**

Run: `./gradlew :app:installDebug`
Then verify:
1. Opening Search shows the empty state, not a spinner.
2. Typing "dune" shows results after roughly half a second; typing more characters cancels the previous request rather than stacking results.
3. Scrolling to the bottom loads a second page.
4. Tapping a result opens Detail; back returns to the results with the query and scroll position intact.
5. In airplane mode the error state with a Retry button appears; leaving airplane mode and tapping Retry loads results.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add feature/search app
git commit -m "feat(search): add search screen and register its entry"
```

---

# Slice 6 — Watchlist

### Task 3: Watchlist ViewModel

**Files:**
- Modify: `settings.gradle.kts`
- Create: `feature/watchlist/impl/build.gradle.kts`
- Create: `feature/watchlist/impl/src/main/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistUiState.kt`
- Create: `.../WatchlistViewModel.kt`
- Test: `feature/watchlist/impl/src/test/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistViewModelTest.kt`

- [ ] **Step 1: Add the module to `settings.gradle.kts`**

```kotlin
include(":feature:watchlist:impl")
```

- [ ] **Step 2: Write `feature/watchlist/impl/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.watchlist.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.watchlist.api)
    implementation(projects.feature.detail.api)
}
```

- [ ] **Step 3: Write `WatchlistUiState.kt`**

An explicit `Empty` state, because "no movies saved" is a first-class screen here rather than an accident of an empty list.

```kotlin
package com.practice.thenewmovies.feature.watchlist.impl

import com.practice.thenewmovies.core.model.WatchlistMovie

internal sealed interface WatchlistUiState {
    data object Loading : WatchlistUiState
    data object Empty : WatchlistUiState
    data class Success(val movies: List<WatchlistMovie>) : WatchlistUiState
}
```

- [ ] **Step 4: Write the failing test**

```kotlin
package com.practice.thenewmovies.feature.watchlist.impl

import app.cash.turbine.test
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testWatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val watchlistRepository = TestWatchlistRepository()

    private fun viewModel() = WatchlistViewModel(watchlistRepository)

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(WatchlistUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `reports empty when nothing is saved`() = runTest {
        viewModel().uiState.test {
            assertEquals(WatchlistUiState.Loading, awaitItem())
            assertEquals(WatchlistUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `emits saved movies`() = runTest {
        watchlistRepository.emit(listOf(testWatchlistMovie))

        viewModel().uiState.test {
            assertEquals(
                WatchlistUiState.Success(listOf(testWatchlistMovie)),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `returns to empty when the last movie is removed`() = runTest {
        watchlistRepository.emit(listOf(testWatchlistMovie))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(
                WatchlistUiState.Success(listOf(testWatchlistMovie)),
                expectMostRecentItem(),
            )

            watchlistRepository.emit(emptyList())

            assertEquals(WatchlistUiState.Empty, awaitItem())
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./gradlew :feature:watchlist:impl:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: WatchlistViewModel`.

- [ ] **Step 6: Write `WatchlistViewModel.kt`**

```kotlin
package com.practice.thenewmovies.feature.watchlist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class WatchlistViewModel @Inject constructor(
    watchlistRepository: WatchlistRepository,
) : ViewModel() {

    val uiState: StateFlow<WatchlistUiState> = watchlistRepository.getWatchlist()
        .map { movies ->
            if (movies.isEmpty()) WatchlistUiState.Empty else WatchlistUiState.Success(movies)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WatchlistUiState.Loading,
        )
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :feature:watchlist:impl:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add feature/watchlist/impl settings.gradle.kts
git commit -m "feat(watchlist): add WatchlistViewModel"
```

---

### Task 4: Watchlist screen

**Files:**
- Create: `feature/watchlist/impl/src/main/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistScreen.kt`
- Create: `.../navigation/WatchlistEntry.kt`
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`, `app/build.gradle.kts`

Ported from `TheMovies/feature/watchlist/.../WatchListScreen.kt`, with the row and empty state now coming from `core:ui`.

- [ ] **Step 1: Write `WatchlistScreen.kt`**

```kotlin
package com.practice.thenewmovies.feature.watchlist.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesTopAppBar
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.WatchlistMovie
import com.practice.thenewmovies.core.ui.component.MetaLabel
import com.practice.thenewmovies.core.ui.component.MovieRow
import com.practice.thenewmovies.core.ui.component.MoviesEmptyState
import com.practice.thenewmovies.core.ui.component.RatingChip

@Composable
internal fun WatchlistScreen(
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WatchlistScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onMovieClick = onMovieClick,
        modifier = modifier,
    )
}

@Composable
internal fun WatchlistScreen(
    uiState: WatchlistUiState,
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MoviesTopAppBar(
            title = "Watch list",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            WatchlistUiState.Loading -> LoadingState()

            WatchlistUiState.Empty -> EmptyState()

            is WatchlistUiState.Success -> WatchlistList(
                movies = uiState.movies,
                onMovieClick = onMovieClick,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    MoviesEmptyState(
        icon = Icons.Outlined.Bookmark,
        title = "There is no movie yet!",
        subtitle = "Find your movie by Type title, categories, years, etc",
        modifier = modifier.padding(horizontal = 40.dp),
    )
}

@Composable
private fun WatchlistList(
    movies: List<WatchlistMovie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = movies, key = { it.id }) { movie ->
            MovieRow(
                posterPath = movie.posterPath,
                title = movie.title,
                onClick = { onMovieClick(movie.id) },
            ) {
                MetaLabel(
                    icon = Icons.Outlined.CalendarMonth,
                    label = movie.releaseDate.take(4),
                )
                Spacer(modifier = Modifier.height(4.dp))
                MetaLabel(
                    icon = Icons.Outlined.Schedule,
                    label = "${movie.runtime} minutes",
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetaLabel(
                        icon = Icons.Outlined.ConfirmationNumber,
                        label = movie.genre,
                    )
                    RatingChip(rating = movie.userRating ?: movie.voteAverage.toFloat())
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WatchlistScreenPreview() {
    MoviesTheme {
        WatchlistScreen(
            uiState = WatchlistUiState.Success(
                movies = listOf(
                    WatchlistMovie(
                        id = 1,
                        title = "Dune",
                        posterPath = null,
                        backdropPath = null,
                        releaseDate = "2021-10-22",
                        voteAverage = 7.8,
                        runtime = 155,
                        genre = "Science Fiction",
                        userRating = 4.5f,
                    ),
                ),
            ),
            onBackClick = {},
            onMovieClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WatchlistScreenEmptyPreview() {
    MoviesTheme {
        WatchlistScreen(
            uiState = WatchlistUiState.Empty,
            onBackClick = {},
            onMovieClick = {},
        )
    }
}
```

- [ ] **Step 2: Write `navigation/WatchlistEntry.kt`**

```kotlin
package com.practice.thenewmovies.feature.watchlist.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.api.navigateToDetail
import com.practice.thenewmovies.feature.watchlist.api.WatchlistNavKey
import com.practice.thenewmovies.feature.watchlist.impl.WatchlistScreen

fun EntryProviderScope<NavKey>.watchlistEntry(navigator: Navigator) {
    entry<WatchlistNavKey> {
        WatchlistScreen(
            onBackClick = { navigator.goBack() },
            onMovieClick = navigator::navigateToDetail,
        )
    }
}
```

- [ ] **Step 3: Wire the entry into `:app` and delete the placeholder**

Add to `app/build.gradle.kts` dependencies:

```kotlin
    implementation(projects.feature.watchlist.impl)
```

In `MoviesApp.kt`, replace

```kotlin
                entry<WatchlistNavKey> { Placeholder("Watch List") }
```

with

```kotlin
                watchlistEntry(navigator)
```

add the import:

```kotlin
import com.practice.thenewmovies.feature.watchlist.impl.navigation.watchlistEntry
```

and delete the now-unused `Placeholder` composable and the `// Replaced one at a time by slices 3-6.` comment. The `entryProvider` block should now read:

```kotlin
            entryProvider = entryProvider {
                homeEntry(navigator)
                searchEntry(navigator)
                watchlistEntry(navigator)
                detailEntry(navigator)
            },
```

- [ ] **Step 4: Install and check by hand**

Run: `./gradlew :app:installDebug`
Then verify:
1. Watch List starts on the empty state.
2. Bookmarking a movie from Detail makes it appear here with year, runtime, genre and rating.
3. A rating given in Detail shows on the row instead of the TMDB average.
4. Tapping a row opens Detail for that movie.
5. Removing the bookmark from Detail returns the tab to the empty state.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add feature/watchlist app
git commit -m "feat(watchlist): add watchlist screen and register its entry"
```

---

# Slice 7 — Documentation and final verification

### Task 5: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write `README.md`**

```markdown
# TheNewMovies

An Android movie browser powered by the [TMDB API](https://www.themoviedb.org/documentation/api),
structured after [Now in Android](https://github.com/android/nowinandroid): convention plugins,
`api`/`impl` feature modules, an offline-first data layer, and Navigation 3.

## Features

- **Home** — featured row, four category tabs, poster grid; remembers the selected tab
- **Search** — debounced, paged TMDB search
- **Detail** — backdrop, poster, About/Reviews/Cast tabs, watchlist toggle, rating sheet
- **Watchlist** — Room-persisted saved movies with user ratings and an empty state

Every screen reads from Room, so the app works offline with whatever it last synced.

## Architecture

```
app ──> feature:*:impl ──> feature:*:api ──> core:navigation
             │
             └──> core:{data, domain, ui, designsystem, model, common}

core:data ──> core:{network, database, datastore, model, common}
```

- **`core:model`** — pure-Kotlin domain models
- **`core:common`** — injected dispatcher qualifiers
- **`core:designsystem`** — theme, typography, themed primitives; knows no app model
- **`core:navigation`** — `NavigationState` (a back stack per tab) and `Navigator`
- **`core:network`** — TMDB DTOs and the Retrofit data source
- **`core:database`** — Room entities, DAOs, and entity-to-model mapping
- **`core:datastore`** — the selected home tab
- **`core:data`** — repository interfaces, `internal` offline-first implementations, mapping
- **`core:domain`** — `GetMovieDetailUseCase`, the one place several repositories are combined
- **`core:ui`** — composites shared by two or more features
- **`core:testing`** — fakes and the main-dispatcher rule
- **`feature:<name>:api`** — one `NavKey` plus a navigate extension
- **`feature:<name>:impl`** — ViewModel, screen, entry function

A feature reaches another feature only through its `api` module, so features compile in parallel
and `:app` is the only module that knows the whole graph.

### Data flow

```
NetworkMovie ──asEntity()──> MovieEntity ──asExternalModel()──> Movie
 (core:network)              (core:database)                   (core:model)
```

Reads come from Room only. The network is touched by `refresh(category)` and
`refreshDetail(id)`, which no-op inside a 24-hour TTL, plus search paging (not persisted).

## Tech stack

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation 3 (`NavDisplay`, `entryProvider`, `NavKey`) |
| DI | Hilt |
| Network | Retrofit + OkHttp + Moshi |
| Persistence | Room, Preferences DataStore |
| Paging | Paging 3 |
| Images | Coil |
| Async | Coroutines + Flow |
| Build | Gradle convention plugins in an included build |
| Format | Spotless + ktlint |

## Getting started

Requires JDK 17+ and Android Studio with AGP 8.10 support.

1. Get a TMDB **v4 read access token** from https://www.themoviedb.org/settings/api
2. Add it to `local.properties`:

```properties
TMDB_API_KEY=your_token_here
```

The build fails with a readable message if the key is missing.

## Commands

```bash
./gradlew assembleDebug                      # build
./gradlew installDebug                       # build and install
./gradlew spotlessApply                      # format
./gradlew testDebugUnitTest                  # unit tests
./gradlew connectedDebugAndroidTest          # DAO instrumented tests
./gradlew build                              # everything, including spotlessCheck
```

## Docs

- Design spec: `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`
- Implementation plans: `docs/superpowers/plans/`
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README"
```

---

### Task 6: Full verification pass

- [ ] **Step 1: Clean build with tests and formatting**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`. This runs `spotlessCheck` and every unit test — roughly 48 tests across nine modules.

- [ ] **Step 2: Instrumented tests**

Run: `./gradlew :core:database:connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`, 9 tests passed.

- [ ] **Step 3: Confirm no feature depends on another feature's impl**

```bash
for f in home search detail watchlist; do
  echo "== $f =="
  ./gradlew ":feature:$f:impl:dependencies" --configuration debugCompileClasspath \
    | grep "feature:" | grep "impl" | grep -v "feature:$f:impl" || echo "clean"
done
```

Expected: `clean` under every feature. Any other line means a feature is compiling against another feature's implementation, which breaks the whole point of the split.

- [ ] **Step 4: Confirm `:app` holds no screens**

```bash
find app/src/main/kotlin -name "*.kt" | xargs grep -l "@Composable" 
```

Expected: only `ui/MoviesApp.kt`. Screens belong in feature modules; `:app` composes them.

- [ ] **Step 5: Release build**

Run: `./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL`. R8 is enabled for release, so this catches missing keep rules. If it fails on a Moshi or Retrofit reflection error, add the corresponding rules to `app/proguard-rules.pro` — Moshi's `KotlinJsonAdapterFactory` uses reflection over the DTO classes:

```proguard
-keep,allowobfuscation,allowshrinking class kotlin.Metadata
-keepclassmembers class com.practice.thenewmovies.core.network.model.** { *; }
```

- [ ] **Step 6: Final manual pass**

Install the debug build and walk the whole app once: Home tabs, search, open a detail from each of the three tabs, bookmark, rate, remove, rotate the device on each screen, and background the app then reopen it. Nothing should reset except in-flight network calls.

- [ ] **Step 7: Commit any fixes**

```bash
./gradlew spotlessApply
git add -A
git commit -m "chore: final verification fixes"
```

---

## Done when

- All four features are reachable and behave like the reference app.
- `./gradlew clean build` and `./gradlew assembleRelease` both pass.
- No feature `impl` module appears on another feature's compile classpath.
- `:app` contains exactly one composable file.
- README documents the architecture, the key setup and the commands.
