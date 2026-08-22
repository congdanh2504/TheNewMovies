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

import app.cash.turbine.test
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testWatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val watchlistRepository = TestWatchlistRepository()

    private fun viewModel() = WatchlistViewModel(watchlistRepository)

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(WatchlistUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `reports empty when nothing is saved`() = runTest {
        viewModel().uiState.test {
            assertEquals(WatchlistUiState.Empty, expectMostRecentItem())
        }
    }

    @Test
    fun `emits saved movies`() = runTest {
        watchlistRepository.emit(listOf(testWatchlistMovie))

        viewModel().uiState.test {
            assertEquals(
                WatchlistUiState.Success(listOf(testWatchlistMovie)),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `returns to empty when the last movie is removed`() = runTest {
        watchlistRepository.emit(listOf(testWatchlistMovie))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(
                WatchlistUiState.Success(listOf(testWatchlistMovie)),
                expectMostRecentItem(),
            )

            watchlistRepository.emit(emptyList())

            assertEquals(WatchlistUiState.Empty, awaitItem())
        }
    }
}
