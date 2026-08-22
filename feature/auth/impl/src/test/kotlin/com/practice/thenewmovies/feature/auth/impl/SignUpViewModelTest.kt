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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = TestAuthRepository()

    private fun viewModel() = SignUpViewModel(authRepository)

    @Test
    fun `a mismatched confirmation never reaches the repository`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.onConfirmationChange("hunter3")

        viewModel.onSubmit()

        assertEquals(
            FieldError.PasswordMismatch,
            viewModel.uiState.value.confirmationError,
        )
        assertTrue(authRepository.signUpCalls.isEmpty())
    }

    @Test
    fun `a valid submit calls signUp`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.onConfirmationChange("hunter2")

        viewModel.onSubmit()

        assertEquals(listOf("user@example.com" to "hunter2"), authRepository.signUpCalls)
    }

    @Test
    fun `an already registered email surfaces as a form error`() = runTest {
        authRepository.nextError = AuthError.EmailAlreadyRegistered
        val viewModel = viewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.onConfirmationChange("hunter2")

        viewModel.onSubmit()

        assertEquals(AuthError.EmailAlreadyRegistered, viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }
}
