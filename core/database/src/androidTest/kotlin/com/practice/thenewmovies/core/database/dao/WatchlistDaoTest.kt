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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.practice.thenewmovies.core.database.MoviesDatabase
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WatchlistDaoTest {

    private lateinit var database: MoviesDatabase
    private lateinit var dao: WatchlistDao

    private val alice = "user-alice"
    private val bob = "user-bob"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        userId: String,
        movieId: Int,
        title: String = "Dune",
        pendingSync: Boolean = false,
        deleted: Boolean = false,
    ) = WatchlistEntity(
        userId = userId,
        movieId = movieId,
        title = title,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        runtime = 155,
        genre = "Science Fiction",
        userRating = null,
        pendingSync = pendingSync,
        deleted = deleted,
    )

    @Test
    fun rowsAreScopedToTheirUser() = runTest {
        dao.upsert(entity(alice, movieId = 1, title = "Dune"))
        dao.upsert(entity(bob, movieId = 2, title = "Arrival"))

        assertEquals(listOf("Dune"), dao.getAll(alice).first().map { it.title })
        assertEquals(listOf("Arrival"), dao.getAll(bob).first().map { it.title })
    }

    @Test
    fun theSameMovieCanBeSavedByTwoUsers() = runTest {
        dao.upsert(entity(alice, movieId = 1))
        dao.upsert(entity(bob, movieId = 1))

        assertEquals(1, dao.getAll(alice).first().size)
        assertEquals(1, dao.getAll(bob).first().size)
    }

    @Test
    fun softDeletedRowsAreHiddenFromReads() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.markDeleted(alice, movieId = 1)

        assertTrue(dao.getAll(alice).first().isEmpty())
        assertFalse(dao.existsById(alice, movieId = 1).first())
    }

    @Test
    fun markingDeletedAlsoMarksTheRowPending() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.markDeleted(alice, movieId = 1)

        val pending = dao.getPending(alice)
        assertEquals(1, pending.size)
        assertTrue(pending.single().deleted)
    }

    @Test
    fun ratingAMovieMarksItPending() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.updateRating(alice, movieId = 1, rating = 4.5f)

        assertEquals(4.5f, dao.getRating(alice, movieId = 1).first())
        assertEquals(listOf(1), dao.getPending(alice).map { it.movieId })
    }

    @Test
    fun clearingPendingLeavesTheRow() = runTest {
        dao.upsert(entity(alice, movieId = 1, pendingSync = true))

        dao.clearPending(alice, movieId = 1)

        assertTrue(dao.getPending(alice).isEmpty())
        assertEquals(1, dao.getAll(alice).first().size)
    }

    @Test
    fun deletingSyncedRowsKeepsPendingOnes() = runTest {
        dao.upsert(entity(alice, movieId = 1, pendingSync = true))
        dao.upsert(entity(alice, movieId = 2, pendingSync = false))

        dao.deleteSynced(alice)

        assertEquals(listOf(1), dao.getAll(alice).first().map { it.movieId })
    }

    @Test
    fun hardDeleteRemovesTheRowEntirely() = runTest {
        dao.upsert(entity(alice, movieId = 1, deleted = true, pendingSync = true))

        dao.deleteById(alice, movieId = 1)

        assertTrue(dao.getPending(alice).isEmpty())
    }
}
