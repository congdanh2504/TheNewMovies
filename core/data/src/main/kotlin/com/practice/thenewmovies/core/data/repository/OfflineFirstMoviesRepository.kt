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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.practice.thenewmovies.core.common.network.Dispatcher
import com.practice.thenewmovies.core.common.network.MoviesDispatchers
import com.practice.thenewmovies.core.data.model.asEntity
import com.practice.thenewmovies.core.data.paging.MoviePagingSource
import com.practice.thenewmovies.core.data.util.Clock
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val TTL_MS = 24L * 60 * 60 * 1000
private const val SEARCH_PAGE_SIZE = 20

@Singleton
internal class OfflineFirstMoviesRepository @Inject constructor(
    private val network: MoviesNetworkDataSource,
    private val movieDao: MovieDao,
    private val movieDetailDao: MovieDetailDao,
    private val castDao: CastDao,
    private val reviewDao: ReviewDao,
    private val clock: Clock,
    @Dispatcher(MoviesDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : MoviesRepository {

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        movieDao.getByCategory(category.name)
            .map { entities -> entities.map { entity -> entity.asExternalModel() } }

    override fun getMovieDetail(movieId: Int): Flow<MovieDetail?> =
        movieDetailDao.getById(movieId).map { it?.asExternalModel() }

    override fun getCast(movieId: Int): Flow<List<Cast>> =
        castDao.getByMovieId(movieId).map { entities -> entities.map { it.asExternalModel() } }

    override fun getReviews(movieId: Int): Flow<List<Review>> =
        reviewDao.getByMovieId(movieId).map { entities -> entities.map { it.asExternalModel() } }

    override fun searchMoviesPaged(query: String): Flow<PagingData<Movie>> =
        Pager(PagingConfig(pageSize = SEARCH_PAGE_SIZE, enablePlaceholders = false)) {
            MoviePagingSource(network = network, query = query)
        }.flow

    override suspend fun refresh(category: MovieCategory): Boolean = withContext(ioDispatcher) {
        if (isFresh(movieDao.getSyncedAt(category.name))) return@withContext true

        runCatching {
            val now = clock.nowMillis()
            val movies = network.getMovies(category)
            movieDao.replaceCategory(
                category = category.name,
                movies = movies.map { it.asEntity(category = category, syncedAt = now) },
            )
        }.isSuccess
    }

    override suspend fun refreshDetail(movieId: Int): Boolean = withContext(ioDispatcher) {
        if (isFresh(movieDetailDao.getSyncedAt(movieId))) return@withContext true

        runCatching {
            val now = clock.nowMillis()
            val detail = network.getMovieDetail(movieId)
            val cast = network.getCast(movieId)
            val reviews = network.getReviews(movieId)

            movieDetailDao.upsert(detail.asEntity(syncedAt = now))
            castDao.upsertAll(cast.map { it.asEntity(movieId) })
            reviewDao.upsertAll(reviews.map { it.asEntity(movieId) })
        }.isSuccess
    }

    private fun isFresh(syncedAt: Long?): Boolean =
        syncedAt != null && clock.nowMillis() - syncedAt < TTL_MS
}
