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
package com.practice.thenewmovies.core.data.di

import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
import com.practice.thenewmovies.core.data.repository.MovieListRepository
import com.practice.thenewmovies.core.data.repository.MovieSearchRepository
import com.practice.thenewmovies.core.data.repository.OfflineFirstMoviesRepository
import com.practice.thenewmovies.core.data.util.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    internal abstract fun bindsMovieListRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieListRepository

    @Binds
    internal abstract fun bindsMovieDetailRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieDetailRepository

    @Binds
    internal abstract fun bindsMovieSearchRepository(
        repository: OfflineFirstMoviesRepository,
    ): MovieSearchRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {

    @Provides
    fun providesClock(): Clock = Clock { System.currentTimeMillis() }
}
