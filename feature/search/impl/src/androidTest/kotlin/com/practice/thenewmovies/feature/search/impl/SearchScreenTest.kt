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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.testing.data.testMovies
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun loadStates(refresh: LoadState) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(true),
        append = LoadState.NotLoading(true),
    )

    private fun setScreen(
        query: String,
        pagingData: PagingData<Movie>,
        onMovieClick: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoviesTheme {
                SearchScreen(
                    query = query,
                    searchResults = flowOf(pagingData).collectAsLazyPagingItems(),
                    onQueryChange = {},
                    onBackClick = {},
                    onMovieClick = onMovieClick,
                )
            }
        }
    }

    @Test
    fun blankQuery_showsEmptyState() {
        setScreen(query = "", pagingData = PagingData.empty())

        composeTestRule
            .onNodeWithText("We are sorry, we can not find the movie :(")
            .assertIsDisplayed()
    }

    @Test
    fun results_areRendered() {
        setScreen(query = "dune", pagingData = PagingData.from(testMovies))

        composeTestRule.onNodeWithTag("search_results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dune").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arrival").assertIsDisplayed()
        // Rating and release year come from the row's meta slot.
        composeTestRule.onNodeWithText("7.8").assertIsDisplayed()
        composeTestRule.onNodeWithText("2021").assertIsDisplayed()
    }

    @Test
    fun tappingAResult_reportsTheMovieId() {
        var clicked: Int? = null
        setScreen(
            query = "dune",
            pagingData = PagingData.from(testMovies),
            onMovieClick = { clicked = it },
        )

        composeTestRule.onNodeWithText("Dune").performClick()

        assert(clicked == testMovies.first().id) { "expected ${testMovies.first().id}, got $clicked" }
    }

    @Test
    fun emptyResultsForANonBlankQuery_showsEmptyState() {
        // Explicit terminal load states: a bare PagingData.empty() reports refresh = Loading,
        // which correctly renders the spinner rather than the empty state.
        setScreen(
            query = "zzzz",
            pagingData = PagingData.empty(
                sourceLoadStates = loadStates(LoadState.NotLoading(endOfPaginationReached = true)),
            ),
        )

        composeTestRule
            .onNodeWithText("We are sorry, we can not find the movie :(")
            .assertIsDisplayed()
    }

    @Test
    fun aFreshQuery_showsTheSpinner() {
        setScreen(query = "dune", pagingData = PagingData.empty())

        composeTestRule.onNodeWithTag("search_results").assertDoesNotExist()
    }

    @Test
    fun refreshError_showsRetry() {
        setScreen(
            query = "dune",
            pagingData = PagingData.empty(
                sourceLoadStates = loadStates(LoadState.Error(IOException("no network"))),
            ),
        )

        composeTestRule.onNodeWithText("no network").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
