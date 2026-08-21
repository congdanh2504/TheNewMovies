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
package com.practice.thenewmovies.feature.search.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.practice.thenewmovies.core.designsystem.component.MoviesSearchBar
import com.practice.thenewmovies.core.designsystem.component.MoviesTopAppBar
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.ui.component.MetaLabel
import com.practice.thenewmovies.core.ui.component.MovieRow
import com.practice.thenewmovies.core.ui.component.MoviesEmptyState
import com.practice.thenewmovies.core.ui.component.RatingChip

@Composable
internal fun SearchScreen(
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()

    SearchScreen(
        query = query,
        searchResults = searchResults,
        onQueryChange = viewModel::onQueryChange,
        onBackClick = onBackClick,
        onMovieClick = onMovieClick,
        modifier = modifier,
    )
}

@Composable
internal fun SearchScreen(
    query: String,
    searchResults: LazyPagingItems<Movie>,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MoviesTopAppBar(
            title = "Search",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        MoviesSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("search_field"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val refreshState = searchResults.loadState.refresh

        when {
            query.isBlank() -> NoResultsState()

            refreshState is LoadState.Loading && searchResults.itemCount == 0 -> LoadingState()

            refreshState is LoadState.Error -> ErrorState(
                message = refreshState.error.message ?: "Something went wrong",
                onRetry = searchResults::retry,
            )

            searchResults.itemCount == 0 -> NoResultsState()

            else -> SearchResultsList(
                searchResults = searchResults,
                onMovieClick = onMovieClick,
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    searchResults: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_results"),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(
            count = searchResults.itemCount,
            key = searchResults.itemKey { it.id },
        ) { index ->
            val movie = searchResults[index] ?: return@items
            MovieRow(
                posterPath = movie.posterPath,
                title = movie.title,
                onClick = { onMovieClick(movie.id) },
            ) {
                RatingChip(rating = movie.voteAverage.toFloat())
                Spacer(modifier = Modifier.height(4.dp))
                MetaLabel(
                    icon = Icons.Outlined.CalendarMonth,
                    label = movie.releaseDate.take(4).ifEmpty { "N/A" },
                )
            }
        }

        item {
            when (val appendState = searchResults.loadState.append) {
                is LoadState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is LoadState.Error -> ErrorState(
                    message = appendState.error.message ?: "Load failed",
                    onRetry = searchResults::retry,
                )

                else -> Unit
            }
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
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    MoviesEmptyState(
        icon = Icons.Outlined.SearchOff,
        title = "We are sorry, we can not find the movie :(",
        subtitle = "Find your movie by Type title, categories, years, etc",
        modifier = modifier.padding(horizontal = 40.dp),
    )
}

@Preview
@Composable
private fun NoResultsStatePreview() {
    MoviesTheme { NoResultsState() }
}
