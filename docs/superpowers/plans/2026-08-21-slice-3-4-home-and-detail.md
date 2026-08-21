# TheNewMovies Slices 3-4 — Home and Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Real TMDB data on screen. Slice 3 adds `core:ui` and the Home feature; slice 4 adds `core:domain` and the Detail feature, which is also the first real exercise of the `api`/`impl` split.

**Architecture:** Each feature `impl` module holds a ViewModel, a sealed UI state, a stateful screen that wires Hilt, a stateless screen that is pure, and an entry function. `:app` swaps one placeholder entry for the real entry per feature. Home navigates to Detail through `:feature:detail:api` only.

**Tech Stack:** Compose Material 3, Coil, Hilt (assisted injection for Detail), Turbine for ViewModel tests.

**Depends on:** Slice 2 complete (`./gradlew build` green, DAO tests passing).

**Spec:** `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`

**Layout provenance:** every screen is a port of the reference implementation at
`/Users/danhtruong/android/TheMovies/feature/*`. The layouts below are the ported versions with
the wiring rewritten, the dead code dropped (`layeredShadow`, the commented-out preview, the
unused `CategoryRow`), and hard-coded colours replaced by theme tokens.

---

## File Structure

| File | Responsibility |
| --- | --- |
| `core/ui/.../component/MoviePoster.kt` | Rounded async image with a placeholder background |
| `core/ui/.../component/RatingChip.kt` | Star + one-decimal rating |
| `core/ui/.../component/MetaLabel.kt` | Small icon + grey label |
| `core/ui/.../component/MovieRow.kt` | Poster + title + caller-supplied meta lines |
| `core/ui/.../component/OfflineBanner.kt` | "showing cached data" strip |
| `core/ui/.../component/MoviesEmptyState.kt` | Icon + title + subtitle |
| `core/ui/.../DevicePreviews.kt` | Multi-device preview annotation |
| `feature/home/impl/.../HomeUiState.kt` | Sealed state |
| `feature/home/impl/.../HomeViewModel.kt` | Combines four category flows, triggers refresh |
| `feature/home/impl/.../HomeScreen.kt` | Screen + private sub-composables + previews |
| `feature/home/impl/.../navigation/HomeEntry.kt` | `homeEntry(navigator)` |
| `core/domain/.../GetMovieDetailUseCase.kt` | Detail + cast + reviews + watchlist + rating |
| `feature/detail/impl/.../DetailUiState.kt` | Sealed state |
| `feature/detail/impl/.../DetailViewModel.kt` | Assisted-injected `movieId`, watchlist actions |
| `feature/detail/impl/.../DetailScreen.kt` | Screen + tabs + rating sheet + previews |
| `feature/detail/impl/.../navigation/DetailEntry.kt` | `detailEntry(navigator)` |

---

# Slice 3 — `core:ui` and Home

### Task 1: Settings and `core:ui`

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/ui/build.gradle.kts`
- Create: `core/ui/src/main/kotlin/com/practice/thenewmovies/core/ui/component/MoviePoster.kt`
- Create: `.../component/RatingChip.kt`, `.../component/MetaLabel.kt`, `.../component/MovieRow.kt`, `.../component/OfflineBanner.kt`, `.../component/MoviesEmptyState.kt`
- Create: `core/ui/src/main/kotlin/com/practice/thenewmovies/core/ui/DevicePreviews.kt`

Every component here has at least two consumers across features, which is the rule for living in `core` rather than in a feature. Components take primitives rather than models, so `core:ui` needs no data dependency at all — stricter than the spec requires, and it keeps the module previewable in isolation.

- [ ] **Step 1: Add the modules to `settings.gradle.kts`**

```kotlin
include(":core:ui")
include(":feature:home:impl")
```

- [ ] **Step 2: Write `core/ui/build.gradle.kts`**

```kotlin
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
}
```

- [ ] **Step 3: Write `component/MoviePoster.kt`**

Every screen shows posters, and every one of them needs the same grey placeholder while Coil loads.

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val PlaceholderGrey = Color(0xFF3A3F47)

@Composable
fun MoviePoster(
    posterPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
) {
    AsyncImage(
        model = posterPath,
        contentDescription = contentDescription,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(PlaceholderGrey),
        contentScale = ContentScale.Crop,
    )
}
```

- [ ] **Step 4: Write `component/RatingChip.kt`**

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

private val AccentOrange = Color(0xFFFF8700)
private val ChipBackground = Color(0xFF252836)

