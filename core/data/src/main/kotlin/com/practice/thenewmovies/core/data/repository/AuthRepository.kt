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
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits [SessionState.Loading] until the stored session has been read. */
    val sessionState: Flow<SessionState>

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun signIn(email: String, password: String): AuthResult

    /** Sends a recovery mail containing a 6-digit code. */
    suspend fun sendPasswordReset(email: String): AuthResult

    /** Verifies the recovery code, then sets [newPassword] on the account. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult

    suspend fun signOut()
}
