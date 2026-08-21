# TheNewMovies Slice 1 — App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An installable app with the real theme, a working bottom bar, and Navigation 3 wired through `core:navigation` — showing placeholder content until the feature impl modules land.

**Architecture:** `core:designsystem` owns the theme and themed primitives and knows nothing about app models. `core:navigation` owns `NavigationState` (a per-tab stack of sub-stacks) and `Navigator`. Each feature's `api` module contributes only a `@Serializable` `NavKey` plus a navigate extension. `:app` holds `MainActivity`, `MoviesApp`, `TopLevelNavItem` and the single `entryProvider {}`.

**Tech Stack:** Navigation 3 (`navigation3-runtime` 1.0.0, `navigation3-ui` 1.0.0), Compose Material 3, Hilt.

**Depends on:** Slice 0 complete (`./gradlew build` green).

**Spec:** `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`

---

## File Structure

| File | Responsibility |
| --- | --- |
| `core/designsystem/build.gradle.kts` | library + compose |
| `core/designsystem/src/main/kotlin/.../core/designsystem/theme/Color.kt` | Palette |
| `.../theme/Type.kt` | Typography using the Montserrat family |
| `.../theme/Theme.kt` | `MoviesTheme` |
| `.../component/MoviesTopAppBar.kt` | Back + title + optional action row |
| `.../component/MoviesSearchBar.kt` | Rounded search field |
| `.../icon/MoviesIcons.kt` | Drawable id constants |
| `core/designsystem/src/main/res/font/core_designsystem_montserrat_*.ttf` | Fonts |
| `core/designsystem/src/main/res/drawable/core_designsystem_ic_*.xml` | Shared vectors |
| `core/navigation/build.gradle.kts` | library, no compose compiler needed |
| `core/navigation/src/main/kotlin/.../core/navigation/NavigationState.kt` | Tab stacks and current tab |
| `core/navigation/src/main/kotlin/.../core/navigation/Navigator.kt` | The verbs features call |
| `feature/<name>/api/build.gradle.kts` + `.../<Name>NavKey.kt` | One key + one navigate extension per feature |
| `app/src/main/kotlin/.../MoviesApplication.kt` | `@HiltAndroidApp`, Timber |
| `app/src/main/kotlin/.../MainActivity.kt` | `@AndroidEntryPoint`, `setContent { MoviesTheme { MoviesApp() } }` |
| `app/src/main/kotlin/.../ui/MoviesApp.kt` | Scaffold, bottom bar, `NavDisplay`, `entryProvider {}` |
| `app/src/main/kotlin/.../navigation/TopLevelNavItem.kt` | The three tabs |

---

### Task 1: Catalog and settings additions

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Confirm the Compose runtime entries are in the catalog**

`core:navigation` needs snapshot state but has no `@Composable` functions, so it takes the runtime artifact alone rather than the whole Compose plugin. Slice 0 already added `androidx-compose-runtime` and `androidx-compose-runtime-saveable` under the `# Compose` group — verify with:

```bash
grep compose-runtime gradle/libs.versions.toml
```

- [ ] **Step 2: Add the new modules to `settings.gradle.kts`**

Replace the two existing `include` lines with:

