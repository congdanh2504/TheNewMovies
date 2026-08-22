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

import com.practice.thenewmovies.core.model.AuthError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Maps a failure from supabase-kt onto the app's error vocabulary.
 *
 * An unmapped or newly added error code falls through to [AuthError.Unknown]. Matching is done
 * on the [AuthErrorCode] constants directly, on purpose: a code this app maps that a future
 * supabase-kt release removes or renames fails compilation, so the mapping gets revisited rather
 * than silently degrading. The exact codes are verified by hand against the live project; see the
 * verification checklist in Slice B.
 */
internal fun Throwable.toAuthError(): AuthError {
    if (this is CancellationException) throw this
    return when (this) {
        is AuthRestException -> when (errorCode) {
            AuthErrorCode.InvalidCredentials -> AuthError.InvalidCredentials
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AuthError.EmailAlreadyRegistered
            AuthErrorCode.WeakPassword -> AuthError.WeakPassword
            AuthErrorCode.OtpExpired, AuthErrorCode.OtpDisabled -> AuthError.InvalidCode
            else -> AuthError.Unknown
        }

        is IOException -> AuthError.Network

        else -> AuthError.Unknown
    }
}
