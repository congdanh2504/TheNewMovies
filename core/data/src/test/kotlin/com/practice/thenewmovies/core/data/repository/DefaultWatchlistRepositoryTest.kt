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

import com.practice.thenewmovies.core.data.remote.WatchlistRemoteDataSource
import com.practice.thenewmovies.core.data.remote.asRow
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import com.practice.thenewmovies.core.model.WatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultWatchlistRepositoryTest {

    private val dao = mockk<WatchlistDao>(relaxed = true)
    private val remote = mockk<WatchlistRemoteDataSource>(relaxed = true)
    private val authRepository = TestAuthRepository().apply { emitSignedIn(id = "user-1") }

    private val repository = DefaultWatchlistRepository(
        watchlistDao = dao,
        remote = remote,
        authRepository = authRepository,
    )

    private val movie = WatchlistMovie(
        id = 100,
        title = "Dune: Part Two",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
    )

    private fun entity(
        movieId: Int = 100,
        pendingSync: Boolean = false,
        deleted: Boolean = false,
    ) = WatchlistEntity(
        userId = "user-1",
        movieId = movieId,
        title = "Dune: Part Two",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
        userRating = null,
        pendingSync = pendingSync,
        deleted = deleted,
    )

    @Test
    fun `reads are scoped to the signed-in user`() = runTest {
        every { dao.getAll("user-1") } returns flowOf(listOf(entity()))

        assertEquals(listOf(100), repository.getWatchlist().first().map { it.id })
    }

    @Test
    fun `a signed-out user has an empty watchlist`() = runTest {
        authRepository.emitSignedOut()

        assertTrue(repository.getWatchlist().first().isEmpty())
    }

    @Test
    fun `adding writes to Room first, then pushes and clears the flag`() = runTest {
        repository.addToWatchlist(movie)

        coVerify { dao.upsert(match { it.pendingSync && it.userId == "user-1" }) }
        coVerify { remote.upsert(any()) }
        coVerify { dao.clearPending("user-1", 100) }
    }

    @Test
    fun `a failed push leaves the row pending`() = runTest {
        coEvery { remote.upsert(any()) } throws IOException("offline")

        repository.addToWatchlist(movie)

        coVerify { dao.upsert(match { it.pendingSync }) }
        coVerify(exactly = 0) { dao.clearPending(any(), any()) }
    }

    @Test
    fun `removing soft-deletes locally, then hard-deletes once accepted`() = runTest {
        repository.removeFromWatchlist(100)

        coVerify { dao.markDeleted("user-1", 100) }
        coVerify { remote.delete("user-1", 100) }
        coVerify { dao.deleteById("user-1", 100) }
    }

    @Test
    fun `a failed delete keeps the soft-deleted row for later`() = runTest {
        coEvery { remote.delete(any(), any()) } throws IOException("offline")

        repository.removeFromWatchlist(100)

        coVerify { dao.markDeleted("user-1", 100) }
        coVerify(exactly = 0) { dao.deleteById(any(), any()) }
    }

    @Test
    fun `rating writes through the same way`() = runTest {
        coEvery { dao.getPending("user-1") } returns listOf(entity(pendingSync = true))

        repository.setRating(100, 4.5f)

        coVerify { dao.updateRating("user-1", 100, 4.5f) }
        coVerify { remote.upsert(any()) }
        coVerify { dao.clearPending("user-1", 100) }
    }

    @Test
    fun `sync pushes pending rows before pulling`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            listOf(entity(movieId = 1, pendingSync = true)),
            emptyList(),
        )
        coEvery { remote.fetchAll("user-1") } returns listOf(entity(movieId = 2).asRow())

        repository.syncWatchlist()

        coVerify { remote.upsert(any()) }
        coVerify { dao.deleteSynced("user-1") }
        coVerify { dao.upsertAll(match { rows -> rows.map { it.movieId } == listOf(2) }) }
    }

    @Test
    fun `sync pushes a pending delete as a delete`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            listOf(entity(movieId = 1, pendingSync = true, deleted = true)),
            emptyList(),
        )
        coEvery { remote.fetchAll("user-1") } returns emptyList()

        repository.syncWatchlist()

        coVerify { remote.delete("user-1", 1) }
        coVerify { dao.deleteById("user-1", 1) }
    }

    @Test
    fun `the pull never overwrites a row that is still pending`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            emptyList(),
            listOf(entity(movieId = 5, pendingSync = true)),
        )
        coEvery { remote.fetchAll("user-1") } returns listOf(
            entity(movieId = 5).asRow(),
            entity(movieId = 6).asRow(),
        )

        repository.syncWatchlist()

        coVerify { dao.upsertAll(match { rows -> rows.map { it.movieId } == listOf(6) }) }
    }

    @Test
    fun `sync does nothing when signed out`() = runTest {
        authRepository.emitSignedOut()

        repository.syncWatchlist()

        coVerify(exactly = 0) { remote.fetchAll(any()) }
    }

    @Test
    fun `a Room failure during sync does not propagate`() = runTest {
        coEvery { dao.getPending("user-1") } throws IllegalStateException("disk full")

        repository.syncWatchlist()

        // No assertion beyond "returned normally" -- reaching this line is the test.
    }

    @Test
    fun `cancellation during sync still propagates`() {
        coEvery { dao.getPending("user-1") } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.syncWatchlist() }
        }
    }
}