```kotlin
include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":feature:detail:api")
include(":feature:home:api")
include(":feature:search:api")
include(":feature:watchlist:api")
```

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts
git commit -m "build: include app shell modules"
```

---

### Task 2: `core:designsystem`

**Files:**
- Create: `core/designsystem/build.gradle.kts`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/theme/Color.kt`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/theme/Type.kt`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/theme/Theme.kt`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/component/MoviesTopAppBar.kt`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/component/MoviesSearchBar.kt`
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/icon/MoviesIcons.kt`
- Create: `core/designsystem/src/main/res/font/*`, `core/designsystem/src/main/res/drawable/*`

The reference repo keeps the theme in `:app` and the fonts and shared components in `core:ui`. Both move here, because `core:ui` in this project is reserved for composites that render models.

- [ ] **Step 1: Write `core/designsystem/build.gradle.kts`**

```kotlin
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
```

- [ ] **Step 2: Copy the fonts and shared drawables, renaming for the resource prefix**

The library convention plugin sets `resourcePrefix = "core_designsystem_"`, so every resource file must carry that prefix or AGP fails the build.

```bash
cd /Users/danhtruong/android/TheNewMovies
mkdir -p core/designsystem/src/main/res/font core/designsystem/src/main/res/drawable
SRC=/Users/danhtruong/android/TheMovies/core/ui/src/main/res
for w in regular medium semibold bold; do
  cp "$SRC/font/montserrat_$w.ttf" "core/designsystem/src/main/res/font/core_designsystem_montserrat_$w.ttf"
done
cp "$SRC/drawable/ic_back_icon.xml" core/designsystem/src/main/res/drawable/core_designsystem_ic_back_icon.xml
cp "$SRC/drawable/ic_search_left.xml" core/designsystem/src/main/res/drawable/core_designsystem_ic_search_left.xml
```

- [ ] **Step 3: Write `Color.kt`**

Only the three colours the app actually uses. The default Material purples in the reference repo are unreferenced and are not ported.

```kotlin
package com.practice.thenewmovies.core.designsystem.theme

import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF0296E5)
val DarkGray = Color(0xFF121212)
val Gray = Color(0xFF67686D)
val SurfaceVariantGray = Color(0xFF2C2C2C)
val ErrorRed = Color(0xFFB00020)
```

- [ ] **Step 4: Write `Type.kt`**

```kotlin
package com.practice.thenewmovies.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.practice.thenewmovies.core.designsystem.R

val Montserrat = FontFamily(
    Font(R.font.core_designsystem_montserrat_regular),
    Font(R.font.core_designsystem_montserrat_medium, FontWeight.Medium),
    Font(R.font.core_designsystem_montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.core_designsystem_montserrat_bold, FontWeight.Bold),
)

internal val MoviesTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

- [ ] **Step 5: Write `Theme.kt`**

The app is dark-only by design (the reference repo declares a light scheme filled with dark colours; this states the intent directly).

```kotlin
package com.practice.thenewmovies.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MoviesColorScheme = darkColorScheme(
    primary = Blue,
    secondary = Gray,
    surface = DarkGray,
    background = DarkGray,
    surfaceVariant = SurfaceVariantGray,
    error = ErrorRed,
)

@Composable
fun MoviesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoviesColorScheme,
        typography = MoviesTypography,
        content = content,
    )
}
```

- [ ] **Step 6: Write `MoviesIcons.kt`**

Drawable ids in one place so features never touch `R` from another module.

```kotlin
package com.practice.thenewmovies.core.designsystem.icon

import com.practice.thenewmovies.core.designsystem.R

object MoviesIcons {
    val Back = R.drawable.core_designsystem_ic_back_icon
    val Search = R.drawable.core_designsystem_ic_search_left
}
```

- [ ] **Step 7: Write `MoviesTopAppBar.kt`**

Ported from `TheMovies/core/ui/src/main/java/com/practice/ui/ToolBar.kt`, with the icon id taken from `MoviesIcons` and the trailing action made optional without a magic `0` default.

```kotlin
package com.practice.thenewmovies.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.icon.MoviesIcons
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
fun MoviesTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: Int? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(MoviesIcons.Back),
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onBackClick),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (actionIcon != null) {
            Icon(
                painter = painterResource(actionIcon),
                contentDescription = "Action",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onActionClick),
            )
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}

@Preview
@Composable
private fun MoviesTopAppBarPreview() {
    MoviesTheme {
        MoviesTopAppBar(title = "Movie Title", onBackClick = {})
    }
}
```

- [ ] **Step 8: Write `MoviesSearchBar.kt`**

Ported from `TheMovies/core/ui/src/main/java/com/practice/ui/SearchBar.kt`. The reference version keeps the query in its own `remember`, which makes it impossible to clear from outside; here the query is hoisted to the caller.

```kotlin
package com.practice.thenewmovies.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.icon.MoviesIcons
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
fun MoviesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            )
            Icon(
                painter = painterResource(MoviesIcons.Search),
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MoviesSearchBarPreview() {
    MoviesTheme {
        MoviesSearchBar(query = "", onQueryChange = {})
    }
}
```

- [ ] **Step 9: Build and commit**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If AGP reports `Resource ... does not start with the specified prefix`, a file in `res/` was not renamed in Step 2.

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add theme, typography and shared components"
```

---

### Task 3: `core:navigation`

**Files:**
- Create: `core/navigation/build.gradle.kts`
- Create: `core/navigation/src/main/kotlin/com/practice/thenewmovies/core/navigation/NavigationState.kt`
- Create: `core/navigation/src/main/kotlin/com/practice/thenewmovies/core/navigation/Navigator.kt`
- Test: `core/navigation/src/test/kotlin/com/practice/thenewmovies/core/navigation/NavigatorTest.kt`

This is the module that fixes a real bug in the reference app: its `navigateToTab` clears the entire back stack, so switching tabs destroys the previous tab's history. Here every top-level key owns its own sub-stack.

`NavigationState` takes its stacks and its current-tab index as constructor parameters rather than creating them, so `:app` can supply `rememberNavBackStack(...)` (which survives process death) and tests can supply plain snapshot lists.

- [ ] **Step 1: Write `core/navigation/build.gradle.kts`**

No `themovies.android.compose` here — the module has no `@Composable` functions, only snapshot state.

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
}

android {
    namespace = "com.practice.thenewmovies.core.navigation"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.navigation3.runtime)

    // The BOM must be repeated for the test configuration: compose-runtime has no version
    // of its own in the catalog, and `api(platform(...))` does not reach testImplementation.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.runtime)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private object Home : NavKey
    private object Search : NavKey
    private object Watchlist : NavKey
    private data class Detail(val movieId: Int) : NavKey

    private fun navigator(): Navigator {
        val state = NavigationState(
            subStacks = linkedMapOf(
                Home to mutableStateListOf<NavKey>(Home),
                Search to mutableStateListOf<NavKey>(Search),
                Watchlist to mutableStateListOf<NavKey>(Watchlist),
            ),
            currentIndex = mutableIntStateOf(0),
        )
        return Navigator(state)
    }

    @Test
    fun `starts on the first top level key`() {
        val navigator = navigator()

        assertEquals(Home, navigator.state.currentTopLevelKey)
        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `navigating to a non top level key pushes onto the current sub stack`() {
        val navigator = navigator()

        navigator.navigate(Detail(42))

        assertEquals(listOf(Home, Detail(42)), navigator.state.backStack)
        assertEquals(Detail(42), navigator.state.currentKey)
    }

    @Test
    fun `switching tabs preserves the previous tab history`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))

        navigator.navigate(Search)
        assertEquals(listOf(Search), navigator.state.backStack)

        navigator.navigate(Home)
        assertEquals(listOf(Home, Detail(42)), navigator.state.backStack)
    }

    @Test
    fun `re-selecting the current tab clears its sub stack`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))
        navigator.navigate(Detail(43))

        navigator.navigate(Home)

        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `going back pops the current sub stack`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))

        assertTrue(navigator.goBack())

        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `going back from a tab root returns to the first tab`() {
        val navigator = navigator()
        navigator.navigate(Watchlist)

        assertTrue(navigator.goBack())

        assertEquals(Home, navigator.state.currentTopLevelKey)
    }

    @Test
    fun `going back at the start key does nothing and reports it`() {
        val navigator = navigator()

        assertFalse(navigator.goBack())

        assertEquals(Home, navigator.state.currentTopLevelKey)
        assertEquals(listOf(Home), navigator.state.backStack)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :core:navigation:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: NavigationState`.

- [ ] **Step 4: Write `NavigationState.kt`**

```kotlin
package com.practice.thenewmovies.core.navigation

import androidx.compose.runtime.MutableIntState
import androidx.navigation3.runtime.NavKey

/**
 * The app's navigation state: one back stack per top-level destination, plus which one is showing.
 *
 * Sub-stacks and the current index are passed in rather than created, so `:app` can hand over
 * state that survives process death while tests hand over plain snapshot state.
 *
 * @param subStacks ordered map of top-level key to that tab's stack; each stack must start with
 *   its own top-level key. Iteration order defines tab order, so pass a [LinkedHashMap].
 * @param currentIndex index into [topLevelKeys] of the visible tab.
 */
