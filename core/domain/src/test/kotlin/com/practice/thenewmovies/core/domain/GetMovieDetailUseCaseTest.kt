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
package com.practice.thenewmovies.core.domain

import com.practice.thenewmovies.core.testing.data.testCast
import com.practice.thenewmovies.core.testing.data.testMovieDetail
import com.practice.thenewmovies.core.testing.data.testReviews
import com.practice.thenewmovies.core.testing.data.testWatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestMovieDetailRepository
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMovieDetailUseCaseTest {

    private val moviesRepository = TestMovieDetailRepository()
    private val watchlistRepository = TestWatchlistRepository()
    private val useCase = GetMovieDetailUseCase(moviesRepository, watchlistRepository)

    @Test
    fun `combines detail cast and reviews`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        moviesRepository.emitCast(testCast)
        moviesRepository.emitReviews(testReviews)

        val result = useCase(movieId = 1).first()

        assertEquals(testMovieDetail, result.detail)
        assertEquals(testCast, result.cast)
        assertEquals(testReviews, result.reviews)
    }

    @Test
    fun `reports the movie is not saved and has no user rating by default`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)

        val result = useCase(movieId = 1).first()

        assertFalse(result.isInWatchlist)
        assertNull(result.userRating)
    }

    @Test
    fun `reports watchlist membership and rating`() = runTest {
        moviesRepository.emitDetail(testMovieDetail)
        watchlistRepository.emit(listOf(testWatchlistMovie.copy(userRating = 4f)))

        val result = useCase(movieId = testWatchlistMovie.id).first()

        assertTrue(result.isInWatchlist)
        assertEquals(4f, result.userRating)
    }

    @Test
    fun `emits a null detail while nothing is cached`() = runTest {
        val result = useCase(movieId = 1).first()

        assertNull(result.detail)
    }
}
