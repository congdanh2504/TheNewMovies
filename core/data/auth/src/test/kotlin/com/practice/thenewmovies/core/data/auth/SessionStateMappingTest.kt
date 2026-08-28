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
package com.practice.thenewmovies.core.data.auth

import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.statement.HttpResponse
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SessionStateMappingTest {

    private val user = AuthUser(id = "user-1", email = "person@example.com")

    private fun session(withUser: Boolean) = UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3600,
        tokenType = "bearer",
        user = if (withUser) UserInfo(aud = "authenticated", id = user.id, email = user.email) else null,
    )

    private fun internalServerError(): RefreshFailureCause.InternalServerError {
        val response = mockk<HttpResponse>(relaxed = true)
        return RefreshFailureCause.InternalServerError(RestException("error", "description", response))
    }

    @Test
    fun `Initializing maps to Loading`() {
        assertEquals(
            SessionState.Loading,
            SessionStatus.Initializing.toSessionState(previous = SessionState.SignedOut),
        )
    }

    @Test
    fun `NotAuthenticated maps to SignedOut`() {
        assertEquals(
            SessionState.SignedOut,
            SessionStatus.NotAuthenticated(isSignOut = false).toSessionState(previous = SessionState.SignedOut),
        )
    }

    @Test
    fun `Authenticated with a user maps to SignedIn carrying that user`() {
        assertEquals(
            SessionState.SignedIn(user),
            SessionStatus.Authenticated(session(withUser = true)).toSessionState(previous = SessionState.Loading),
        )
    }

    @Test
    fun `Authenticated with no user maps to SignedOut`() {
        assertEquals(
            SessionState.SignedOut,
            SessionStatus.Authenticated(session(withUser = false)).toSessionState(previous = SessionState.Loading),
        )
    }

    @Test
    fun `RefreshFailure from a network error keeps a signed-in user signed in`() {
        val status = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("no route to host")))

        assertEquals(
            SessionState.SignedIn(user),
            status.toSessionState(previous = SessionState.SignedIn(user)),
        )
    }

    @Test
    fun `RefreshFailure from a network error stays signed out when previously signed out`() {
        val status = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("no route to host")))

        assertEquals(
            SessionState.SignedOut,
            status.toSessionState(previous = SessionState.SignedOut),
        )
    }

    @Test
    fun `RefreshFailure from an internal server error signs out even a signed-in user`() {
        val status = SessionStatus.RefreshFailure(internalServerError())

        assertEquals(
            SessionState.SignedOut,
            status.toSessionState(previous = SessionState.SignedIn(user)),
        )
    }
}
