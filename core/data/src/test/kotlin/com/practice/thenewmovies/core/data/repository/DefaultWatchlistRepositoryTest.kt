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
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import com.practice.thenewmovies.core.model.WatchlistMovie
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultWatchlistRepositoryTest {

    private val dao = mockk<WatchlistDao>(relaxed = true)
    private val repository = DefaultWatchlistRepository(dao)

    @Test
    fun `getWatchlist maps entities to models`() = runTest {
        every { dao.getAll() } returns flowOf(
            listOf(
                WatchlistEntity(
                    movieId = 7,
                    title = "Dune",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2021-10-22",
                    voteAverage = 7.8,
                    runtime = 155,
                    genre = "Science Fiction",
                    userRating = 4.5f,
                ),
            ),
        )

        val movies = repository.getWatchlist().first()

        assertEquals(7, movies.single().id)
        assertEquals(4.5f, movies.single().userRating)
    }

    @Test
    fun `addToWatchlist writes the mapped entity`() = runTest {
        repository.addToWatchlist(
            WatchlistMovie(
                id = 7,
                title = "Dune",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2021-10-22",
                voteAverage = 7.8,
                runtime = 155,
                genre = "Science Fiction",
            ),
        )

        coVerify(exactly = 1) { dao.upsert(match { it.movieId == 7 }) }
    }

    @Test
    fun `setRating delegates to the dao`() = runTest {
        repository.setRating(movieId = 7, rating = 3f)

        coVerify(exactly = 1) { dao.updateRating(movieId = 7, rating = 3f) }
    }
}
