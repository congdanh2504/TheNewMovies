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

import com.practice.thenewmovies.core.common.network.Dispatcher
import com.practice.thenewmovies.core.common.network.MoviesDispatchers
import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient,
    @Dispatcher(MoviesDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val sessionState: Flow<SessionState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated ->
                status.session.toAuthUser()?.let(SessionState::SignedIn) ?: SessionState.SignedOut

            is SessionStatus.Initializing -> SessionState.Loading

            is SessionStatus.NotAuthenticated -> SessionState.SignedOut

            is SessionStatus.RefreshFailure -> SessionState.SignedOut
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult = attempt {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = attempt {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = attempt {
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AuthResult = attempt {
        client.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = code)
        client.auth.updateUser { password = newPassword }
    }

    override suspend fun signOut() {
        withContext(ioDispatcher) { runCatching { client.auth.signOut() } }
    }

    private suspend fun attempt(block: suspend () -> Unit): AuthResult =
        withContext(ioDispatcher) {
            try {
                block()
                AuthResult.Success
            } catch (throwable: Throwable) {
                AuthResult.Failure(throwable.toAuthError())
            }
        }

    private fun UserSession.toAuthUser(): AuthUser? = user?.let { user ->
        AuthUser(id = user.id, email = user.email.orEmpty())
    }
}