@Composable
fun RatingChip(rating: Float, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ChipBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = AccentOrange,
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "%.1f".format(rating),
            color = AccentOrange,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun RatingChipPreview() {
    MoviesTheme { RatingChip(rating = 7.85f) }
}
```

- [ ] **Step 5: Write `component/MetaLabel.kt`**

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val MetaGrey = Color(0xFF92929D)

@Composable
fun MetaLabel(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MetaGrey,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = MetaGrey,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
```

- [ ] **Step 6: Write `component/MovieRow.kt`**

Search and Watchlist render the same row and differ only in the meta lines beneath the title, so the meta block is a slot.

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MovieRow(
    posterPath: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoviePoster(
            posterPath = posterPath,
            contentDescription = title,
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(2f / 3f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            meta()
        }
    }
}
```

- [ ] **Step 7: Write `component/OfflineBanner.kt`**

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error)
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "You're offline — showing cached data",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
private fun OfflineBannerPreview() {
    MoviesTheme { OfflineBanner() }
}
```

- [ ] **Step 8: Write `component/MoviesEmptyState.kt`**

```kotlin
package com.practice.thenewmovies.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

private val IconGrey = Color(0xFF3A3F47)
private val SubtitleGrey = Color(0xFF92929D)

@Composable
fun MoviesEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IconGrey,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            color = Color(0xFFEBEBEF),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = SubtitleGrey,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun MoviesEmptyStatePreview() {
    MoviesTheme {
        MoviesEmptyState(
            icon = Icons.Outlined.Bookmark,
            title = "There is no movie yet!",
            subtitle = "Find your movie by Type title, categories, years, etc",
        )
    }
}
```

- [ ] **Step 9: Write `DevicePreviews.kt`**

```kotlin
package com.practice.thenewmovies.core.ui

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "phone", device = "spec:width=411dp,height=891dp")
@Preview(name = "foldable", device = "spec:width=673dp,height=841dp")
@Preview(name = "tablet", device = "spec:width=1280dp,height=800dp")
annotation class DevicePreviews
```

- [ ] **Step 10: Build and commit**

Run: `./gradlew :core:ui:assembleDebug`
Expected: `BUILD SUCCESSFUL`

```bash
./gradlew spotlessApply
git add core/ui settings.gradle.kts
git commit -m "feat(ui): add shared composites"
```

---

### Task 2: Home ViewModel

**Files:**
- Create: `feature/home/impl/build.gradle.kts`
- Create: `feature/home/impl/src/main/kotlin/com/practice/thenewmovies/feature/home/impl/HomeUiState.kt`
- Create: `.../HomeViewModel.kt`
- Test: `feature/home/impl/src/test/kotlin/com/practice/thenewmovies/feature/home/impl/HomeViewModelTest.kt`

- [ ] **Step 1: Write `feature/home/impl/build.gradle.kts`**

The feature-impl convention plugin already brings Compose, Hilt, `core:ui`, `core:designsystem`, `core:model`, lifecycle, navigation3 and Coil, so only what is specific to Home appears here.

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.home.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.datastore)
    implementation(projects.feature.home.api)
    implementation(projects.feature.detail.api)
    implementation(projects.feature.search.api)
}
```

Home depends on `:feature:detail:api` and `:feature:search:api` — the keys and navigate extensions — and on neither feature's `impl`.

- [ ] **Step 2: Write `HomeUiState.kt`**

```kotlin
package com.practice.thenewmovies.feature.home.impl

import com.practice.thenewmovies.core.model.Movie

internal sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(
        val nowPlaying: List<Movie>,
        val upcoming: List<Movie>,
        val topRated: List<Movie>,
        val popular: List<Movie>,
    ) : HomeUiState {
        fun moviesForTab(tabIndex: Int): List<Movie> = when (tabIndex) {
            0 -> nowPlaying
            1 -> upcoming
            2 -> topRated
            3 -> popular
            else -> emptyList()
        }
    }

    data class Error(val message: String) : HomeUiState
}

internal val homeTabs = listOf("Now playing", "Upcoming", "Top rated", "Popular")
```

- [ ] **Step 3: Write the failing test**

```kotlin
package com.practice.thenewmovies.feature.home.impl

