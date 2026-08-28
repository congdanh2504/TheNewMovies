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
package com.practice.thenewmovies.core.data.watchlist

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only file in the app that talks to Postgrest.
 *
 * The `user_id` filters are belt and braces: the table's RLS policy already restricts every
 * statement to `auth.uid()`. They stay because a filter-less delete would be a catastrophic bug
 * if the policy were ever dropped.
 */
@Singleton
internal class WatchlistRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
) {

    suspend fun fetchAll(userId: String): List<WatchlistRow> =
        client.from(TABLE)
            .select { filter { eq("user_id", userId) } }
            .decodeList()

    suspend fun upsert(row: WatchlistRow) {
        client.from(TABLE).upsert(row)
    }

    suspend fun delete(userId: String, movieId: Int) {
        client.from(TABLE).delete {
            filter {
                eq("user_id", userId)
                eq("movie_id", movieId)
            }
        }
    }

    private companion object {
        const val TABLE = "watchlist"
    }
}
