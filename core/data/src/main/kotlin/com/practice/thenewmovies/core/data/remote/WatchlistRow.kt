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
package com.practice.thenewmovies.core.data.remote

import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `public.watchlist`. `updated_at` is deliberately absent: Postgres defaults it, and
 * sending a client clock would make it useless as a merge signal later.
 */
@Serializable
internal data class WatchlistRow(
    @SerialName("user_id") val userId: String,
    @SerialName("movie_id") val movieId: Int,
    @SerialName("title") val title: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("vote_average") val voteAverage: Double,
    @SerialName("runtime") val runtime: Int,
    @SerialName("genre") val genre: String,
    @SerialName("user_rating") val userRating: Float?,
)

internal fun WatchlistEntity.asRow() = WatchlistRow(
    userId = userId,
    movieId = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
)

internal fun WatchlistRow.asEntity() = WatchlistEntity(
    userId = userId,
    movieId = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
    pendingSync = false,
    deleted = false,
)
