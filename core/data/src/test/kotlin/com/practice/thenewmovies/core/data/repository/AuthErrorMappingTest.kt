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
}
