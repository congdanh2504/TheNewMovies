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
package com.practice.thenewmovies.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.entity.CastEntity
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.database.entity.ReviewEntity
import com.practice.thenewmovies.core.database.entity.WatchlistEntity

@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        CastEntity::class,
        ReviewEntity::class,
        WatchlistEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MoviesDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun castDao(): CastDao
    abstract fun reviewDao(): ReviewDao
    abstract fun watchlistDao(): WatchlistDao
}
