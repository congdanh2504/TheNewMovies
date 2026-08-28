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

import app.cash.turbine.test
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.data.testMovies
import com.practice.thenewmovies.core.testing.repository.TestMovieListRepository
import com.practice.thenewmovies.core.testing.repository.TestNetworkMonitor
import com.practice.thenewmovies.core.testing.repository.TestUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMovieListRepository()
    private val networkMonitor = TestNetworkMonitor()
    private val userPreferences = TestUserPreferencesRepository()

    private fun viewModel() = HomeViewModel(
        moviesRepository = moviesRepository,
        userPreferencesRepository = userPreferences,
        networkMonitor = networkMonitor,
    )

    @Test
    fun `starts in the loading state`() = runTest {
        assertEquals(HomeUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `refreshes every category on creation`() = runTest {
        viewModel()

        assertEquals(MovieCategory.entries.toSet(), moviesRepository.refreshedCategories.toSet())
    }

    @Test
    fun `emits success once any category has movies`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            moviesRepository.emitMovies(MovieCategory.POPULAR, testMovies)

            val state = expectMostRecentItem()
            assertTrue(state is HomeUiState.Success)
            assertEquals(testMovies, (state as HomeUiState.Success).popular)
        }
    }

    @Test
    fun `reports an error when offline with nothing cached`() = runTest {
        networkMonitor.setOnline(false)
        moviesRepository.refreshSucceeds = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Error("No data available offline"), expectMostRecentItem())
        }
    }

    @Test
    fun `shows cached movies when offline`() = runTest {
        moviesRepository.emitMovies(MovieCategory.TOP_RATED, testMovies)
        networkMonitor.setOnline(false)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is HomeUiState.Success)
        }
    }

    @Test
    fun `reports an error when online but every refresh fails`() = runTest {
        moviesRepository.refreshSucceeds = false
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(
                HomeUiState.Error("Couldn't load movies. Check your connection."),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `stays in loading while a refresh is still succeeding`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, expectMostRecentItem())
        }
    }

    @Test
    fun `exposes offline state`() = runTest {
        val viewModel = viewModel()

        viewModel.isOffline.test {
            assertEquals(false, awaitItem())
            networkMonitor.setOnline(false)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `selecting a tab persists it`() = runTest {
        val viewModel = viewModel()

        viewModel.selectedTabIndex.test {
            assertEquals(0, awaitItem())
            viewModel.onTabSelected(2)
            assertEquals(2, awaitItem())
        }
    }
}
