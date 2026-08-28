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
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkMovieDetail(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "original_title") val originalTitle: String,
    @param:Json(name = "original_language") val originalLanguage: String,
    @param:Json(name = "overview") val overview: String?,
    @param:Json(name = "genres") val genres: List<NetworkGenre>,
    @param:Json(name = "poster_path") val posterPath: String?,
    @param:Json(name = "backdrop_path") val backdropPath: String?,
    @param:Json(name = "release_date") val releaseDate: String,
    @param:Json(name = "runtime") val runtime: Long?,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "video") val video: Boolean,
    @param:Json(name = "vote_average") val voteAverage: Double,
    @param:Json(name = "vote_count") val voteCount: Int,
)

@JsonClass(generateAdapter = true)
data class NetworkGenre(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "name") val name: String,
)
