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

import app.cash.turbine.test
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestMoviesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val moviesRepository = TestMoviesRepository()

    private fun viewModel() = SearchViewModel(moviesRepository)

    @Test
    fun `starts with an empty query`() = runTest {
        assertEquals("", viewModel().query.value)
    }

    @Test
    fun `query updates immediately`() = runTest {
        val viewModel = viewModel()

        viewModel.query.test {
            assertEquals("", awaitItem())
            viewModel.onQueryChange("dune")
            assertEquals("dune", awaitItem())
        }
    }

    @Test
    fun `clearing the query resets it`() = runTest {
        val viewModel = viewModel()
        viewModel.onQueryChange("dune")

        viewModel.onQueryChange("")

        assertEquals("", viewModel.query.value)
    }
}