import app.cash.turbine.test
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testMovies
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import com.practice.thenewmovies.core.testing.repository.TestNetworkMonitor
import com.practice.thenewmovies.core.testing.repository.TestUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMoviesRepository()
    private val networkMonitor = TestNetworkMonitor()
    private val userPreferences = TestUserPreferencesRepository()

    private fun viewModel() = HomeViewModel(
        moviesRepository = moviesRepository,
        userPreferencesRepository = userPreferences,
        networkMonitor = networkMonitor,
    )

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `refreshes every category on creation`() = runTest {
        viewModel()

        assertEquals(MovieCategory.entries.toSet(), moviesRepository.refreshedCategories.toSet())
    }

    @Test
    fun `emits success once any category has movies`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            moviesRepository.emitMovies(MovieCategory.POPULAR, testMovies)

            val state = awaitItem()
            assertTrue(state is HomeUiState.Success)
            assertEquals(testMovies, (state as HomeUiState.Success).popular)
        }
    }

    @Test
    fun `reports an error when offline with nothing cached`() = runTest {
        networkMonitor.setOnline(false)
        moviesRepository.refreshSucceeds = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Error("No data available offline"), awaitItem())
        }
    }

    @Test
    fun `shows cached movies when offline`() = runTest {
        moviesRepository.emitMovies(MovieCategory.TOP_RATED, testMovies)
        networkMonitor.setOnline(false)
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is HomeUiState.Success)
        }
    }

    @Test
    fun `exposes offline state`() = runTest {
        val viewModel = viewModel()

        viewModel.isOffline.test {
            assertEquals(false, awaitItem())
            networkMonitor.setOnline(false)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `selecting a tab persists it`() = runTest {
        val viewModel = viewModel()

        viewModel.selectedTabIndex.test {
            assertEquals(0, awaitItem())
            viewModel.onTabSelected(2)
            assertEquals(2, awaitItem())
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :feature:home:impl:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: HomeViewModel`.

- [ ] **Step 5: Write `HomeViewModel.kt`**

```kotlin
package com.practice.thenewmovies.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.data.util.NetworkMonitor
import com.practice.thenewmovies.core.datastore.UserPreferencesRepository
import com.practice.thenewmovies.core.model.MovieCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OFFLINE_MESSAGE = "No data available offline"

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val moviesRepository: MoviesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        moviesRepository.getMovies(MovieCategory.NOW_PLAYING),
        moviesRepository.getMovies(MovieCategory.UPCOMING),
        moviesRepository.getMovies(MovieCategory.TOP_RATED),
        moviesRepository.getMovies(MovieCategory.POPULAR),
        networkMonitor.isOnline,
    ) { nowPlaying, upcoming, topRated, popular, isOnline ->
        val hasMovies = listOf(nowPlaying, upcoming, topRated, popular).any { it.isNotEmpty() }
        when {
            hasMovies -> HomeUiState.Success(
                nowPlaying = nowPlaying,
                upcoming = upcoming,
                topRated = topRated,
                popular = popular,
            )

            !isOnline -> HomeUiState.Error(OFFLINE_MESSAGE)

            else -> HomeUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { isOnline -> !isOnline }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val selectedTabIndex: StateFlow<Int> = userPreferencesRepository.preferences
        .map { it.selectedHomeTab }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    init {
        refresh()
    }

    fun onTabSelected(index: Int) {
        viewModelScope.launch { userPreferencesRepository.setSelectedHomeTab(index) }
    }

    fun refresh() {
        MovieCategory.entries.forEach { category ->
            viewModelScope.launch { moviesRepository.refresh(category) }
        }
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :feature:home:impl:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 7 tests passed.

If `emits success once any category has movies` sees `Loading` twice, `SharingStarted.WhileSubscribed` is replaying the initial value — use `expectMostRecentItem()` in that assertion rather than changing the ViewModel.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add feature/home/impl
git commit -m "feat(home): add HomeViewModel"
```

---

### Task 3: Home screen

**Files:**
- Create: `feature/home/impl/src/main/kotlin/com/practice/thenewmovies/feature/home/impl/HomeScreen.kt`
- Create: `.../navigation/HomeEntry.kt`
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Write `HomeScreen.kt`**

Two overloads of the same name: the stateful one wires Hilt, the stateless one is pure and previewable. `MovieCard` and the numbered featured poster stay private here — Home is their only consumer.

```kotlin
package com.practice.thenewmovies.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesSearchBar
import com.practice.thenewmovies.core.designsystem.theme.Montserrat
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.ui.DevicePreviews
import com.practice.thenewmovies.core.ui.component.MoviePoster
import com.practice.thenewmovies.core.ui.component.OfflineBanner
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isOffline = isOffline,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = viewModel::onTabSelected,
        onMovieClick = onMovieClick,
        onSearchClick = onSearchClick,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    isOffline: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "What do you want to watch?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        if (isOffline) OfflineBanner()

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(onClick = onSearchClick),
        ) {
            // The home search bar is a button: typing happens on the search screen.
            MoviesSearchBar(query = "", onQueryChange = {})
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            HomeUiState.Loading -> LoadingState()

            is HomeUiState.Error -> ErrorState(message = uiState.message)

            is HomeUiState.Success -> HomeContent(
                uiState = uiState,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
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
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState.Success,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))
        FeaturedRow(movies = uiState.topRated, onMovieClick = onMovieClick)
        CategoryTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
        )
        Spacer(modifier = Modifier.height(16.dp))
        MovieGrid(
            movies = uiState.moviesForTab(selectedTabIndex),
            onMovieClick = onMovieClick,
        )
    }
}

@Composable
private fun FeaturedRow(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        itemsIndexed(items = movies, key = { _, movie -> movie.id }) { index, movie ->
            NumberedPoster(
                movie = movie,
                position = index + 1,
                onClick = { onMovieClick(movie.id) },
            )
        }
    }
}

@Composable
private fun NumberedPoster(
    movie: Movie,
    position: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .width(160.dp)
            .height(260.dp)
            .clickable(onClick = onClick),
    ) {
        MoviePoster(
            posterPath = movie.posterPath,
            contentDescription = movie.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, bottom = 42.dp),
        )
        StrokedText(
            text = "$position",
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun StrokedText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 96.sp,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = Color(0xFF242A32),
) {
    Box(modifier = modifier.wrapContentSize()) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = Montserrat,
            fontWeight = FontWeight.SemiBold,
            color = strokeColor,
            style = TextStyle(drawStyle = Stroke(width = 5f)),
        )
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = Montserrat,
            fontWeight = FontWeight.SemiBold,
            color = fillColor,
        )
    }
}

