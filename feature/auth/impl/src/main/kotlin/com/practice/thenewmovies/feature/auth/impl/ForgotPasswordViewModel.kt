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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.auth.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val formError: AuthError? = null,
    val isSubmitting: Boolean = false,
    /** Non-null once a code has been mailed; the screen navigates on it. */
    val sentTo: String? = null,
)

@HiltViewModel
internal class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val email = state.email.trim()
        val emailError = validateEmail(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError, formError = null) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(email)) {
                AuthResult.Success ->
                    _uiState.update { it.copy(isSubmitting = false, sentTo = email) }

                is AuthResult.Failure ->
                    _uiState.update { it.copy(isSubmitting = false, formError = result.error) }
            }
        }
    }

    /** Called after the screen has navigated, so returning does not navigate again. */
    fun onNavigated() {
        _uiState.update { it.copy(sentTo = null) }
    }
}
