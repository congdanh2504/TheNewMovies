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
package com.practice.thenewmovies.feature.auth.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.practice.thenewmovies.feature.auth.api.ForgotPasswordNavKey
import com.practice.thenewmovies.feature.auth.api.LoginNavKey
import com.practice.thenewmovies.feature.auth.api.ResetPasswordNavKey
import com.practice.thenewmovies.feature.auth.api.SignUpNavKey
import com.practice.thenewmovies.feature.auth.impl.ForgotPasswordScreen
import com.practice.thenewmovies.feature.auth.impl.LoginScreen
import com.practice.thenewmovies.feature.auth.impl.ResetPasswordScreen
import com.practice.thenewmovies.feature.auth.impl.SignUpScreen

fun EntryProviderScope<NavKey>.authEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    entry<LoginNavKey> {
        LoginScreen(
            onSignUpClick = { onNavigate(SignUpNavKey) },
            onForgotPasswordClick = { onNavigate(ForgotPasswordNavKey) },
        )
    }
    entry<SignUpNavKey> {
        SignUpScreen(onBackClick = onBack)
    }
    entry<ForgotPasswordNavKey> {
        ForgotPasswordScreen(
            onBackClick = onBack,
            onCodeSent = { email -> onNavigate(ResetPasswordNavKey(email)) },
        )
    }
    entry<ResetPasswordNavKey> { key ->
        ResetPasswordScreen(email = key.email, onBackClick = onBack)
    }
}
