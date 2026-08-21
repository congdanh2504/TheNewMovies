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
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.model.Genre
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailEntityExtTest {

    private fun entity(genresJson: String) = MovieDetailEntity(
        id = 1,
        title = "Dune",
        originalTitle = "Dune",
        originalLanguage = "en",
        overview = "Sand.",
        genresJson = genresJson,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        runtime = 155,
        status = "Released",
        video = false,
        voteAverage = 7.8,
        voteCount = 100,
        syncedAt = 0,
    )

    @Test
    fun `parses stored genres`() {
        val model = entity("""[{"id":878,"name":"Science Fiction"}]""").asExternalModel()

        assertEquals(listOf(Genre(id = 878, name = "Science Fiction")), model.genres)
    }

    @Test
    fun `returns no genres when the column is malformed`() {
        val model = entity("not json").asExternalModel()

        assertEquals(emptyList<Genre>(), model.genres)
    }

    @Test
    fun `round trips a genre list through the column encoding`() {
        val genres = listOf(Genre(id = 1, name = "Action"), Genre(id = 2, name = "Drama"))

        val decoded = entity(genres.asGenresJson()).asExternalModel().genres

        assertEquals(genres, decoded)
    }
}
