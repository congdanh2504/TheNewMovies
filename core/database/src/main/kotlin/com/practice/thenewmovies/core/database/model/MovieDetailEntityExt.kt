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
import com.practice.thenewmovies.core.model.MovieDetail
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class GenreJson(val id: Int, val name: String)

private val json = Json { ignoreUnknownKeys = true }

fun MovieDetailEntity.asExternalModel() = MovieDetail(
    id = id,
    title = title,
    originalTitle = originalTitle,
    originalLanguage = originalLanguage,
    overview = overview,
    genres = genresJson.asGenres(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    runtime = runtime,
    status = status,
    video = video,
    voteAverage = voteAverage,
    voteCount = voteCount,
)

/** Encodes genres for the `genresJson` column. */
fun List<Genre>.asGenresJson(): String =
    json.encodeToString(map { GenreJson(id = it.id, name = it.name) })

private fun String.asGenres(): List<Genre> = try {
    json.decodeFromString<List<GenreJson>>(this).map { Genre(id = it.id, name = it.name) }
} catch (e: SerializationException) {
    emptyList()
} catch (e: IllegalArgumentException) {
    emptyList()
}
