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

import androidx.paging.PagingData
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class TestMoviesRepository : MoviesRepository {

    private val moviesByCategory =
        MovieCategory.entries.associateWith { MutableStateFlow(emptyList<Movie>()) }
    private val detail = MutableStateFlow<MovieDetail?>(null)
    private val cast = MutableStateFlow(emptyList<Cast>())
    private val reviews = MutableStateFlow(emptyList<Review>())
    private val searchResults = MutableStateFlow(PagingData.empty<Movie>())

    var refreshSucceeds: Boolean = true
    val refreshedCategories = mutableListOf<MovieCategory>()
    val refreshedDetailIds = mutableListOf<Int>()

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        moviesByCategory.getValue(category).asStateFlow()

    override fun getMovieDetail(movieId: Int): Flow<MovieDetail?> = detail.asStateFlow()

    override fun getCast(movieId: Int): Flow<List<Cast>> = cast.asStateFlow()

    override fun getReviews(movieId: Int): Flow<List<Review>> = reviews.asStateFlow()

    override fun searchMoviesPaged(query: String): Flow<PagingData<Movie>> =
        flowOf(searchResults.value)

    override suspend fun refresh(category: MovieCategory): Boolean {
        refreshedCategories += category
        return refreshSucceeds
    }

    override suspend fun refreshDetail(movieId: Int): Boolean {
        refreshedDetailIds += movieId
        return refreshSucceeds
    }

    fun emitMovies(category: MovieCategory, movies: List<Movie>) {
        moviesByCategory.getValue(category).value = movies
    }

    fun emitDetail(movieDetail: MovieDetail?) {
        detail.value = movieDetail
    }

    fun emitCast(value: List<Cast>) {
        cast.value = value
    }

    fun emitReviews(value: List<Review>) {
        reviews.value = value
    }

    fun emitSearchResults(movies: List<Movie>) {
        searchResults.value = PagingData.from(movies)
    }
}
