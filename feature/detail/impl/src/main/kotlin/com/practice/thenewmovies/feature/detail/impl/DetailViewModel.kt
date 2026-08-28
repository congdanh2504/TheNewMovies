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
package com.practice.thenewmovies.feature.detail.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.connectivity.NetworkMonitor
import com.practice.thenewmovies.core.data.movies.MovieDetailRepository
import com.practice.thenewmovies.core.data.watchlist.WatchlistRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val LOAD_FAILED_MESSAGE = "Could not load this movie"

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
internal class DetailViewModel @AssistedInject constructor(
    @param:Assisted private val movieId: Int,
    getMovieDetail: GetMovieDetailUseCase,
    private val moviesRepository: MovieDetailRepository,
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
