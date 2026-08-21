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
package com.practice.thenewmovies.feature.detail.impl

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review

internal sealed interface DetailUiState {

    data object Loading : DetailUiState

    data class Success(
        val detail: MovieDetail,
        val cast: List<Cast>,
        val reviews: List<Review>,
        val isInWatchlist: Boolean,
        val userRating: Float?,
    ) : DetailUiState

    data class Error(val message: String) : DetailUiState
}

internal val detailTabs = listOf("About Movie", "Reviews", "Cast")
