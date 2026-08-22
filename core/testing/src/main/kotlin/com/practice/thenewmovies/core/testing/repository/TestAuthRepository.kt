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
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestAuthRepository : AuthRepository {

    private val state = MutableStateFlow<SessionState>(SessionState.Loading)

    override val sessionState: Flow<SessionState> = state

    /**
     * Set to non-null to make every call fail with this error until reset back to `null`.
     * [signOut] never consults it, since sign-out always succeeds locally.
     */
    var nextError: AuthError? = null

    val signUpCalls = mutableListOf<Pair<String, String>>()
    val signInCalls = mutableListOf<Pair<String, String>>()
    val resetEmails = mutableListOf<String>()
    val resetCalls = mutableListOf<Triple<String, String, String>>()
    var signOutCount = 0
        private set

    /**
     * Unlike the production repository — where [sessionState] is driven independently by
     * supabase's own session flow — this fake emits [SessionState.SignedIn] synchronously on
     * success, same as [signIn]. That coupling is what makes the signed-in path testable here,
     * but a test asserting [sessionState] becomes [SessionState.SignedIn] after this call
     * succeeds is exercising the fake's wiring, not any production behaviour.
     */
    override suspend fun signUp(email: String, password: String): AuthResult {
        signUpCalls += email to password
        return result { emitSignedIn(email) }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += email to password
        return result { emitSignedIn(email) }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        resetEmails += email
        return result {}
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AuthResult {
        resetCalls += Triple(email, code, newPassword)
        return result {}
    }

    override suspend fun signOut() {
        signOutCount++
        state.value = SessionState.SignedOut
    }

    fun emitLoading() {
        state.value = SessionState.Loading
    }

    fun emitSignedOut() {
        state.value = SessionState.SignedOut
    }

    fun emitSignedIn(email: String = "user@example.com", id: String = "user-1") {
        state.value = SessionState.SignedIn(AuthUser(id = id, email = email))
    }

    private inline fun result(onSuccess: () -> Unit): AuthResult {
        val error = nextError
        return if (error == null) {
            onSuccess()
            AuthResult.Success
        } else {
            AuthResult.Failure(error)
        }
    }
}