@Composable
private fun CategoryTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollableTabRow(
        modifier = modifier,
        containerColor = Color.Transparent,
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            Box(
                Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(3.dp)
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF3A3F47)),
            )
        },
        divider = {},
    ) {
        homeTabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) Color.White else Color.Gray,
                    )
                },
            )
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .height(500.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = movies, key = { it.id }) { movie ->
            MoviePoster(
                posterPath = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .testTag("movie_card")
                    .clickable { onMovieClick(movie.id) },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    val movies = List(6) { index ->
        Movie(
            id = index,
            title = "Movie $index",
            overview = "Overview",
            posterPath = null,
            backdropPath = null,
            releaseDate = "2024-01-01",
            voteAverage = 7.5,
            voteCount = 100,
        )
    }
    MoviesTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                nowPlaying = movies,
                upcoming = movies,
                topRated = movies,
                popular = movies,
            ),
            isOffline = false,
            selectedTabIndex = 0,
            onTabSelected = {},
            onMovieClick = {},
            onSearchClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeScreenLoadingPreview() {
    MoviesTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            isOffline = false,
            selectedTabIndex = 0,
            onTabSelected = {},
            onMovieClick = {},
            onSearchClick = {},
        )
    }
}
```

- [ ] **Step 2: Write `navigation/HomeEntry.kt`**

The one public function in the module. It is what `:app` calls, and it is where Home's navigation targets are resolved through other features' `api` modules.

```kotlin
package com.practice.thenewmovies.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.api.navigateToDetail
import com.practice.thenewmovies.feature.home.api.HomeNavKey
import com.practice.thenewmovies.feature.home.impl.HomeScreen
import com.practice.thenewmovies.feature.search.api.navigateToSearch

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeScreen(
            onMovieClick = navigator::navigateToDetail,
            onSearchClick = navigator::navigateToSearch,
        )
    }
}
```

- [ ] **Step 3: Wire the entry into `:app`**

Add to `app/build.gradle.kts` dependencies:

```kotlin
    implementation(projects.feature.home.impl)
```

In `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`, replace the line

```kotlin
                entry<HomeNavKey> { Placeholder("Home") }
```

with

```kotlin
                homeEntry(navigator)
```

and add the import:

```kotlin
import com.practice.thenewmovies.feature.home.impl.navigation.homeEntry
```

`HomeNavKey` is still imported for the back-stack construction, so leave that import in place.

- [ ] **Step 4: Install and check by hand**

Run: `./gradlew :app:installDebug`
Then verify:
1. Home shows a numbered featured row and a 3-column grid of real posters.
2. The four category tabs switch the grid, and the choice survives killing and reopening the app.
3. Tapping a poster shows the Detail placeholder with the right movie id.
4. Tapping the search bar switches to the Search placeholder.
5. Turning on airplane mode and relaunching still shows the cached grid with the offline banner.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add feature/home app
git commit -m "feat(home): add home screen and register its entry"
```

---

# Slice 4 — `core:domain` and Detail

### Task 4: `core:domain`

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/domain/build.gradle.kts`
- Create: `core/domain/src/main/kotlin/com/practice/thenewmovies/core/domain/MovieDetailWithExtras.kt`
- Create: `.../GetMovieDetailUseCase.kt`
- Test: `core/domain/src/test/kotlin/com/practice/thenewmovies/core/domain/GetMovieDetailUseCaseTest.kt`

This is the only use case in the project. It exists because the detail screen needs five flows from two repositories combined into one state; every other screen reads a single repository method and calls it directly.

- [ ] **Step 1: Add the modules to `settings.gradle.kts`**

```kotlin
include(":core:domain")
include(":feature:detail:impl")
```

- [ ] **Step 2: Write `core/domain/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
```

- [ ] **Step 3: Write `MovieDetailWithExtras.kt`**

```kotlin
package com.practice.thenewmovies.core.domain

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review

