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
package com.practice.thenewmovies.core.domain

import com.practice.thenewmovies.core.data.repository.MovieDetailRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val moviesRepository: MovieDetailRepository,
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(movieId: Int): Flow<MovieDetailWithExtras> = combine(
        moviesRepository.getMovieDetail(movieId),
        moviesRepository.getCast(movieId),
        moviesRepository.getReviews(movieId),
        watchlistRepository.isInWatchlist(movieId),
        watchlistRepository.getRating(movieId),
    ) { detail, cast, reviews, isInWatchlist, userRating ->
        MovieDetailWithExtras(
            detail = detail,
            cast = cast,
            reviews = reviews,
            isInWatchlist = isInWatchlist,
            userRating = userRating,
        )
    }
}
