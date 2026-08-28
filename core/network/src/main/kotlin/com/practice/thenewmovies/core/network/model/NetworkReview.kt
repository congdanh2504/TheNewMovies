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
data class NetworkReview(
    @param:Json(name = "author") val author: String,
    @param:Json(name = "content") val content: String,
    @param:Json(name = "created_at") val createdAt: String,
    @param:Json(name = "author_details") val authorDetails: NetworkAuthorDetails? = null,
)

@JsonClass(generateAdapter = true)
data class NetworkAuthorDetails(
    @param:Json(name = "avatar_path") val avatarPath: String? = null,
    @param:Json(name = "rating") val rating: Float? = null,
)