/** Everything the detail screen renders, gathered from two repositories. */
data class MovieDetailWithExtras(
    val detail: MovieDetail?,
    val cast: List<Cast>,
    val reviews: List<Review>,
    val isInWatchlist: Boolean,
    val userRating: Float?,
)
```

- [ ] **Step 4: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.domain

import com.practice.thenewmovies.core.testing.data.testCast
import com.practice.thenewmovies.core.testing.data.testMovieDetail
import com.practice.thenewmovies.core.testing.data.testReviews
import com.practice.thenewmovies.core.testing.data.testWatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMovieDetailUseCaseTest {

    private val moviesRepository = TestMoviesRepository()
    private val watchlistRepository = TestWatchlistRepository()
    private val useCase = GetMovieDetailUseCase(moviesRepository, watchlistRepository)

    @Test
    fun `combines detail cast and reviews`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        moviesRepository.emitCast(testCast)
        moviesRepository.emitReviews(testReviews)

        val result = useCase(movieId = 1).first()

        assertEquals(testMovieDetail, result.detail)
        assertEquals(testCast, result.cast)
        assertEquals(testReviews, result.reviews)
    }

    @Test
    fun `reports the movie is not saved and has no user rating by default`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)

        val result = useCase(movieId = 1).first()

        assertFalse(result.isInWatchlist)
        assertNull(result.userRating)
    }

    @Test
    fun `reports watchlist membership and rating`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        watchlistRepository.emit(listOf(testWatchlistMovie.copy(userRating = 4f)))

        val result = useCase(movieId = testWatchlistMovie.id).first()

        assertTrue(result.isInWatchlist)
        assertEquals(4f, result.userRating)
    }

    @Test
    fun `emits a null detail while nothing is cached`() = runTest {
        val result = useCase(movieId = 1).first()

        assertNull(result.detail)
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `./gradlew :core:domain:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: GetMovieDetailUseCase`.

- [ ] **Step 6: Write `GetMovieDetailUseCase.kt`**

```kotlin
package com.practice.thenewmovies.core.domain

import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val moviesRepository: MoviesRepository,
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(movieId: Int): Flow<MovieDetailWithExtras> = combine(
        moviesRepository.getMovieDetail(movieId),
        moviesRepository.getCast(movieId),
        moviesRepository.getReviews(movieId),
        watchlistRepository.isInWatchlist(movieId),
        watchlistRepository.getRating(movieId),
    ) { detail, cast, reviews, isInWatchlist, userRating ->
        MovieDetailWithExtras(
            detail = detail,
            cast = cast,
            reviews = reviews,
            isInWatchlist = isInWatchlist,
            userRating = userRating,
        )
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :core:domain:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add core/domain settings.gradle.kts
git commit -m "feat(domain): add GetMovieDetailUseCase"
```

---

### Task 5: Detail ViewModel

**Files:**
- Create: `feature/detail/impl/build.gradle.kts`
- Create: `feature/detail/impl/src/main/kotlin/com/practice/thenewmovies/feature/detail/impl/DetailUiState.kt`
- Create: `.../DetailViewModel.kt`
- Test: `feature/detail/impl/src/test/kotlin/com/practice/thenewmovies/feature/detail/impl/DetailViewModelTest.kt`

`movieId` arrives as a navigation argument, and Navigation 3 entries have no `SavedStateHandle` carrying it, so the ViewModel uses assisted injection.

- [ ] **Step 1: Write `feature/detail/impl/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.detail.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.feature.detail.api)
}
```

- [ ] **Step 2: Write `DetailUiState.kt`**

```kotlin
package com.practice.thenewmovies.feature.detail.impl

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review

internal sealed interface DetailUiState {

    data object Loading : DetailUiState

    data class Success(
        val detail: MovieDetail,
        val cast: List<Cast>,
        val reviews: List<Review>,
        val isInWatchlist: Boolean,
        val userRating: Float?,
    ) : DetailUiState

    data class Error(val message: String) : DetailUiState
}

internal val detailTabs = listOf("About Movie", "Reviews", "Cast")
```

- [ ] **Step 3: Write the failing test**

```kotlin
package com.practice.thenewmovies.feature.detail.impl

