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

import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession

/**
 * Maps supabase-kt's session status onto the app's [SessionState], given the state the app
 * already believed before this status arrived. supabase-kt gives [SessionStatus.RefreshFailure]
 * no cached session to fall back on -- verified against the 3.1.4 artifact, not docs -- so
 * `previous` is the only signal available for deciding whether a failed refresh should sign the
 * user out.
 */
internal fun SessionStatus.toSessionState(previous: SessionState): SessionState = when (this) {
    is SessionStatus.Initializing -> SessionState.Loading

    is SessionStatus.Authenticated ->
        // A session with no user record is unusable for anything the app needs, so it
        // is folded into the same SignedOut bucket as SessionStatus.NotAuthenticated.
        session.toAuthUser()?.let(SessionState::SignedIn) ?: SessionState.SignedOut

    is SessionStatus.NotAuthenticated -> SessionState.SignedOut

    is SessionStatus.RefreshFailure -> when (cause) {
        is RefreshFailureCause.NetworkError ->
            // The refresh couldn't reach the server at all, so this says nothing about whether
            // the credentials are still good -- only that a request timed out or the network
            // was briefly down. This app is offline-first and renders every screen from Room, so
            // treating a lost second of signal as a sign-out would eject a still-valid user and
            // destroy their back stack for nothing. Keep whatever we already believed.
            if (previous is SessionState.SignedIn) previous else SessionState.SignedOut

        is RefreshFailureCause.InternalServerError ->
            // Here the server did answer, and it rejected the refresh -- the token is likely
            // revoked (password changed on another device, account deleted). Unlike a network
            // blip, this is a real signal that the session is no longer valid.
            SessionState.SignedOut
    }
}

private fun UserSession.toAuthUser(): AuthUser? = user?.let { user ->
    AuthUser(id = user.id, email = user.email.orEmpty())
}
