/*
 * Copyright 2026 TheNewMovies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.practice.thenewmovies.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.practice.thenewmovies.core.model.SessionState
import com.practice.thenewmovies.core.navigation.NavigationState
import com.practice.thenewmovies.core.navigation.Navigator
import com.practice.thenewmovies.feature.detail.impl.navigation.detailEntry
import com.practice.thenewmovies.feature.home.impl.navigation.homeEntry
import com.practice.thenewmovies.feature.search.impl.navigation.searchEntry
import com.practice.thenewmovies.feature.watchlist.impl.navigation.watchlistEntry
import com.practice.thenewmovies.navigation.TopLevelNavItem

@Composable
fun MoviesApp(viewModel: AppViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        SessionState.Loading -> LoadingScreen()
        SessionState.SignedOut -> AuthHost()
        is SessionState.SignedIn -> SignedInApp(onSignOut = viewModel::signOut)
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SignedInApp(onSignOut: () -> Unit) {
    val currentIndex = rememberSaveable { mutableIntStateOf(0) }

    // TopLevelNavItem is the single source of tab identity and order: the bottom bar renders it
    // and the back stacks are derived from it. Declaring the stacks separately let the two lists
    // drift, and a bar item with no matching sub-stack crashes NavigationState.goToTopLevel.
    val subStacks = linkedMapOf<NavKey, MutableList<NavKey>>()
    TopLevelNavItem.entries.forEach { item ->
        subStacks[item.key] = key(item) { rememberNavBackStack(item.key) }
    }

    val navigator = remember {
        Navigator(NavigationState(subStacks = subStacks, currentIndex = currentIndex))
    }

    val backStack = navigator.state.backStack
    val showNavigation = backStack.last() in navigator.state.topLevelKeys

    // NavDisplay only dispatches back when its own stack has more than one entry, so at a tab
    // root it lets back fall through and finish the activity. Handle exactly that case here:
    // back from a non-first tab returns to the first tab; from the first tab it exits, as it should.
    BackHandler(
        enabled = backStack.size == 1 &&
            navigator.state.currentTopLevelKey != navigator.state.topLevelKeys.first(),
    ) {
        navigator.goBack()
    }

    // NavigationSuiteScaffold picks the navigation container from the window size: a bottom bar
    // on a phone, a rail on a tablet or an unfolded foldable. The state hides it on Detail, which
    // is what the old `showBottomBar` branch did.
    // Item colours are read here, not inside navigationSuiteItems: that lambda is a plain
    // builder scope, not a @Composable one, so MaterialTheme is unreadable from inside it.
    val itemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.secondary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.secondary,
            indicatorColor = Color.Transparent,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.secondary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.secondary,
            indicatorColor = Color.Transparent,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    )
    val navigationState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(showNavigation) {
        if (showNavigation) navigationState.show() else navigationState.hide()
    }

    NavigationSuiteScaffold(
        state = navigationState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationSuiteItems = {
            TopLevelNavItem.entries.forEach { navItem ->
                item(
                    modifier = Modifier.testTag("nav_${navItem.name.lowercase()}"),
                    selected = navItem.key == navigator.state.currentTopLevelKey,
                    onClick = { navigator.navigate(navItem.key) },
                    icon = {
                        Icon(
                            painter = painterResource(navItem.icon),
                            contentDescription = navItem.label,
                        )
                    },
                    label = { Text(navItem.label) },
                    colors = itemColors,
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                homeEntry(navigator)
                searchEntry(navigator)
                watchlistEntry(navigator, onSignOut)
                detailEntry(navigator)
            },
        )
    }
}
