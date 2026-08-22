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

import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = TestAuthRepository()

    private fun viewModel() = LoginViewModel(authRepository)

    @Test
    fun `starts empty with no errors`() {
        val state = viewModel().uiState.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.formError)
        assertTrue(!state.isSubmitting)
    }

    @Test
    fun `submitting a blank email never reaches the repository`() = runTest {
        val viewModel = viewModel()

        viewModel.onSubmit()

        assertEquals(FieldError.EmailRequired, viewModel.uiState.value.emailError)
        assertTrue(authRepository.signInCalls.isEmpty())
    }

    @Test
    fun `submitting a short password never reaches the repository`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("123")

        viewModel.onSubmit()

        assertEquals(FieldError.PasswordTooShort, viewModel.uiState.value.passwordError)
        assertTrue(authRepository.signInCalls.isEmpty())
    }

    @Test
    fun `a valid submit calls the repository with the trimmed email`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("  user@example.com  ")
        viewModel.onPasswordChange("hunter2")

        viewModel.onSubmit()

        assertEquals(listOf("user@example.com" to "hunter2"), authRepository.signInCalls)
    }

    @Test
    fun `a failure surfaces as a form error and clears submitting`() = runTest {
        authRepository.nextError = AuthError.InvalidCredentials
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(AuthError.InvalidCredentials, state.formError)
        assertTrue(!state.isSubmitting)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest {
        authRepository.nextError = AuthError.InvalidCredentials
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.onSubmit()

        viewModel.onPasswordChange("hunter3")

        assertNull(viewModel.uiState.value.formError)
    }
}
