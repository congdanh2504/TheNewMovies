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

import app.cash.turbine.test
import com.practice.thenewmovies.core.domain.GetMovieDetailUseCase
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testCast
import com.practice.thenewmovies.core.testing.data.testMovieDetail
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import com.practice.thenewmovies.core.testing.repository.TestNetworkMonitor
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMoviesRepository()
    private val watchlistRepository = TestWatchlistRepository()
    private val networkMonitor = TestNetworkMonitor()

    private fun viewModel(movieId: Int = 1) = DetailViewModel(
        movieId = movieId,
        getMovieDetail = GetMovieDetailUseCase(moviesRepository, watchlistRepository),
        moviesRepository = moviesRepository,
        watchlistRepository = watchlistRepository,
        networkMonitor = networkMonitor,
    )

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(DetailUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `refreshes the detail on creation`() = runTest {
        viewModel(movieId = 7)

        assertEquals(listOf(7), moviesRepository.refreshedDetailIds)
    }

    @Test
    fun `emits success once the detail is cached`() = runTest {
        val viewModel = viewModel()
        moviesRepository.emitDetail(testMovieDetail)
        moviesRepository.emitCast(testCast)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DetailUiState.Success)
            assertEquals(testMovieDetail, (state as DetailUiState.Success).detail)
            assertEquals(testCast, state.cast)
        }
    }

    @Test
    fun `reports an error when the refresh fails with nothing cached`() = runTest {
        moviesRepository.refreshSucceeds = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(DetailUiState.Error("Could not load this movie"), expectMostRecentItem())
        }
    }

    @Test
    fun `keeps showing cached detail when a refresh fails`() = runTest {
        moviesRepository.refreshSucceeds = false
        moviesRepository.emitDetail(testMovieDetail)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is DetailUiState.Success)
        }
    }

    @Test
    fun `toggling the watchlist adds then removes the movie`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        val viewModel = viewModel(movieId = testMovieDetail.id)
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.toggleWatchlist()
        assertTrue(watchlistRepository.isInWatchlist(testMovieDetail.id).first())

        viewModel.toggleWatchlist()
        assertFalse(watchlistRepository.isInWatchlist(testMovieDetail.id).first())
    }

    @Test
    fun `submitting a rating saves the movie first`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        val viewModel = viewModel(movieId = testMovieDetail.id)
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.submitRating(4.5f)

        assertTrue(watchlistRepository.isInWatchlist(testMovieDetail.id).first())
        assertEquals(4.5f, watchlistRepository.getRating(testMovieDetail.id).first())
    }

    @Test
    fun `the rating sheet opens and closes`() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.showRatingSheet.value)
        viewModel.onRateClick()
        assertTrue(viewModel.showRatingSheet.value)
        viewModel.onRatingSheetDismissed()
        assertFalse(viewModel.showRatingSheet.value)
    }
}
