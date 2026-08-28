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

import com.practice.thenewmovies.core.model.AuthError
import io.github.jan.supabase.auth.exception.AuthRestException
import io.ktor.client.statement.HttpResponse
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class AuthErrorMappingTest {

    @Test
    fun `an IO failure maps to Network`() {
        assertEquals(AuthError.Network, IOException("no route to host").toAuthError())
    }

    @Test
    fun `an unknown host maps to Network`() {
        assertEquals(AuthError.Network, UnknownHostException("db.supabase.co").toAuthError())
    }

    @Test
    fun `an unrecognised failure maps to Unknown`() {
        assertEquals(AuthError.Unknown, IllegalStateException("what").toAuthError())
    }

    @Test
    fun `cancellation is rethrown, never mapped`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("scope died").toAuthError()
        }
    }

    @Test
    fun `each supabase error code maps to the expected AuthError`() {
        val response = mockk<HttpResponse>(relaxed = true)
        val cases = listOf(
            // the six codes this app maps
            "invalid_credentials" to AuthError.InvalidCredentials,
            "user_already_exists" to AuthError.EmailAlreadyRegistered,
            "email_exists" to AuthError.EmailAlreadyRegistered,
            "weak_password" to AuthError.WeakPassword,
            "otp_expired" to AuthError.InvalidCode,
            "otp_disabled" to AuthError.InvalidCode,
            // a real but unmapped code: pins the `else` branch of the inner `when`
            "session_expired" to AuthError.Unknown,
            // a code string supabase never sends: pins the null-errorCode path
            "not_a_real_code" to AuthError.Unknown,
        )

        cases.forEach { (wireValue, expected) ->
            val exception = AuthRestException(wireValue, "description", response)
            assertEquals("wireValue=$wireValue", expected, exception.toAuthError())
        }
    }
}