class NavigationState(
    private val subStacks: Map<NavKey, MutableList<NavKey>>,
    private val currentIndex: MutableIntState,
) {
    val topLevelKeys: List<NavKey> = subStacks.keys.toList()

    init {
        require(topLevelKeys.isNotEmpty()) { "NavigationState needs at least one top-level key" }
        require(subStacks.all { (key, stack) -> stack.firstOrNull() == key }) {
            "Each sub-stack must start with its own top-level key"
        }
        require(currentIndex.intValue in topLevelKeys.indices) {
            "currentIndex ${currentIndex.intValue} is out of bounds"
        }
    }

    val currentTopLevelKey: NavKey get() = topLevelKeys[currentIndex.intValue]

    /** The stack `NavDisplay` renders. Reads are snapshot-observed, so recomposition follows. */
    val backStack: MutableList<NavKey> get() = subStacks.getValue(currentTopLevelKey)

    val currentKey: NavKey get() = backStack.last()

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun goToTopLevel(key: NavKey) {
        val index = topLevelKeys.indexOf(key)
        require(index >= 0) { "$key is not a top-level key" }
        currentIndex.intValue = index
    }

    fun clearSubStack() {
        val stack = backStack
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }

    /** Pops one entry, or falls back to the first tab. Returns false when already at the start. */
    fun pop(): Boolean {
        val stack = backStack
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (currentIndex.intValue != 0) {
            currentIndex.intValue = 0
            return true
        }
        return false
    }
}
```

- [ ] **Step 5: Write `Navigator.kt`**

This is the only navigation type features see. Feature `api` modules add extensions on it.

```kotlin
package com.practice.thenewmovies.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * The verbs features use to navigate. Feature `api` modules add typed extensions on this type,
 * which is how a feature can navigate to another feature without depending on its `impl`.
 */
