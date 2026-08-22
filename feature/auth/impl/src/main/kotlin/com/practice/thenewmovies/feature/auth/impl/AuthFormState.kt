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

enum class FieldError {
    EmailRequired,
    EmailInvalid,
    PasswordTooShort,
    PasswordMismatch,
    CodeInvalid,
}

internal const val MIN_PASSWORD_LENGTH = 6
internal const val CODE_LENGTH = 6

internal fun validateEmail(email: String): FieldError? = when {
    email.isBlank() -> FieldError.EmailRequired
    !email.trim().contains('@') -> FieldError.EmailInvalid
    else -> null
}

internal fun validatePassword(password: String): FieldError? =
    if (password.length < MIN_PASSWORD_LENGTH) FieldError.PasswordTooShort else null

internal fun validateConfirmation(password: String, confirmation: String): FieldError? =
    if (password != confirmation) FieldError.PasswordMismatch else null

internal fun validateCode(code: String): FieldError? =
    if (code.length != CODE_LENGTH || !code.all { it.isDigit() }) FieldError.CodeInvalid else null

/** Human text for the UI. Kept next to the errors so no screen invents its own wording. */
fun FieldError.message(): String = when (this) {
    FieldError.EmailRequired -> "Enter your email"
    FieldError.EmailInvalid -> "That does not look like an email address"
    FieldError.PasswordTooShort -> "At least $MIN_PASSWORD_LENGTH characters"
    FieldError.PasswordMismatch -> "The passwords do not match"
    FieldError.CodeInvalid -> "Enter the $CODE_LENGTH-digit code from the email"
}

fun AuthError.message(): String = when (this) {
    AuthError.InvalidCredentials -> "Wrong email or password"
    AuthError.EmailAlreadyRegistered -> "That email already has an account"
    AuthError.WeakPassword -> "Pick a stronger password"
    AuthError.InvalidCode -> "That code is wrong or has expired"
    AuthError.Network -> "No connection. Check your network and try again."
    AuthError.Unknown -> "Something went wrong. Try again."
}
