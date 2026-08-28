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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ResetPasswordUiState(
    val code: String = "",
    val password: String = "",
    val codeError: FieldError? = null,
    val passwordError: FieldError? = null,
    val formError: AuthError? = null,
    val isSubmitting: Boolean = false,
)

@HiltViewModel(assistedFactory = ResetPasswordViewModel.Factory::class)
internal class ResetPasswordViewModel @AssistedInject constructor(
    @Assisted private val email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(email: String): ResetPasswordViewModel
    }

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        _uiState.update {
            it.copy(
                code = value.filter(Char::isDigit).take(CODE_LENGTH),
                codeError = null,
                formError = null,
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val codeError = validateCode(state.code)
        val passwordError = validatePassword(state.password)
        if (codeError != null || passwordError != null) {
            _uiState.update {
                it.copy(codeError = codeError, passwordError = passwordError, formError = null)
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.resetPassword(
                email = email,
                code = state.code,
                newPassword = state.password,
            )
            _uiState.update {
                when (result) {
                    AuthResult.Success -> it.copy(isSubmitting = false)
                    is AuthResult.Failure -> it.copy(isSubmitting = false, formError = result.error)
                }
            }
        }
    }
}