import app.cash.turbine.test
import com.practice.thenewmovies.core.domain.GetMovieDetailUseCase
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testCast
import com.practice.thenewmovies.core.testing.data.testMovieDetail
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import com.practice.thenewmovies.core.testing.repository.TestNetworkMonitor
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMoviesRepository()
    private val watchlistRepository = TestWatchlistRepository()
    private val networkMonitor = TestNetworkMonitor()

    private fun viewModel(movieId: Int = 1) = DetailViewModel(
        movieId = movieId,
        getMovieDetail = GetMovieDetailUseCase(moviesRepository, watchlistRepository),
        moviesRepository = moviesRepository,
        watchlistRepository = watchlistRepository,
        networkMonitor = networkMonitor,
    )

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(DetailUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `refreshes the detail on creation`() = runTest {
        viewModel(movieId = 7)

        assertEquals(listOf(7), moviesRepository.refreshedDetailIds)
    }

    @Test
    fun `emits success once the detail is cached`() = runTest {
        val viewModel = viewModel()
        moviesRepository.emitDetail(testMovieDetail)
        moviesRepository.emitCast(testCast)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DetailUiState.Success)
            assertEquals(testMovieDetail, (state as DetailUiState.Success).detail)
            assertEquals(testCast, state.cast)
        }
    }

    @Test
    fun `reports an error when the refresh fails with nothing cached`() = runTest {
        moviesRepository.refreshSucceeds = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(DetailUiState.Loading, awaitItem())
            assertEquals(DetailUiState.Error("Could not load this movie"), awaitItem())
        }
    }

    @Test
    fun `toggling the watchlist adds then removes the movie`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        val viewModel = viewModel(movieId = testMovieDetail.id)
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.toggleWatchlist()
        assertTrue(watchlistRepository.isInWatchlist(testMovieDetail.id).first())

        viewModel.toggleWatchlist()
        assertFalse(watchlistRepository.isInWatchlist(testMovieDetail.id).first())
    }

    @Test
    fun `submitting a rating saves the movie first`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        val viewModel = viewModel(movieId = testMovieDetail.id)
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.submitRating(4.5f)

        assertTrue(watchlistRepository.isInWatchlist(testMovieDetail.id).first())
        assertEquals(4.5f, watchlistRepository.getRating(testMovieDetail.id).first())
    }

    @Test
    fun `the rating sheet opens and closes`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.showRatingSheet.value)
        viewModel.onRateClick()
        assertTrue(viewModel.showRatingSheet.value)
        viewModel.onRatingSheetDismissed()
        assertFalse(viewModel.showRatingSheet.value)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :feature:detail:impl:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: DetailViewModel`.

- [ ] **Step 5: Write `DetailViewModel.kt`**

`refreshFailed` is a separate flow rather than a field on the state, so a failed network call can turn into an error only while nothing is cached — a stale detail keeps rendering.

```kotlin
package com.practice.thenewmovies.feature.detail.impl

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.data.util.NetworkMonitor
import com.practice.thenewmovies.core.domain.GetMovieDetailUseCase
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.WatchlistMovie
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val LOAD_FAILED_MESSAGE = "Could not load this movie"

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
internal class DetailViewModel @AssistedInject constructor(
    @Assisted private val movieId: Int,
    getMovieDetail: GetMovieDetailUseCase,
    private val moviesRepository: MoviesRepository,
    private val watchlistRepository: WatchlistRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(movieId: Int): DetailViewModel
    }

    private val refreshFailed = MutableStateFlow(false)
    private val _showRatingSheet = MutableStateFlow(false)

    val showRatingSheet: StateFlow<Boolean> = _showRatingSheet.asStateFlow()

    val uiState: StateFlow<DetailUiState> = combine(
        getMovieDetail(movieId),
        refreshFailed,
    ) { extras, failed ->
        val detail = extras.detail
        when {
            detail != null -> DetailUiState.Success(
                detail = detail,
                cast = extras.cast,
                reviews = extras.reviews,
                isInWatchlist = extras.isInWatchlist,
                userRating = extras.userRating,
            )

            failed -> DetailUiState.Error(LOAD_FAILED_MESSAGE)

            else -> DetailUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { isOnline -> !isOnline }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshFailed.value = !moviesRepository.refreshDetail(movieId)
        }
    }

    fun onRateClick() {
        _showRatingSheet.value = true
    }

    fun onRatingSheetDismissed() {
        _showRatingSheet.value = false
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val state = uiState.value as? DetailUiState.Success ?: return@launch
            if (state.isInWatchlist) {
                watchlistRepository.removeFromWatchlist(movieId)
            } else {
                watchlistRepository.addToWatchlist(state.detail.asWatchlistMovie())
            }
        }
    }

    fun submitRating(rating: Float) {
        viewModelScope.launch {
            val state = uiState.value as? DetailUiState.Success ?: return@launch
            if (!state.isInWatchlist) {
                watchlistRepository.addToWatchlist(state.detail.asWatchlistMovie())
            }
            watchlistRepository.setRating(movieId = movieId, rating = rating)
            _showRatingSheet.value = false
        }
    }
}

private fun MovieDetail.asWatchlistMovie() = WatchlistMovie(
    id = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime?.toInt() ?: 0,
    genre = genres.firstOrNull()?.name.orEmpty(),
)
```

Note the difference from the reference implementation: it re-prefixed `posterPath` with the TMDB base URL when saving to the watchlist, double-prefixing a path the mapper had already made absolute. Here the mapper is the only place that builds image URLs.

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :feature:detail:impl:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add feature/detail/impl
git commit -m "feat(detail): add DetailViewModel"
```

---

### Task 6: Detail screen

