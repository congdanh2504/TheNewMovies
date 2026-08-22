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
package com.practice.thenewmovies.core.database.di

import android.content.Context
import androidx.room.Room
import com.practice.thenewmovies.core.database.MoviesDatabase
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesMoviesDatabase(@ApplicationContext context: Context): MoviesDatabase =
        Room.databaseBuilder(context, MoviesDatabase::class.java, "movies.db")
            // The app is unreleased and every pre-auth watchlist row belongs to no user, so
            // there is nothing worth migrating.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providesMovieDao(database: MoviesDatabase): MovieDao = database.movieDao()

    @Provides
    fun providesMovieDetailDao(database: MoviesDatabase): MovieDetailDao = database.movieDetailDao()

    @Provides
    fun providesCastDao(database: MoviesDatabase): CastDao = database.castDao()

    @Provides
    fun providesReviewDao(database: MoviesDatabase): ReviewDao = database.reviewDao()

    @Provides
    fun providesWatchlistDao(database: MoviesDatabase): WatchlistDao = database.watchlistDao()
}
