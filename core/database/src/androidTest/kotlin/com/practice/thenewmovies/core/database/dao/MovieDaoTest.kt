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
import com.practice.thenewmovies.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MovieDaoTest {

    private lateinit var database: MoviesDatabase
    private lateinit var dao: MovieDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.movieDao()
    }

    @After
    fun tearDown() = database.close()

    private fun movie(id: Int, category: String, syncedAt: Long = 1_000L) = MovieEntity(
        id = id,
        category = category,
        title = "Movie $id",
        overview = "Overview",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 10,
        syncedAt = syncedAt,
    )

    @Test
    fun getByCategory_returnsOnlyThatCategory() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "TOP_RATED")))

        val popular = dao.getByCategory("POPULAR").first()

        assertEquals(listOf(1), popular.map { it.id })
    }

    @Test
    fun theSameMovieCanLiveInTwoCategories() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(1, "NOW_PLAYING")))

        assertEquals(1, dao.getByCategory("POPULAR").first().size)
        assertEquals(1, dao.getByCategory("NOW_PLAYING").first().size)
    }

    @Test
    fun replaceCategory_dropsRowsMissingFromTheNewList() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "POPULAR")))

        dao.replaceCategory("POPULAR", listOf(movie(3, "POPULAR")))

        assertEquals(listOf(3), dao.getByCategory("POPULAR").first().map { it.id })
    }

    @Test
    fun replaceCategory_leavesOtherCategoriesAlone() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "UPCOMING")))

        dao.replaceCategory("POPULAR", emptyList())

        assertEquals(listOf(2), dao.getByCategory("UPCOMING").first().map { it.id })
    }

    @Test
    fun getSyncedAt_returnsNewestValueAndNullWhenEmpty() = runTest {
        assertEquals(null, dao.getSyncedAt("POPULAR"))

        dao.upsertAll(
            listOf(
                movie(1, "POPULAR", syncedAt = 100L),
                movie(2, "POPULAR", syncedAt = 500L),
            ),
        )

        assertEquals(500L, dao.getSyncedAt("POPULAR"))
    }
}
