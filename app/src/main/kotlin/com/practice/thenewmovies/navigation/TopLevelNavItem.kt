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
