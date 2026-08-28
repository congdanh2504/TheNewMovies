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

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * One movie's detail, cast and reviews. Reads come from local storage; [refreshDetail] is the
 * only network access and no-ops inside the sync TTL.
 */
interface MovieDetailRepository {

    fun getMovieDetail(movieId: Int): Flow<MovieDetail?>

    fun getCast(movieId: Int): Flow<List<Cast>>

    fun getReviews(movieId: Int): Flow<List<Review>>

    /** Returns false when the network call failed; cached data is still readable. */
    suspend fun refreshDetail(movieId: Int): Boolean
}