class Navigator(val state: NavigationState) {

    fun navigate(key: NavKey) {
        when {
            key == state.currentTopLevelKey -> state.clearSubStack()
            key in state.topLevelKeys -> state.goToTopLevel(key)
            else -> state.push(key)
        }
    }

    /** Returns false when there is nothing left to pop. */
    fun goBack(): Boolean = state.pop()
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :core:navigation:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add core/navigation
git commit -m "feat(navigation): add NavigationState with per-tab sub-stacks and Navigator"
```

---

### Task 4: Feature `api` modules

**Files:**
- Create: `feature/home/api/build.gradle.kts`, `feature/home/api/src/main/kotlin/com/practice/thenewmovies/feature/home/api/HomeNavKey.kt`
- Create: `feature/search/api/build.gradle.kts`, `.../feature/search/api/SearchNavKey.kt`
- Create: `feature/watchlist/api/build.gradle.kts`, `.../feature/watchlist/api/WatchlistNavKey.kt`
- Create: `feature/detail/api/build.gradle.kts`, `.../feature/detail/api/DetailNavKey.kt`

All four are created now, even though only three are tabs, because `:app` needs every key to build its `entryProvider` and `:feature:home:impl` will need `DetailNavKey` in slice 4. Each module is two files and no logic.

- [ ] **Step 1: Write the four build files**

`feature/home/api/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.api)
}

android {
    namespace = "com.practice.thenewmovies.feature.home.api"
}
```

`feature/search/api/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.api)
}

android {
    namespace = "com.practice.thenewmovies.feature.search.api"
}
```

`feature/watchlist/api/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.api)
}

android {
    namespace = "com.practice.thenewmovies.feature.watchlist.api"
}
```

`feature/detail/api/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.api)
}

android {
    namespace = "com.practice.thenewmovies.feature.detail.api"
}
```

- [ ] **Step 2: Write the four keys**

`HomeNavKey.kt`:

```kotlin
package com.practice.thenewmovies.feature.home.api

import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object HomeNavKey : NavKey

