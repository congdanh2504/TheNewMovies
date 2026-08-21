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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesSearchBar
import com.practice.thenewmovies.core.designsystem.theme.Montserrat
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.ui.DevicePreviews
import com.practice.thenewmovies.core.ui.component.MoviePoster
import com.practice.thenewmovies.core.ui.component.OfflineBanner

@Composable
internal fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isOffline = isOffline,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = viewModel::onTabSelected,
        onMovieClick = onMovieClick,
        onSearchClick = onSearchClick,
        onRetryClick = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    isOffline: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Text(
            text = "What do you want to watch?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        if (isOffline) OfflineBanner()

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(onClick = onSearchClick),
        ) {
            // The home search bar is a button: typing happens on the search screen.
            MoviesSearchBar(query = "", onQueryChange = {}, enabled = false)
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            HomeUiState.Loading -> LoadingState()

            is HomeUiState.Error -> ErrorState(
                message = uiState.message,
                onRetryClick = onRetryClick,
            )

            is HomeUiState.Success -> HomeContent(
                uiState = uiState,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
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
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryClick) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState.Success,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))
        FeaturedRow(movies = uiState.topRated, onMovieClick = onMovieClick)
        CategoryTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
        )
        Spacer(modifier = Modifier.height(16.dp))
        MovieGrid(
            movies = uiState.moviesForTab(selectedTabIndex),
            onMovieClick = onMovieClick,
        )
    }
}

@Composable
private fun FeaturedRow(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        itemsIndexed(items = movies, key = { _, movie -> movie.id }) { index, movie ->
            NumberedPoster(
                movie = movie,
                position = index + 1,
                onClick = { onMovieClick(movie.id) },
            )
        }
    }
}

@Composable
private fun NumberedPoster(
    movie: Movie,
    position: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .width(160.dp)
            .height(260.dp)
            .clickable(onClick = onClick),
    ) {
        MoviePoster(
            posterPath = movie.posterPath,
            contentDescription = movie.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, bottom = 42.dp),
        )
        StrokedText(
            text = "$position",
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun StrokedText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 96.sp,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = Color(0xFF242A32),
) {
    Box(modifier = modifier.wrapContentSize()) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = Montserrat,
            fontWeight = FontWeight.SemiBold,
            color = strokeColor,
            style = TextStyle(drawStyle = Stroke(width = 5f)),
        )
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = Montserrat,
            fontWeight = FontWeight.SemiBold,
            color = fillColor,
        )
    }
}

@Composable
private fun CategoryTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollableTabRow(
        modifier = modifier,
        containerColor = Color.Transparent,
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            Box(
                Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(3.dp)
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF3A3F47)),
            )
        },
        divider = {},
    ) {
        homeTabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) Color.White else Color.Gray,
                    )
                },
            )
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .height(500.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = movies, key = { it.id }) { movie ->
            MoviePoster(
                posterPath = movie.posterPath,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .testTag("movie_card")
                    .clickable { onMovieClick(movie.id) },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    val movies = List(6) { index ->
        Movie(
            id = index,
            title = "Movie $index",
            overview = "Overview",
            posterPath = null,
            backdropPath = null,
            releaseDate = "2024-01-01",
            voteAverage = 7.5,
            voteCount = 100,
        )
    }
    MoviesTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                nowPlaying = movies,
                upcoming = movies,
                topRated = movies,
                popular = movies,
            ),
            isOffline = false,
            selectedTabIndex = 0,
            onTabSelected = {},
            onMovieClick = {},
            onSearchClick = {},
            onRetryClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeScreenLoadingPreview() {
    MoviesTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            isOffline = false,
            selectedTabIndex = 0,
            onTabSelected = {},
            onMovieClick = {},
            onSearchClick = {},
            onRetryClick = {},
        )
    }
}
