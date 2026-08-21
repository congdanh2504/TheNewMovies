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

import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestWatchlistRepository : WatchlistRepository {

    private val movies = MutableStateFlow(emptyList<WatchlistMovie>())

    override fun getWatchlist(): Flow<List<WatchlistMovie>> = movies

    override fun isInWatchlist(movieId: Int): Flow<Boolean> =
        movies.map { list -> list.any { it.id == movieId } }

    override fun getRating(movieId: Int): Flow<Float?> =
        movies.map { list -> list.firstOrNull { it.id == movieId }?.userRating }

    override suspend fun addToWatchlist(movie: WatchlistMovie) {
        movies.value = movies.value.filterNot { it.id == movie.id } + movie
    }

    override suspend fun removeFromWatchlist(movieId: Int) {
        movies.value = movies.value.filterNot { it.id == movieId }
    }

    override suspend fun setRating(movieId: Int, rating: Float) {
        movies.value = movies.value.map {
            if (it.id == movieId) it.copy(userRating = rating) else it
        }
    }

    fun emit(value: List<WatchlistMovie>) {
        movies.value = value
    }
}