fun Navigator.navigateToHome() = navigate(HomeNavKey)
```

`SearchNavKey.kt`:

```kotlin
package com.practice.thenewmovies.feature.search.api

import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object SearchNavKey : NavKey

fun Navigator.navigateToSearch() = navigate(SearchNavKey)
```

`WatchlistNavKey.kt`:

```kotlin
package com.practice.thenewmovies.feature.watchlist.api

import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object WatchlistNavKey : NavKey

fun Navigator.navigateToWatchlist() = navigate(WatchlistNavKey)
```

`DetailNavKey.kt`:

```kotlin
package com.practice.thenewmovies.feature.detail.api

import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class DetailNavKey(val movieId: Int) : NavKey

fun Navigator.navigateToDetail(movieId: Int) = navigate(DetailNavKey(movieId))
```

- [ ] **Step 3: Build and commit**

Run: `./gradlew :feature:home:api:assembleDebug :feature:search:api:assembleDebug :feature:watchlist:api:assembleDebug :feature:detail:api:assembleDebug`
Expected: `BUILD SUCCESSFUL`

```bash
./gradlew spotlessApply
git add feature
git commit -m "feat(navigation): add feature api modules with nav keys"
```

---

### Task 5: `:app`

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_home.xml`, `ic_search.xml`, `ic_save.xml` (copied)
- Create: `app/src/main/kotlin/com/practice/thenewmovies/MoviesApplication.kt`
- Create: `app/src/main/kotlin/com/practice/thenewmovies/MainActivity.kt`
- Create: `app/src/main/kotlin/com/practice/thenewmovies/navigation/TopLevelNavItem.kt`
- Create: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`

Signing configs from the reference repo are not ported: it reads a `keystore.properties` that does not exist here, and debug builds sign themselves.

- [ ] **Step 1: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.application)
    alias(libs.plugins.themovies.android.compose)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies"

    defaultConfig {
        applicationId = "com.practice.thenewmovies"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.feature.detail.api)
    implementation(projects.feature.home.api)
    implementation(projects.feature.search.api)
    implementation(projects.feature.watchlist.api)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
```

- [ ] **Step 2: Create the release proguard file**

R8 is on for release builds, so the file must exist.

```bash
printf '# Keep default rules; add project-specific rules here.\n' > app/proguard-rules.pro
```

- [ ] **Step 3: Copy the bottom-bar icons**

The app module has no resource prefix, so no renaming is needed.

```bash
mkdir -p app/src/main/res/drawable
SRC=/Users/danhtruong/android/TheMovies/app/src/main/res/drawable
cp "$SRC/ic_home.xml" "$SRC/ic_search.xml" "$SRC/ic_save.xml" app/src/main/res/drawable/
```

- [ ] **Step 4: Write the manifest and values**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".MoviesApplication"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.TheNewMovies">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.TheNewMovies">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">The New Movies</string>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.TheNewMovies" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#121212</item>
        <item name="android:navigationBarColor">#121212</item>
        <item name="android:windowBackground">#121212</item>
    </style>
</resources>
```

- [ ] **Step 5: Write `MoviesApplication.kt`**

```kotlin
package com.practice.thenewmovies

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MoviesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
```

- [ ] **Step 6: Write `TopLevelNavItem.kt`**

```kotlin
package com.practice.thenewmovies.navigation

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.R
import com.practice.thenewmovies.feature.home.api.HomeNavKey
import com.practice.thenewmovies.feature.search.api.SearchNavKey
import com.practice.thenewmovies.feature.watchlist.api.WatchlistNavKey

enum class TopLevelNavItem(
    val key: NavKey,
    @DrawableRes val icon: Int,
    val label: String,
) {
    Home(HomeNavKey, R.drawable.ic_home, "Home"),
    Search(SearchNavKey, R.drawable.ic_search, "Search"),
    Watchlist(WatchlistNavKey, R.drawable.ic_save, "Watch List"),
}
```

- [ ] **Step 7: Write `MoviesApp.kt`**

`rememberNavBackStack` gives each tab a stack that survives process death; the Android overload needs no serializers module because every key is `@Serializable`. The `entryProvider` block is the single place features are composed — slices 3 to 6 replace one placeholder each.

```kotlin
package com.practice.thenewmovies.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.practice.thenewmovies.core.navigation.NavigationState
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.api.DetailNavKey
import com.practice.thenewmovies.feature.home.api.HomeNavKey
import com.practice.thenewmovies.feature.search.api.SearchNavKey
import com.practice.thenewmovies.feature.watchlist.api.WatchlistNavKey
import com.practice.thenewmovies.navigation.TopLevelNavItem

