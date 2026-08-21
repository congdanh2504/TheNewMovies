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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OFFLINE_MESSAGE = "No data available offline"
private const val LOAD_FAILED_MESSAGE = "Couldn't load movies. Check your connection."

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val moviesRepository: MoviesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val refreshFailed = MutableStateFlow(false)

    private val movies = combine(
        moviesRepository.getMovies(MovieCategory.NOW_PLAYING),
        moviesRepository.getMovies(MovieCategory.UPCOMING),
        moviesRepository.getMovies(MovieCategory.TOP_RATED),
        moviesRepository.getMovies(MovieCategory.POPULAR),
    ) { nowPlaying, upcoming, topRated, popular ->
        HomeUiState.Success(
            nowPlaying = nowPlaying,
            upcoming = upcoming,
            topRated = topRated,
            popular = popular,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        movies,
        networkMonitor.isOnline,
        refreshFailed,
    ) { loaded, isOnline, failed ->
        val hasMovies = listOf(loaded.nowPlaying, loaded.upcoming, loaded.topRated, loaded.popular)
            .any { it.isNotEmpty() }
        when {
            hasMovies -> loaded

            !isOnline -> HomeUiState.Error(OFFLINE_MESSAGE)

            // Online but every refresh failed and nothing is cached: without this the screen
            // spins forever whenever the API is unreachable.
            failed -> HomeUiState.Error(LOAD_FAILED_MESSAGE)

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
        viewModelScope.launch {
            val results = MovieCategory.entries.map { moviesRepository.refresh(it) }
            refreshFailed.value = results.none { it }
        }
    }
}
