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

import com.practice.thenewmovies.core.database.entity.CastEntity
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.database.entity.ReviewEntity
import com.practice.thenewmovies.core.database.model.asGenresJson
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.model.NetworkCast
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkReview

internal const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

internal fun String?.asImageUrl(): String? = when {
    this == null -> null
    startsWith("http") -> this
    else -> IMAGE_BASE_URL + this
}

fun NetworkMovie.asEntity(category: MovieCategory, syncedAt: Long) = MovieEntity(
    id = id,
    category = category.name,
    title = title,
    overview = overview,
    posterPath = posterPath.asImageUrl(),
    backdropPath = backdropPath.asImageUrl(),
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    syncedAt = syncedAt,
)

fun NetworkMovieDetail.asEntity(syncedAt: Long) = MovieDetailEntity(
    id = id,
    title = title,
    originalTitle = originalTitle,
    originalLanguage = originalLanguage,
    overview = overview,
    genresJson = genres.map { Genre(id = it.id, name = it.name) }.asGenresJson(),
    posterPath = posterPath.asImageUrl(),
    backdropPath = backdropPath.asImageUrl(),
    releaseDate = releaseDate,
    runtime = runtime,
    status = status,
    video = video,
    voteAverage = voteAverage,
    voteCount = voteCount,
    syncedAt = syncedAt,
)

fun NetworkCast.asEntity(movieId: Int) = CastEntity(
    movieId = movieId,
    castId = castId,
    character = character,
    name = name,
    profilePath = profilePath.asImageUrl(),
)

fun NetworkReview.asEntity(movieId: Int) = ReviewEntity(
    movieId = movieId,
    author = author,
    content = content,
    createdAt = createdAt,
    avatarPath = authorDetails?.avatarPath.asImageUrl(),
    rating = authorDetails?.rating,
)
