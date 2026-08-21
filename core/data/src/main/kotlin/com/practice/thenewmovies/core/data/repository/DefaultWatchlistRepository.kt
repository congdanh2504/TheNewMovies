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

import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.model.asEntity
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
) : WatchlistRepository {

    override fun getWatchlist(): Flow<List<WatchlistMovie>> =
        watchlistDao.getAll().map { entities -> entities.map { it.asExternalModel() } }

    override fun isInWatchlist(movieId: Int): Flow<Boolean> = watchlistDao.existsById(movieId)

    override fun getRating(movieId: Int): Flow<Float?> = watchlistDao.getRating(movieId)

    override suspend fun addToWatchlist(movie: WatchlistMovie) =
        watchlistDao.upsert(movie.asEntity())

    override suspend fun removeFromWatchlist(movieId: Int) = watchlistDao.deleteById(movieId)

    override suspend fun setRating(movieId: Int, rating: Float) =
        watchlistDao.updateRating(movieId = movieId, rating = rating)
}
