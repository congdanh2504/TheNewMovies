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
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.movies.MovieListRepository
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestMovieListRepository : MovieListRepository {

    private val moviesByCategory =
        MovieCategory.entries.associateWith { MutableStateFlow(emptyList<Movie>()) }

    var refreshSucceeds: Boolean = true
    val refreshedCategories = mutableListOf<MovieCategory>()

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        moviesByCategory.getValue(category).asStateFlow()

    override suspend fun refresh(category: MovieCategory): Boolean {
        refreshedCategories += category
        return refreshSucceeds
    }

    fun emitMovies(category: MovieCategory, movies: List<Movie>) {
        moviesByCategory.getValue(category).value = movies
    }
}
