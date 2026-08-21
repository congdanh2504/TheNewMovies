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

import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import com.practice.thenewmovies.core.network.model.NetworkCast
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkPage
import com.practice.thenewmovies.core.network.model.NetworkReview
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RetrofitMoviesNetwork @Inject constructor(
    private val api: TmdbApi,
) : MoviesNetworkDataSource {

    override suspend fun getMovies(category: MovieCategory): List<NetworkMovie> = when (category) {
        MovieCategory.POPULAR -> api.getPopular()
        MovieCategory.TOP_RATED -> api.getTopRated()
        MovieCategory.NOW_PLAYING -> api.getNowPlaying()
        MovieCategory.UPCOMING -> api.getUpcoming()
    }.results

    override suspend fun getMovieDetail(movieId: Int): NetworkMovieDetail =
        api.getMovieDetail(movieId)

    override suspend fun getCast(movieId: Int): List<NetworkCast> =
        api.getCredits(movieId).cast

    override suspend fun getReviews(movieId: Int): List<NetworkReview> =
        api.getReviews(movieId).results

    override suspend fun searchMovies(query: String, page: Int): NetworkPage<NetworkMovie> =
        api.searchMovies(query = query, page = page)
}
