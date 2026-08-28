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
package com.practice.thenewmovies.core.data.movies

import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.model.NetworkMovie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkEntityTest {

    private fun networkMovie(
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
    ) = NetworkMovie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
    )

    @Test
    fun `prefixes relative image paths`() {
        val entity = networkMovie().asEntity(MovieCategory.POPULAR, syncedAt = 42L)

        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", entity.posterPath)
        assertEquals("https://image.tmdb.org/t/p/w500/backdrop.jpg", entity.backdropPath)
    }

    @Test
    fun `keeps missing image paths null`() {
        val entity = networkMovie(posterPath = null, backdropPath = null)
            .asEntity(MovieCategory.POPULAR, syncedAt = 42L)

        assertNull(entity.posterPath)
        assertNull(entity.backdropPath)
    }

    @Test
    fun `carries the category and sync timestamp`() {
        val entity = networkMovie().asEntity(MovieCategory.UPCOMING, syncedAt = 42L)

        assertEquals("UPCOMING", entity.category)
        assertEquals(42L, entity.syncedAt)
    }
}
