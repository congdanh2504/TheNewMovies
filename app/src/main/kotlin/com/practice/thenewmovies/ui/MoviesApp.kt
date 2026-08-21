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
import com.practice.thenewmovies.feature.detail.impl.navigation.detailEntry
import com.practice.thenewmovies.feature.home.api.HomeNavKey
import com.practice.thenewmovies.feature.home.impl.navigation.homeEntry
import com.practice.thenewmovies.feature.search.api.SearchNavKey
import com.practice.thenewmovies.feature.search.impl.navigation.searchEntry
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
                homeEntry(navigator)
                searchEntry(navigator)
                entry<WatchlistNavKey> { Placeholder("Watch List") }
                detailEntry(navigator)
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
