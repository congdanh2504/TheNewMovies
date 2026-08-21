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
