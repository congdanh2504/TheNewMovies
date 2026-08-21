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

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() = database.close()

    private fun entry(movieId: Int, title: String = "Movie $movieId") = WatchlistEntity(
        movieId = movieId,
        title = title,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        runtime = 120,
        genre = "Action",
    )

    @Test
    fun getAll_isSortedByTitle() = runTest {
        dao.upsert(entry(1, title = "Zulu"))
        dao.upsert(entry(2, title = "Alien"))

        assertEquals(listOf(2, 1), dao.getAll().first().map { it.movieId })
    }

    @Test
    fun existsById_reflectsInsertAndDelete() = runTest {
        assertFalse(dao.existsById(1).first())

        dao.upsert(entry(1))
        assertTrue(dao.existsById(1).first())

        dao.deleteById(1)
        assertFalse(dao.existsById(1).first())
    }

    @Test
    fun updateRating_storesTheRating() = runTest {
        dao.upsert(entry(1))

        dao.updateRating(movieId = 1, rating = 4.5f)

        assertEquals(4.5f, dao.getRating(1).first())
    }

    @Test
    fun upsert_replacesAnExistingEntry() = runTest {
        dao.upsert(entry(1, title = "Old"))
        dao.upsert(entry(1, title = "New"))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("New", all.single().title)
    }
}
