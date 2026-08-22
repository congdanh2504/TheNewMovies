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

import androidx.lifecycle.SavedStateHandle
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = TestAuthRepository()

    @Test
    fun `forgot password rejects a bad email without calling the repository`() = runTest {
        val viewModel = ForgotPasswordViewModel(authRepository)
        viewModel.onEmailChange("nope")

        viewModel.onSubmit()

        assertEquals(FieldError.EmailInvalid, viewModel.uiState.value.emailError)
        assertTrue(authRepository.resetEmails.isEmpty())
    }

    @Test
    fun `forgot password reports the email it sent to`() = runTest {
        val viewModel = ForgotPasswordViewModel(authRepository)
        viewModel.onEmailChange("user@example.com")

        viewModel.onSubmit()

        assertEquals(listOf("user@example.com"), authRepository.resetEmails)
        assertEquals("user@example.com", viewModel.uiState.value.sentTo)
    }

    @Test
    fun `forgot password surfaces a failure and reports no send`() = runTest {
        authRepository.nextError = AuthError.Network
        val viewModel = ForgotPasswordViewModel(authRepository)
        viewModel.onEmailChange("user@example.com")

        viewModel.onSubmit()

        assertEquals(AuthError.Network, viewModel.uiState.value.formError)
        assertEquals(null, viewModel.uiState.value.sentTo)
    }

    @Test
    fun `reset rejects a five digit code`() = runTest {
        val viewModel = resetViewModel()
        viewModel.onCodeChange("12345")
        viewModel.onPasswordChange("hunter2")

        viewModel.onSubmit()

        assertEquals(FieldError.CodeInvalid, viewModel.uiState.value.codeError)
        assertTrue(authRepository.resetCalls.isEmpty())
    }

    @Test
    fun `reset passes the email from the nav key`() = runTest {
        val viewModel = resetViewModel(email = "someone@example.com")
        viewModel.onCodeChange("123456")
        viewModel.onPasswordChange("hunter2")

        viewModel.onSubmit()

        assertEquals(
            listOf(Triple("someone@example.com", "123456", "hunter2")),
            authRepository.resetCalls,
        )
    }

    @Test
    fun `reset reports a wrong code`() = runTest {
        authRepository.nextError = AuthError.InvalidCode
        val viewModel = resetViewModel()
        viewModel.onCodeChange("123456")
        viewModel.onPasswordChange("hunter2")

        viewModel.onSubmit()

        assertEquals(AuthError.InvalidCode, viewModel.uiState.value.formError)
    }

    private fun resetViewModel(email: String = "user@example.com") = ResetPasswordViewModel(
        savedStateHandle = SavedStateHandle(mapOf("email" to email)),
        authRepository = authRepository,
    )
}
