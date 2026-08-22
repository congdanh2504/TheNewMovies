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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.AuthError
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        uiState: LoginUiState,
        onSubmit: () -> Unit = {},
        onSignUpClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MoviesTheme {
                LoginScreen(
                    uiState = uiState,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = onSubmit,
                    onSignUpClick = onSignUpClick,
                    onForgotPasswordClick = {},
                )
            }
        }
    }

    @Test
    fun theFormErrorIsShown() {
        setScreen(LoginUiState(formError = AuthError.InvalidCredentials))

        composeTestRule.onNodeWithTag("login_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wrong email or password").assertIsDisplayed()
    }

    @Test
    fun theSubmitButtonIsDisabledWhileSubmitting() {
        setScreen(LoginUiState(isSubmitting = true))

        composeTestRule.onNodeWithTag("login_submit").assertIsNotEnabled()
    }

    @Test
    fun tappingSubmitInvokesTheCallback() {
        var submitted = false
        setScreen(
            LoginUiState(email = "user@example.com", password = "hunter2"),
            onSubmit = { submitted = true },
        )

        composeTestRule.onNodeWithTag("login_submit").performClick()

        assertTrue("onSubmit was not invoked", submitted)
    }

    @Test
    fun tappingCreateAnAccountInvokesTheCallback() {
        var tapped = false
        setScreen(LoginUiState(), onSignUpClick = { tapped = true })

        composeTestRule.onNodeWithTag("login_to_signup").performClick()

        assertTrue("onSignUpClick was not invoked", tapped)
    }
}
