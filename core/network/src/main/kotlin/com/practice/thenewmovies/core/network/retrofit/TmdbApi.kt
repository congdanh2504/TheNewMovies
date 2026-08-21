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
package com.practice.thenewmovies.core.network.retrofit

import com.practice.thenewmovies.core.network.model.NetworkCastResponse
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkPage
import com.practice.thenewmovies.core.network.model.NetworkReview
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TmdbApi {

    @GET("movie/popular")
    suspend fun getPopular(): NetworkPage<NetworkMovie>

    @GET("movie/top_rated")
    suspend fun getTopRated(): NetworkPage<NetworkMovie>

    @GET("movie/now_playing")
    suspend fun getNowPlaying(): NetworkPage<NetworkMovie>

    @GET("movie/upcoming")
    suspend fun getUpcoming(): NetworkPage<NetworkMovie>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(@Path("movie_id") movieId: Int): NetworkMovieDetail

    @GET("movie/{movie_id}/credits")
    suspend fun getCredits(@Path("movie_id") movieId: Int): NetworkCastResponse

    @GET("movie/{movie_id}/reviews")
    suspend fun getReviews(@Path("movie_id") movieId: Int): NetworkPage<NetworkReview>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): NetworkPage<NetworkMovie>
}
