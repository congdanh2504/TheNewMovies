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
package com.practice.thenewmovies.core.data.watchlist

import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WatchlistRowMappingTest {

    private val entity = WatchlistEntity(
        userId = "user-1",
        movieId = 100,
        title = "Dune: Part Two",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
        userRating = 4.0f,
        pendingSync = true,
        deleted = false,
    )

    @Test
    fun `an entity maps to a row without its local sync flags`() {
        val row = entity.asRow()

        assertEquals("user-1", row.userId)
        assertEquals(100, row.movieId)
        assertEquals("Dune: Part Two", row.title)
        assertEquals(4.0f, row.userRating)
    }

    @Test
    fun `a row maps back to a synced entity`() {
        val mapped = entity.asRow().asEntity()

        assertEquals(entity.copy(pendingSync = false), mapped)
        assertFalse(mapped.pendingSync)
    }

    @Test
    fun `the row serialises with snake case column names`() {
        val json = Json.encodeToString(WatchlistRow.serializer(), entity.asRow())

        assertEquals(true, json.contains("\"user_id\""))
        assertEquals(true, json.contains("\"movie_id\""))
        assertEquals(true, json.contains("\"poster_path\""))
        assertEquals(true, json.contains("\"vote_average\""))
        assertEquals(true, json.contains("\"user_rating\""))
        // updated_at is defaulted by Postgres; sending it would overwrite the server's clock.
        assertEquals(false, json.contains("updated_at"))
    }
}
