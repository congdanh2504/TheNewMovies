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
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDetailDao {

    @Upsert
    suspend fun upsert(detail: MovieDetailEntity)

    @Query("SELECT * FROM movie_details WHERE id = :movieId")
    fun getById(movieId: Int): Flow<MovieDetailEntity?>

    @Query("SELECT syncedAt FROM movie_details WHERE id = :movieId")
    suspend fun getSyncedAt(movieId: Int): Long?
}