@Composable
fun MoviesApp() {
    val homeStack = rememberNavBackStack(HomeNavKey)
    val searchStack = rememberNavBackStack(SearchNavKey)
    val watchlistStack = rememberNavBackStack(WatchlistNavKey)
    val currentIndex = rememberSaveable { mutableIntStateOf(0) }

    val navigator = remember {
        Navigator(
            NavigationState(
                subStacks = linkedMapOf<NavKey, MutableList<NavKey>>(
                    HomeNavKey to homeStack,
                    SearchNavKey to searchStack,
                    WatchlistNavKey to watchlistStack,
                ),
                currentIndex = currentIndex,
            ),
        )
    }

    val backStack = navigator.state.backStack
    val showBottomBar = backStack.last() in navigator.state.topLevelKeys

    // NavDisplay only dispatches back when its own stack has more than one entry, so at a tab
    // root it lets back fall through and finish the activity. Handle exactly that case here:
    // back from a non-first tab returns to the first tab; from the first tab it exits, as it should.
    BackHandler(
        enabled = backStack.size == 1 &&
            navigator.state.currentTopLevelKey != navigator.state.topLevelKeys.first(),
    ) {
        navigator.goBack()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MoviesBottomBar(
                    currentTopLevelKey = navigator.state.currentTopLevelKey,
                    onItemClick = navigator::navigate,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.goBack() },
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                // Replaced one at a time by slices 3-6.
                entry<HomeNavKey> { Placeholder("Home") }
                entry<SearchNavKey> { Placeholder("Search") }
                entry<WatchlistNavKey> { Placeholder("Watch List") }
                entry<DetailNavKey> { key -> Placeholder("Detail ${key.movieId}") }
            },
        )
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

@Composable
private fun MoviesBottomBar(
    currentTopLevelKey: NavKey,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            TopLevelNavItem.entries.forEach { item ->
                val selected = item.key == currentTopLevelKey
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_${item.name.lowercase()}"),
                    icon = {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = item.label,
                        )
                    },
                    label = { Text(item.label) },
                    selected = selected,
                    onClick = { onItemClick(item.key) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}
```

- [ ] **Step 8: Write `MainActivity.kt`**

```kotlin
package com.practice.thenewmovies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.ui.MoviesApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MoviesTheme {
                MoviesApp()
            }
        }
    }
}
```

- [ ] **Step 9: Build the app**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. If `rememberNavBackStack(HomeNavKey)` fails to resolve, the Android-only overload is missing — add `implementation(libs.androidx.navigation3.runtime)` to `app/build.gradle.kts` explicitly instead of relying on the transitive dependency from `:core:navigation`.

- [ ] **Step 10: Install and check by hand**

Run: `./gradlew :app:installDebug`
Then launch the app and verify:
1. Dark background, Montserrat text, three tabs at the bottom.
2. Tapping Search then Watch List then Home switches the visible placeholder.
3. Pressing system back from Watch List returns to Home rather than exiting.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply
git add app
git commit -m "feat(app): add app shell with theme, bottom bar and NavDisplay"
```

---

### Task 6: Verify the slice

- [ ] **Step 1: Full build and test**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`, `NavigatorTest` 7 tests passed.

- [ ] **Step 2: Formatting check**

Run: `./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`

---

## Done when

- The app installs and runs with the real theme.
- Bottom-bar tabs switch, each tab keeps its own history, and system back behaves per the `NavigatorTest` cases.
- `core:designsystem` has no dependency on any model or data module (check with `./gradlew :core:designsystem:dependencies --configuration debugCompileClasspath`).
- Every top-level and detail `NavKey` lives in a feature `api` module, not in `:app`.