**Files:**
- Create: `feature/detail/impl/src/main/kotlin/com/practice/thenewmovies/feature/detail/impl/DetailScreen.kt`
- Create: `.../navigation/DetailEntry.kt`
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`, `app/build.gradle.kts`

The reference `DetailMovieScreen` is 594 lines in one function-heavy file. The port keeps the same visual result but splits it: header, meta row, tab bodies, review card, cast card and the rating sheet each become a `private` composable.

- [ ] **Step 1: Write `DetailScreen.kt`**

```kotlin
package com.practice.thenewmovies.feature.detail.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.ui.component.MetaLabel
import com.practice.thenewmovies.core.ui.component.MoviePoster
import com.practice.thenewmovies.core.ui.component.OfflineBanner
import com.practice.thenewmovies.core.ui.component.RatingChip

@Composable
internal fun DetailScreen(
    movieId: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(movieId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val showRatingSheet by viewModel.showRatingSheet.collectAsStateWithLifecycle()

    DetailScreen(
        uiState = uiState,
        isOffline = isOffline,
        showRatingSheet = showRatingSheet,
        onBackClick = onBackClick,
        onBookmarkClick = viewModel::toggleWatchlist,
        onRateClick = viewModel::onRateClick,
        onRatingSheetDismissed = viewModel::onRatingSheetDismissed,
        onRatingSubmitted = viewModel::submitRating,
        modifier = modifier,
    )
}

@Composable
internal fun DetailScreen(
    uiState: DetailUiState,
    isOffline: Boolean,
    showRatingSheet: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onRateClick: () -> Unit,
    onRatingSheetDismissed: () -> Unit,
    onRatingSubmitted: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (uiState) {
            DetailUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )

            is DetailUiState.Error -> Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )

            is DetailUiState.Success -> {
                DetailContent(
                    state = uiState,
                    isOffline = isOffline,
                    onRateClick = onRateClick,
                )
                DetailToolbar(
                    isInWatchlist = uiState.isInWatchlist,
                    onBackClick = onBackClick,
                    onBookmarkClick = onBookmarkClick,
                )
                if (showRatingSheet) {
                    RatingSheet(
                        initialRating = uiState.userRating ?: uiState.detail.voteAverage.toFloat(),
                        onDismiss = onRatingSheetDismissed,
                        onConfirm = onRatingSubmitted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState.Success,
    isOffline: Boolean,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (isOffline) OfflineBanner()

        DetailHeader(state = state, onRateClick = onRateClick)

        Spacer(modifier = Modifier.height(16.dp))
        DetailMetaRow(state = state)

        Spacer(modifier = Modifier.height(24.dp))
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            },
            divider = {},
        ) {
            detailTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color.White else MetaGrey,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            when (selectedTab) {
                0 -> AboutTab(overview = state.detail.overview.orEmpty())
                1 -> ReviewsTab(reviews = state.reviews)
                else -> CastTab(cast = state.cast)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun DetailHeader(
    state: DetailUiState.Success,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(271.dp),
    ) {
        AsyncImage(
            model = detail.backdropPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x44000000), MaterialTheme.colorScheme.surface),
                    ),
                ),
        )
        RatingChip(
            rating = state.userRating ?: detail.voteAverage.toFloat(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 148.dp, end = 16.dp)
                .clickable(onClick = onRateClick),
        )
        MoviePoster(
            posterPath = detail.posterPath,
            contentDescription = detail.title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 29.dp)
                .size(width = 95.dp, height = 120.dp),
        )
        Text(
            text = detail.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 136.dp, end = 29.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun DetailMetaRow(state: DetailUiState.Success, modifier: Modifier = Modifier) {
    val detail = state.detail
    Row(
        modifier = modifier.padding(start = 29.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaLabel(icon = Icons.Outlined.CalendarToday, label = detail.releaseDate.take(4))
        Text(text = "|", color = MetaGrey, style = MaterialTheme.typography.labelMedium)
        MetaLabel(
            icon = Icons.Outlined.AccessTime,
            label = "${detail.runtime ?: 0} Minutes",
        )
        Text(text = "|", color = MetaGrey, style = MaterialTheme.typography.labelMedium)
        MetaLabel(
            icon = Icons.Outlined.LocalActivity,
            label = detail.genres.firstOrNull()?.name.orEmpty(),
        )
    }
}

@Composable
private fun DetailToolbar(
    isInWatchlist: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
        Text(
            text = "Detail",
            color = Color(0xFFECECEC),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onBookmarkClick) {
            Icon(
                imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isInWatchlist) "Remove from watch list" else "Add to watch list",
                tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
    }
}

@Composable
private fun AboutTab(overview: String, modifier: Modifier = Modifier) {
    Text(
        text = overview,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

@Composable
private fun ReviewsTab(reviews: List<Review>, modifier: Modifier = Modifier) {
    if (reviews.isEmpty()) {
        Text(
            text = "No reviews yet.",
            color = MetaGrey,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        reviews.forEach { review -> ReviewCard(review = review) }
    }
}

@Composable
private fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            AsyncImage(
                model = review.avatarPath,
                contentDescription = review.author,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PlaceholderGrey),
                contentScale = ContentScale.Crop,
            )
            review.rating?.let { rating ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.1f".format(rating),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = review.author,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.content,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CastTab(cast: List<Cast>, modifier: Modifier = Modifier) {
    if (cast.isEmpty()) {
        Text(
            text = "No cast information.",
            color = MetaGrey,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        cast.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowItems.forEach { member ->
                    CastCard(cast = member, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CastCard(cast: Cast, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = cast.profilePath,
            contentDescription = cast.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PlaceholderGrey),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cast.name,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingSheet(
    initialRating: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var rating by remember { mutableFloatStateOf(initialRating.coerceIn(0f, 10f)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Rate this movie",
                color = SheetTextColor,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "%.1f".format(rating),
                color = SheetTextColor,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Skip for now",
                    color = SheetTextColor,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onConfirm(rating) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OK",
                    color = Color(0xFFFCFCFC),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private val MetaGrey = Color(0xFF92929D)
private val PlaceholderGrey = Color(0xFF3A3F47)
private val SheetTextColor = Color(0xFF4E4B66)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DetailScreenPreview() {
    MoviesTheme {
        DetailScreen(
            uiState = DetailUiState.Success(
                detail = com.practice.thenewmovies.core.model.MovieDetail(
                    id = 1,
                    title = "Dune",
                    originalTitle = "Dune",
                    originalLanguage = "en",
                    overview = "A noble family becomes embroiled in a war for a desert planet.",
                    genres = listOf(
                        com.practice.thenewmovies.core.model.Genre(id = 878, name = "Sci-Fi"),
                    ),
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2021-10-22",
                    runtime = 155,
                    status = "Released",
                    video = false,
                    voteAverage = 7.8,
                    voteCount = 1000,
                ),
                cast = emptyList(),
                reviews = emptyList(),
                isInWatchlist = false,
                userRating = null,
            ),
            isOffline = false,
            showRatingSheet = false,
            onBackClick = {},
            onBookmarkClick = {},
            onRateClick = {},
            onRatingSheetDismissed = {},
            onRatingSubmitted = {},
        )
    }
}
```

- [ ] **Step 2: Write `navigation/DetailEntry.kt`**

```kotlin
package com.practice.thenewmovies.feature.detail.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.api.DetailNavKey
import com.practice.thenewmovies.feature.detail.impl.DetailScreen

fun EntryProviderScope<NavKey>.detailEntry(navigator: Navigator) {
    entry<DetailNavKey> { key ->
        DetailScreen(
            movieId = key.movieId,
            onBackClick = { navigator.goBack() },
        )
    }
}
```

- [ ] **Step 3: Wire the entry into `:app`**

Add to `app/build.gradle.kts` dependencies:

```kotlin
    implementation(projects.feature.detail.impl)
```

In `MoviesApp.kt`, replace

```kotlin
                entry<DetailNavKey> { key -> Placeholder("Detail ${key.movieId}") }
```

with

```kotlin
                detailEntry(navigator)
```

and add the import:

```kotlin
import com.practice.thenewmovies.feature.detail.impl.navigation.detailEntry
```

- [ ] **Step 4: Install and check by hand**

Run: `./gradlew :app:installDebug`
Then verify:
1. Tapping a Home poster opens Detail with backdrop, poster, title, and the meta row.
2. The bottom bar is hidden on Detail and back returns to Home with the grid position intact.
3. About, Reviews and Cast tabs all render; empty reviews and empty cast show their messages.
4. The bookmark icon fills when tapped and stays filled after leaving and re-entering the screen.
5. Tapping the rating chip opens the sheet; confirming a rating updates the chip.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add feature/detail app
git commit -m "feat(detail): add detail screen and register its entry"
```

---

### Task 7: Verify slices 3 and 4

- [ ] **Step 1: Full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`, 34 unit tests passing across all modules.

- [ ] **Step 2: Confirm the api/impl boundary holds**

Run: `./gradlew :feature:home:impl:dependencies --configuration debugCompileClasspath | grep "feature:detail"`
Expected: exactly one line naming `feature:detail:api`. If `feature:detail:impl` appears, a dependency was added to the wrong module and the split is broken.

- [ ] **Step 3: Formatting**

Run: `./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`

---

## Done when

- Home shows real TMDB data, remembers its tab across process death, and renders cached data with a banner when offline.
- Detail shows all three tabs, watchlist toggling persists, and rating updates the chip.
- Home compiles against `:feature:detail:api` and `:feature:search:api` only.
- `core:domain` holds exactly one use case.
