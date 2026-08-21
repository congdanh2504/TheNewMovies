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
package com.practice.thenewmovies.core.data.repository

import androidx.paging.PagingData
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * Reads always come from local storage, so every screen works offline. Network access happens
 * only through the `refresh` functions, which no-op inside the sync TTL.
 */
interface MoviesRepository {

    fun getMovies(category: MovieCategory): Flow<List<Movie>>

    fun getMovieDetail(movieId: Int): Flow<MovieDetail?>

    fun getCast(movieId: Int): Flow<List<Cast>>

    fun getReviews(movieId: Int): Flow<List<Review>>

    /** Network-backed and not persisted: page numbering belongs to the server. */
    fun searchMoviesPaged(query: String): Flow<PagingData<Movie>>

    /** Returns false when the network call failed; cached data is still readable. */
    suspend fun refresh(category: MovieCategory): Boolean

    suspend fun refreshDetail(movieId: Int): Boolean
}
