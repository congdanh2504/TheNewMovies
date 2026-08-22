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

import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getWatchlist(): Flow<List<WatchlistMovie>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    fun getRating(movieId: Int): Flow<Float?>
    suspend fun addToWatchlist(movie: WatchlistMovie)
    suspend fun removeFromWatchlist(movieId: Int)
    suspend fun setRating(movieId: Int, rating: Float)

    /** Pushes local changes, then pulls the server's rows. Called on sign-in. */
    suspend fun syncWatchlist()
}
