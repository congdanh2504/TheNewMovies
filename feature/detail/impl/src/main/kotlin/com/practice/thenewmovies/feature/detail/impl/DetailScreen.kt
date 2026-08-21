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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.ui.component.MetaLabel
import com.practice.thenewmovies.core.ui.component.MoviePoster
import com.practice.thenewmovies.core.ui.component.OfflineBanner
import com.practice.thenewmovies.core.ui.component.RatingChip

private val MetaGrey = Color(0xFF92929D)
private val PlaceholderGrey = Color(0xFF3A3F47)
private val SheetTextColor = Color(0xFF4E4B66)

@Composable
internal fun DetailScreen(
    movieId: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(movieId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val showRatingSheet by viewModel.showRatingSheet.collectAsStateWithLifecycle()

    DetailScreen(
        uiState = uiState,
        isOffline = isOffline,
        showRatingSheet = showRatingSheet,
        onBackClick = onBackClick,
        onBookmarkClick = viewModel::toggleWatchlist,
        onRateClick = viewModel::onRateClick,
        onRatingSheetDismissed = viewModel::onRatingSheetDismissed,
        onRatingSubmitted = viewModel::submitRating,
        onRetryClick = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun DetailScreen(
    uiState: DetailUiState,
    isOffline: Boolean,
    showRatingSheet: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onRateClick: () -> Unit,
    onRatingSheetDismissed: () -> Unit,
    onRatingSubmitted: (Float) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (uiState) {
            DetailUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )

            is DetailUiState.Error -> ErrorState(
                message = uiState.message,
                onRetryClick = onRetryClick,
                modifier = Modifier.align(Alignment.Center),
            )

            is DetailUiState.Success -> {
                DetailContent(
                    state = uiState,
                    isOffline = isOffline,
                    onRateClick = onRateClick,
                )
                if (showRatingSheet) {
                    RatingSheet(
                        initialRating = uiState.userRating ?: uiState.detail.voteAverage.toFloat(),
                        onDismiss = onRatingSheetDismissed,
                        onConfirm = onRatingSubmitted,
                    )
                }
            }
        }

        // The toolbar stays available in every state, so a failed load is still escapable.
        DetailToolbar(
            isInWatchlist = (uiState as? DetailUiState.Success)?.isInWatchlist == true,
            showBookmark = uiState is DetailUiState.Success,
            onBackClick = onBackClick,
            onBookmarkClick = onBookmarkClick,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
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

@Composable
private fun DetailContent(
    state: DetailUiState.Success,
    isOffline: Boolean,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (isOffline) OfflineBanner()

        DetailHeader(state = state, onRateClick = onRateClick)

        Spacer(modifier = Modifier.height(16.dp))
        DetailMetaRow(state = state)

        Spacer(modifier = Modifier.height(24.dp))
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            },
            divider = {},
        ) {
            detailTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color.White else MetaGrey,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            when (selectedTab) {
                0 -> AboutTab(overview = state.detail.overview.orEmpty())
                1 -> ReviewsTab(reviews = state.reviews)
                else -> CastTab(cast = state.cast)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun DetailHeader(
    state: DetailUiState.Success,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(271.dp),
    ) {
        AsyncImage(
            model = detail.backdropPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x44000000), MaterialTheme.colorScheme.surface),
                    ),
                ),
        )
        RatingChip(
            rating = state.userRating ?: detail.voteAverage.toFloat(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 148.dp, end = 16.dp)
                .clickable(onClick = onRateClick),
        )
        MoviePoster(
            posterPath = detail.posterPath,
            contentDescription = detail.title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 29.dp)
                .size(width = 95.dp, height = 120.dp),
        )
        Text(
            text = detail.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 136.dp, end = 29.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun DetailMetaRow(state: DetailUiState.Success, modifier: Modifier = Modifier) {
    val detail = state.detail
    Row(
        modifier = modifier.padding(start = 29.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaLabel(icon = Icons.Outlined.CalendarToday, label = detail.releaseDate.take(4))
        Text(text = "|", color = MetaGrey, style = MaterialTheme.typography.labelMedium)
        MetaLabel(
            icon = Icons.Outlined.AccessTime,
            label = "${detail.runtime ?: 0} Minutes",
        )
        Text(text = "|", color = MetaGrey, style = MaterialTheme.typography.labelMedium)
        MetaLabel(
            icon = Icons.Outlined.LocalActivity,
            label = detail.genres.firstOrNull()?.name.orEmpty(),
        )
    }
}

@Composable
private fun DetailToolbar(
    isInWatchlist: Boolean,
    showBookmark: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
        Text(
            text = "Detail",
            color = Color(0xFFECECEC),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        if (showBookmark) {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (isInWatchlist) {
                        Icons.Default.Bookmark
                    } else {
                        Icons.Default.BookmarkBorder
                    },
                    contentDescription = if (isInWatchlist) {
                        "Remove from watch list"
                    } else {
                        "Add to watch list"
                    },
                    tint = if (isInWatchlist) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun AboutTab(overview: String, modifier: Modifier = Modifier) {
    Text(
        text = overview,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

@Composable
private fun ReviewsTab(reviews: List<Review>, modifier: Modifier = Modifier) {
    if (reviews.isEmpty()) {
        Text(
            text = "No reviews yet.",
            color = MetaGrey,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        reviews.forEach { review -> ReviewCard(review = review) }
    }
}

@Composable
private fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            AsyncImage(
                model = review.avatarPath,
                contentDescription = review.author,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PlaceholderGrey),
                contentScale = ContentScale.Crop,
            )
            review.rating?.let { rating ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.1f".format(rating),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = review.author,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.content,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CastTab(cast: List<Cast>, modifier: Modifier = Modifier) {
    if (cast.isEmpty()) {
        Text(
            text = "No cast information.",
            color = MetaGrey,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        cast.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowItems.forEach { member ->
                    CastCard(cast = member, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CastCard(cast: Cast, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = cast.profilePath,
            contentDescription = cast.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PlaceholderGrey),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cast.name,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingSheet(
    initialRating: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var rating by remember { mutableFloatStateOf(initialRating.coerceIn(0f, 10f)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Rate this movie",
                color = SheetTextColor,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "%.1f".format(rating),
                color = SheetTextColor,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Skip for now",
                    color = SheetTextColor,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onConfirm(rating) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OK",
                    color = Color(0xFFFCFCFC),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
private fun DetailScreenPreview() {
    MoviesTheme {
        DetailScreen(
            uiState = DetailUiState.Success(
                detail = MovieDetail(
                    id = 1,
                    title = "Dune",
                    originalTitle = "Dune",
                    originalLanguage = "en",
                    overview = "A noble family becomes embroiled in a war for a desert planet.",
                    genres = listOf(Genre(id = 878, name = "Sci-Fi")),
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2021-10-22",
                    runtime = 155,
                    status = "Released",
                    video = false,
                    voteAverage = 7.8,
                    voteCount = 1000,
                ),
                cast = emptyList(),
                reviews = emptyList(),
                isInWatchlist = false,
                userRating = null,
            ),
            isOffline = false,
            showRatingSheet = false,
            onBackClick = {},
            onBookmarkClick = {},
            onRateClick = {},
            onRatingSheetDismissed = {},
            onRatingSubmitted = {},
            onRetryClick = {},
        )
    }
}
