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
package com.practice.thenewmovies.core.testing.data

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.model.WatchlistMovie

val testMovies = listOf(
    Movie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = "https://image.tmdb.org/t/p/w500/dune.jpg",
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 1000,
    ),
    Movie(
        id = 2,
        title = "Arrival",
        overview = "Squid ink.",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2016-11-11",
        voteAverage = 7.6,
        voteCount = 900,
    ),
)

val testMovieDetail = MovieDetail(
    id = 1,
    title = "Dune",
    originalTitle = "Dune",
    originalLanguage = "en",
    overview = "Sand.",
    genres = listOf(Genre(id = 878, name = "Science Fiction")),
    posterPath = null,
    backdropPath = null,
    releaseDate = "2021-10-22",
    runtime = 155,
    status = "Released",
    video = false,
    voteAverage = 7.8,
    voteCount = 1000,
)

val testCast = listOf(
    Cast(castId = 1, character = "Paul", name = "Timothee Chalamet", profilePath = null),
)

val testReviews = listOf(
    Review(author = "critic", content = "Long.", createdAt = "2021-11-01", rating = 8f),
)

val testWatchlistMovie = WatchlistMovie(
    id = 1,
    title = "Dune",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2021-10-22",
    voteAverage = 7.8,
    runtime = 155,
    genre = "Science Fiction",
)
