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
