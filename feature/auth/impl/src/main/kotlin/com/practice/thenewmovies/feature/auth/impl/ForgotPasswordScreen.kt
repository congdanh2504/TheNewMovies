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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesTextField
import com.practice.thenewmovies.core.designsystem.component.MoviesTopAppBar
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
internal fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onCodeSent: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.sentTo) {
        val sentTo = uiState.sentTo
        if (sentTo != null) {
            onCodeSent(sentTo)
            viewModel.onNavigated()
        }
    }

    ForgotPasswordScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        MoviesTopAppBar(
            title = "Forgot password",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "We will email you a 6-digit code.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            MoviesTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = "Email",
                modifier = Modifier.testTag("forgot_email"),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.emailError?.message(),
            )

            if (uiState.formError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.formError.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("forgot_error"),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forgot_submit"),
                enabled = !uiState.isSubmitting,
            ) {
                if (uiState.isSubmitting) {
                    // A CircularProgressIndicator chains `.size(CircularIndicatorDiameter)` after
                    // the modifier it is given, so constraining only height leaves it 40dp wide
                    // and squished to 20dp tall. Constrain both dimensions to keep it a circle.
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(text = "Send code")
                }
            }
        }
    }
}

@Preview
@Composable
private fun ForgotPasswordScreenPreview() {
    MoviesTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(email = "user@example.com"),
            onEmailChange = {},
            onSubmit = {},
            onBackClick = {},
        )
    }
}
