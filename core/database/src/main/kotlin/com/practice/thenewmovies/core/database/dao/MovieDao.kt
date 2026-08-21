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
import androidx.room.Transaction
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Upsert
    suspend fun upsertAll(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies WHERE category = :category")
    fun getByCategory(category: String): Flow<List<MovieEntity>>

    @Query("SELECT MAX(syncedAt) FROM movies WHERE category = :category")
    suspend fun getSyncedAt(category: String): Long?

    @Query("DELETE FROM movies WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Transaction
    suspend fun replaceCategory(category: String, movies: List<MovieEntity>) {
        deleteByCategory(category)
        upsertAll(movies)
    }
}
