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

import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OfflineFirstMoviesRepositoryTest {

    private val network = mockk<MoviesNetworkDataSource>(relaxed = true)
    private val movieDao = mockk<MovieDao>(relaxed = true)
    private val movieDetailDao = mockk<MovieDetailDao>(relaxed = true)
    private val castDao = mockk<CastDao>(relaxed = true)
    private val reviewDao = mockk<ReviewDao>(relaxed = true)
    private var now = 1_000_000L

    private val repository = OfflineFirstMoviesRepository(
        network = network,
        movieDao = movieDao,
        movieDetailDao = movieDetailDao,
        castDao = castDao,
        reviewDao = reviewDao,
        clock = { now },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private val networkMovie = NetworkMovie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
    )

    private val movieEntity = MovieEntity(
        id = 1,
        category = "POPULAR",
        title = "Dune",
        overview = "Sand.",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
        syncedAt = 0L,
    )

    private fun detailFixture() = NetworkMovieDetail(
        id = 1,
        title = "Dune",
        originalTitle = "Dune",
        originalLanguage = "en",
        overview = "Sand.",
        genres = emptyList(),
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        runtime = 155,
        status = "Released",
        video = false,
        voteAverage = 7.8,
        voteCount = 100,
    )

    @Test
    fun `getMovies reads from the database and maps to models`() = runTest {
        coEvery { movieDao.getByCategory("POPULAR") } returns flowOf(listOf(movieEntity))

        val movies = repository.getMovies(MovieCategory.POPULAR).first()

        assertEquals(listOf("Dune"), movies.map { it.title })
    }

    @Test
    fun `refresh skips the network while the cache is fresh`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns now - 1_000L

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 0) { network.getMovies(any()) }
        coVerify(exactly = 0) { movieDao.replaceCategory(any(), any()) }
    }

    @Test
    fun `refresh calls the network when nothing is cached`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns null
        coEvery { network.getMovies(MovieCategory.POPULAR) } returns listOf(networkMovie)

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 1) { movieDao.replaceCategory("POPULAR", any()) }
    }

    @Test
    fun `refresh calls the network once the TTL has elapsed`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns now - TTL_MS - 1
        coEvery { network.getMovies(MovieCategory.POPULAR) } returns listOf(networkMovie)

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 1) { network.getMovies(MovieCategory.POPULAR) }
    }

    @Test
    fun `refresh reports failure and writes nothing when the network throws`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns null
        coEvery { network.getMovies(MovieCategory.POPULAR) } throws IOException("offline")

        assertFalse(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 0) { movieDao.replaceCategory(any(), any()) }
    }

    @Test
    fun `refreshDetail stores detail cast and reviews together`() = runTest {
        coEvery { movieDetailDao.getSyncedAt(1) } returns null
        coEvery { network.getMovieDetail(1) } returns detailFixture()
        coEvery { network.getCast(1) } returns emptyList()
        coEvery { network.getReviews(1) } returns emptyList()

        assertTrue(repository.refreshDetail(1))

        coVerify(exactly = 1) { movieDetailDao.upsert(any()) }
        coVerify(exactly = 1) { castDao.upsertAll(any()) }
        coVerify(exactly = 1) { reviewDao.upsertAll(any()) }
    }

    @Test
    fun `refreshDetail skips the network while the cache is fresh`() = runTest {
        coEvery { movieDetailDao.getSyncedAt(1) } returns now - 1_000L

        assertTrue(repository.refreshDetail(1))

        coVerify(exactly = 0) { network.getMovieDetail(any()) }
    }
}
