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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesTopAppBar
import com.practice.thenewmovies.core.designsystem.icon.MoviesIcons
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
        onSignOutClick = viewModel::onSignOutClick,
        modifier = modifier,
    )
}

@Composable
internal fun WatchlistScreen(
    uiState: WatchlistUiState,
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        MoviesTopAppBar(
            title = "Watch list",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
            actionIcon = MoviesIcons.Logout,
            onActionClick = onSignOutClick,
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
            .padding(horizontal = 24.dp)
            .testTag("watchlist"),
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

@Preview
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
            onSignOutClick = {},
        )
    }
}

@Preview
@Composable
private fun WatchlistScreenEmptyPreview() {
    MoviesTheme {
        WatchlistScreen(
            uiState = WatchlistUiState.Empty,
            onBackClick = {},
            onMovieClick = {},
            onSignOutClick = {},
        )
    }
}
