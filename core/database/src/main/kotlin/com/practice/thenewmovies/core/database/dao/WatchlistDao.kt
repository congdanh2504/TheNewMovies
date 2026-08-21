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
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Upsert
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE movieId = :movieId")
    suspend fun deleteById(movieId: Int)

    @Query("SELECT * FROM watchlist ORDER BY title ASC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId)")
    fun existsById(movieId: Int): Flow<Boolean>

    @Query("UPDATE watchlist SET userRating = :rating WHERE movieId = :movieId")
    suspend fun updateRating(movieId: Int, rating: Float)

    @Query("SELECT userRating FROM watchlist WHERE movieId = :movieId")
    fun getRating(movieId: Int): Flow<Float?>
}
