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
package com.practice.thenewmovies.feature.auth.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `a blank email is rejected`() {
        assertEquals(FieldError.EmailRequired, validateEmail(""))
        assertEquals(FieldError.EmailRequired, validateEmail("   "))
    }

    @Test
    fun `an email without an at sign is rejected`() {
        assertEquals(FieldError.EmailInvalid, validateEmail("nope"))
    }

    @Test
    fun `a plausible email is accepted`() {
        assertNull(validateEmail("user@example.com"))
    }

    @Test
    fun `a bare at sign passes because the server is the authority`() {
        assertNull(validateEmail("@"))
    }

    @Test
    fun `a short password is rejected`() {
        assertEquals(FieldError.PasswordTooShort, validatePassword("12345"))
    }

    @Test
    fun `a six character password is accepted`() {
        assertNull(validatePassword("123456"))
    }

    @Test
    fun `a mismatched confirmation is rejected`() {
        assertEquals(FieldError.PasswordMismatch, validateConfirmation("abcdef", "abcdeg"))
    }

    @Test
    fun `a matching confirmation is accepted`() {
        assertNull(validateConfirmation("abcdef", "abcdef"))
    }

    @Test
    fun `a code must be six digits`() {
        assertEquals(FieldError.CodeInvalid, validateCode("123"))
        assertEquals(FieldError.CodeInvalid, validateCode("12345a"))
        assertNull(validateCode("123456"))
    }
}
