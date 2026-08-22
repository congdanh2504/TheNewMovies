# Supabase Auth — Slice B: Auth UI and Session Gating Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put the app behind a login. Four auth screens (sign in, create account, forgot password, new password), a session-driven branch in `:app` that shows either the auth stack or the existing tabbed app, and a sign-out button.

**Architecture:** `feature:auth:api` holds four `NavKey`s; `feature:auth:impl` holds a screen + ViewModel per key, with all validation and submit state in the ViewModel so the screens stay stateless and previewable. `:app` observes `AuthRepository.sessionState` through a small `AppViewModel` and renders one of three things: a spinner while the stored session loads, an auth `NavDisplay` over its own back stack, or the existing tabbed `NavDisplay` untouched. Two separate back stacks mean sign-out cannot be back-navigated around and `core:navigation` needs no changes.

**Tech Stack:** Compose + Material 3, Navigation 3 (`NavDisplay`, `entryProvider`, `NavKey`), Hilt + `hiltViewModel()`, Turbine for ViewModel tests, Compose UI test for the login screen.

**Depends on:** Slice A (`AuthRepository`, `TestAuthRepository`, `SessionState`, `AuthResult`, `AuthError`).

Spec: [`../specs/2026-08-22-supabase-auth-design.md`](../specs/2026-08-22-supabase-auth-design.md)

---

## Before you start

Dashboard prerequisites for the flows this slice builds:

1. **Authentication → Sign In / Providers → uncheck "Confirm email".** Otherwise sign-up returns
   a user with no session and the screen appears to hang.
2. **Authentication → Email Templates → Reset Password:** replace the body's
   `{{ .ConfirmationURL }}` link with `{{ .Token }}`, for example:

   ```html
   <h2>Reset your password</h2>
   <p>Enter this code in the app:</p>
   <p><strong>{{ .Token }}</strong></p>
   ```

   Without this the recovery mail carries a link to a page that does not exist, and the code
   screen has nothing to type.

## File structure

| File | Responsibility |
| --- | --- |
| `core/designsystem/.../component/MoviesTextField.kt` | labelled text field, optional password toggle |
| `core/designsystem/.../icon/MoviesIcons.kt` | add `Logout` |
| `core/designsystem/src/main/res/drawable/core_designsystem_ic_logout.xml` | the icon |
| `feature/auth/api/build.gradle.kts` + `AuthNavKeys.kt` | four keys and their navigate extensions |
| `feature/auth/impl/build.gradle.kts` | module config |
| `feature/auth/impl/.../AuthFormState.kt` | shared form/validation state used by all four ViewModels |
| `feature/auth/impl/.../LoginViewModel.kt` + `LoginScreen.kt` | sign in |
| `feature/auth/impl/.../SignUpViewModel.kt` + `SignUpScreen.kt` | create account |
| `feature/auth/impl/.../ForgotPasswordViewModel.kt` + `ForgotPasswordScreen.kt` | request a code |
| `feature/auth/impl/.../ResetPasswordViewModel.kt` + `ResetPasswordScreen.kt` | code + new password |
| `feature/auth/impl/.../navigation/AuthEntries.kt` | the four `entry<>` blocks |
| `app/.../ui/AppViewModel.kt` | exposes `sessionState` |
| `app/.../ui/MoviesApp.kt` | the three-way branch |
| `app/.../ui/AuthHost.kt` | auth `NavDisplay` + its own back stack |

Screens live one-per-file next to their ViewModel, matching every existing feature.

---

### Task 1: `MoviesTextField`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/component/MoviesTextField.kt`

No test: it is a dumb component with no logic, and Task 6's Compose UI test exercises it through
the login screen.

- [ ] **Step 1: Write the component**

```kotlin
package com.practice.thenewmovies.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
fun MoviesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val hideText = isPassword && !visible

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = errorMessage != null,
            visualTransformation = if (hideText) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Text(
                            text = if (visible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MoviesTextFieldPreview() {
    MoviesTheme {
        Column {
            MoviesTextField(value = "user@example.com", onValueChange = {}, label = "Email")
            MoviesTextField(
                value = "hunter2",
                onValueChange = {},
                label = "Password",
                isPassword = true,
                errorMessage = "At least 6 characters",
            )
        }
    }
}
```

The visibility toggle uses a text label rather than an eye icon so no new drawable is needed for
it. Sign-out does need one — Task 2.

- [ ] **Step 2: Compile**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add MoviesTextField"
```

---

### Task 2: The sign-out icon

`MoviesTopAppBar` already takes `actionIcon: Int?` and `onActionClick`, so sign-out needs only a
drawable.

**Files:**
- Create: `core/designsystem/src/main/res/drawable/core_designsystem_ic_logout.xml`
- Modify: `core/designsystem/src/main/kotlin/com/practice/thenewmovies/core/designsystem/icon/MoviesIcons.kt`

- [ ] **Step 1: Add the drawable**

The filename must keep the `core_designsystem_` prefix — the library convention plugin sets
`resourcePrefix` from the module path and the build fails without it.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5zM4,5h8V3H4c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h8v-2H4V5z" />
</vector>
```

- [ ] **Step 2: Expose it**

```kotlin
object MoviesIcons {
    val Back = R.drawable.core_designsystem_ic_back_icon
    val Search = R.drawable.core_designsystem_ic_search_left
    val Logout = R.drawable.core_designsystem_ic_logout
}
```

- [ ] **Step 3: Verify the resource compiles**

Run: `./gradlew :core:designsystem:assembleDebug`
Expected: BUILD SUCCESSFUL. A prefix violation fails with
`Resource 'ic_logout' does not start with the specified prefix`.

- [ ] **Step 4: Format and commit**

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add the logout icon"
```

---

### Task 3: `feature:auth:api`

**Files:**
- Modify: `settings.gradle.kts`
- Create: `feature/auth/api/build.gradle.kts`
- Create: `feature/auth/api/src/main/kotlin/com/practice/thenewmovies/feature/auth/api/AuthNavKeys.kt`

- [ ] **Step 1: Register both modules now**

In `settings.gradle.kts`, before `include(":feature:detail:api")`:

```kotlin
include(":feature:auth:api")
include(":feature:auth:impl")
```

`:feature:auth:impl` gets its build file in Task 4. An include pointing at a directory with no
build file is a valid empty Gradle project, so builds between these two tasks still work.

- [ ] **Step 2: Write the api build file**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.api)
}

android {
    namespace = "com.practice.thenewmovies.feature.auth.api"
}
```

- [ ] **Step 3: Write the keys**

`AuthNavKeys.kt` — all four in one file, because they are one flow and always change together:

```kotlin
package com.practice.thenewmovies.feature.auth.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LoginNavKey : NavKey

@Serializable
data object SignUpNavKey : NavKey

@Serializable
data object ForgotPasswordNavKey : NavKey

@Serializable
data class ResetPasswordNavKey(val email: String) : NavKey
```

Unlike the other features these carry no `Navigator` extensions: auth runs on its own back stack
in `:app`, not through `Navigator`, which owns the per-tab stacks of the signed-in app.

- [ ] **Step 4: Compile**

Run: `./gradlew :feature:auth:api:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add settings.gradle.kts feature/auth/api
git commit -m "feat(auth): add the auth navigation keys"
```

---

### Task 4: `feature:auth:impl` module and shared form state

All four ViewModels share the same validation rules and the same submit/error handling, so that
lives in one small file instead of being copied four times.

**Files:**
- Create: `feature/auth/impl/build.gradle.kts`
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/AuthFormState.kt`
- Test: `feature/auth/impl/src/test/kotlin/com/practice/thenewmovies/feature/auth/impl/AuthValidationTest.kt`

- [ ] **Step 1: Write the build file**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.feature.impl)
}

android {
    namespace = "com.practice.thenewmovies.feature.auth.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.auth.api)
}
```

The feature-impl convention plugin already adds `core:ui`, `core:designsystem`, `core:model`,
lifecycle, `hilt-navigation-compose`, navigation3, Coil, and `testImplementation(core:testing)`.

- [ ] **Step 2: Write the failing test**

```kotlin
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
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*AuthValidationTest*'`
Expected: FAIL — `Unresolved reference: validateEmail`.

- [ ] **Step 4: Write `AuthFormState.kt`**

```kotlin
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
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*AuthValidationTest*'`
Expected: PASS, 8 tests.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add feature/auth/impl
git commit -m "feat(auth): add form validation and error messages"
```

---

### Task 5: `LoginViewModel`

**Files:**
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/LoginViewModel.kt`
- Test: `feature/auth/impl/src/test/kotlin/com/practice/thenewmovies/feature/auth/impl/LoginViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

No assertion is made about navigating away on success: nothing navigates. The session flow flips
to `SignedIn` and `:app` swaps the whole stack (Task 9).

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*LoginViewModelTest*'`
Expected: FAIL — `Unresolved reference: LoginViewModel`.

- [ ] **Step 3: Write the ViewModel**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val formError: AuthError? = null,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val email = state.email.trim()
        val emailError = validateEmail(email)
        val passwordError = validatePassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email = email, password = state.password)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    formError = (result as? AuthResult.Failure)?.error,
                )
            }
        }
    }
}
```

The `if (state.isSubmitting) return` guard is what stops a double tap from firing two sign-in
requests.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*LoginViewModelTest*'`
Expected: PASS, 6 tests.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add feature/auth/impl
git commit -m "feat(auth): add LoginViewModel"
```

---

### Task 6: `LoginScreen`

**Files:**
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/LoginScreen.kt`
- Test: `feature/auth/impl/src/androidTest/kotlin/com/practice/thenewmovies/feature/auth/impl/LoginScreenTest.kt`

- [ ] **Step 1: Write the screen**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.designsystem.component.MoviesTextField
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme

@Composable
internal fun LoginScreen(
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onSignUpClick = onSignUpClick,
        onForgotPasswordClick = onForgotPasswordClick,
        modifier = modifier,
    )
}

@Composable
internal fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))

        MoviesTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email",
            modifier = Modifier.testTag("login_email"),
            keyboardType = KeyboardType.Email,
            enabled = !uiState.isSubmitting,
            errorMessage = uiState.emailError?.message(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        MoviesTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            modifier = Modifier.testTag("login_password"),
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !uiState.isSubmitting,
            errorMessage = uiState.passwordError?.message(),
        )

        if (uiState.formError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.formError.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("login_error"),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_submit"),
            enabled = !uiState.isSubmitting,
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Sign in")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(text = "Forgot password?")
        }
        TextButton(
            onClick = onSignUpClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("login_to_signup"),
        ) {
            Text(text = "Create an account")
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    MoviesTheme {
        LoginScreen(
            uiState = LoginUiState(email = "user@example.com", password = "hunter2"),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onSignUpClick = {},
            onForgotPasswordClick = {},
        )
    }
}
```

`statusBarsPadding()` is required: the Scaffold in `:app` zeroes its content insets, so a screen
without it draws under the status bar.

- [ ] **Step 2: Write the UI test**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.practice.thenewmovies.core.designsystem.theme.MoviesTheme
import com.practice.thenewmovies.core.model.AuthError
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
        setScreen(LoginUiState(email = "user@example.com", password = "hunter2")) {
            submitted = true
        }

        composeTestRule.onNodeWithTag("login_submit").performClick()

        assert(submitted)
    }

    @Test
    fun tappingCreateAnAccountInvokesTheCallback() {
        var tapped = false
        setScreen(LoginUiState(), onSignUpClick = { tapped = true })

        composeTestRule.onNodeWithTag("login_to_signup").performClick()

        assert(tapped)
    }
}
```

These screens load no images, so unlike `feature:search:impl` this module needs no
`src/androidTest/AndroidManifest.xml` granting INTERNET. If you later add a Coil image to any
auth screen, add that manifest or the test APK dies with
`SecurityException: Permission denied (missing INTERNET permission?)`.

- [ ] **Step 3: Run the UI test on a connected device**

Run: `./gradlew :feature:auth:impl:connectedDebugAndroidTest`
Expected: PASS, 4 tests. Check
`feature/auth/impl/build/outputs/androidTest-results/connected/**/*.xml` and confirm
`tests="4"` — a run reporting `tests="0" failures="0"` means the runner found nothing, not that
everything passed.

- [ ] **Step 4: Format and commit**

```bash
./gradlew spotlessApply
git add feature/auth/impl
git commit -m "feat(auth): add the sign-in screen"
```

---

### Task 7: `SignUpViewModel` and `SignUpScreen`

**Files:**
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/SignUpViewModel.kt`
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/SignUpScreen.kt`
- Test: `feature/auth/impl/src/test/kotlin/com/practice/thenewmovies/feature/auth/impl/SignUpViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertTrue(!viewModel.uiState.value.isSubmitting)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*SignUpViewModelTest*'`
Expected: FAIL — `Unresolved reference: SignUpViewModel`.

- [ ] **Step 3: Write the ViewModel**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmationError: FieldError? = null,
    val formError: AuthError? = null,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
internal class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onConfirmationChange(value: String) {
        _uiState.update { it.copy(confirmation = value, confirmationError = null, formError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val email = state.email.trim()
        val emailError = validateEmail(email)
        val passwordError = validatePassword(state.password)
        val confirmationError = validateConfirmation(state.password, state.confirmation)
        if (emailError != null || passwordError != null || confirmationError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmationError = confirmationError,
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(email = email, password = state.password)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    formError = (result as? AuthResult.Failure)?.error,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*SignUpViewModelTest*'`
Expected: PASS, 3 tests.

- [ ] **Step 5: Write the screen**

`SignUpScreen.kt` — same shape as `LoginScreen`, three fields and a back affordance:

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
internal fun SignUpScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onSubmit = viewModel::onSubmit,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun SignUpScreen(
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
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
            title = "Create account",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MoviesTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.emailError?.message(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoviesTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = "Password",
                isPassword = true,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.passwordError?.message(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoviesTextField(
                value = uiState.confirmation,
                onValueChange = onConfirmationChange,
                label = "Confirm password",
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.confirmationError?.message(),
            )
            if (uiState.formError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.formError.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("signup_error"),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_submit"),
                enabled = !uiState.isSubmitting,
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(text = "Create account")
                }
            }
        }
    }
}

@Preview
@Composable
private fun SignUpScreenPreview() {
    MoviesTheme {
        SignUpScreen(
            uiState = SignUpUiState(email = "user@example.com"),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmationChange = {},
            onSubmit = {},
            onBackClick = {},
        )
    }
}
```

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add feature/auth/impl
git commit -m "feat(auth): add the create-account screen"
```

---

### Task 8: Password recovery — two ViewModels and two screens

**Files:**
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/ForgotPasswordViewModel.kt`
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/ForgotPasswordScreen.kt`
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/ResetPasswordViewModel.kt`
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/ResetPasswordScreen.kt`
- Test: `feature/auth/impl/src/test/kotlin/com/practice/thenewmovies/feature/auth/impl/RecoveryViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*RecoveryViewModelTest*'`
Expected: FAIL — `Unresolved reference: ForgotPasswordViewModel`.

- [ ] **Step 3: Write `ForgotPasswordViewModel.kt`**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
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
            _uiState.update { it.copy(emailError = emailError) }
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
```

- [ ] **Step 4: Write `ResetPasswordViewModel.kt`**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val code: String = "",
    val password: String = "",
    val codeError: FieldError? = null,
    val passwordError: FieldError? = null,
    val formError: AuthError? = null,
    val isSubmitting: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
internal class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val email: String = requireNotNull(savedStateHandle["email"]) {
        "ResetPasswordViewModel needs an email in its SavedStateHandle"
    }

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        _uiState.update {
            it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), codeError = null, formError = null)
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
            _uiState.update { it.copy(codeError = codeError, passwordError = passwordError) }
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
                    AuthResult.Success -> it.copy(isSubmitting = false, done = true)
                    is AuthResult.Failure -> it.copy(isSubmitting = false, formError = result.error)
                }
            }
        }
    }
}
```

A successful reset sets `done`, but a signed-in session is what actually moves the user on:
`verifyEmailOtp` authenticates the account, so `sessionState` flips to `SignedIn` and `:app`
replaces the auth stack. `done` exists so the screen can show a confirmation in the
one-frame gap and for the test to assert on.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :feature:auth:impl:testDebugUnitTest --tests '*RecoveryViewModelTest*'`
Expected: PASS, 6 tests.

- [ ] **Step 6: Write `ForgotPasswordScreen.kt`**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
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
        val email = uiState.sentTo
        if (email != null) {
            onCodeSent(email)
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MoviesTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = "Email",
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
                Text(text = "Send code")
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
```

`onNavigated()` is called right after `onCodeSent`, so returning to this screen with back does
not immediately push the code screen again.

- [ ] **Step 7: Write `ResetPasswordScreen.kt`**

```kotlin
package com.practice.thenewmovies.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
internal fun ResetPasswordScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ResetPasswordScreen(
        uiState = uiState,
        onCodeChange = viewModel::onCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun ResetPasswordScreen(
    uiState: ResetPasswordUiState,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
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
            title = "New password",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MoviesTextField(
                value = uiState.code,
                onValueChange = onCodeChange,
                label = "6-digit code",
                keyboardType = KeyboardType.NumberPassword,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.codeError?.message(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            MoviesTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = "New password",
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.passwordError?.message(),
            )
            if (uiState.formError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.formError.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_submit"),
                enabled = !uiState.isSubmitting,
            ) {
                Text(text = "Set password")
            }
        }
    }
}

@Preview
@Composable
private fun ResetPasswordScreenPreview() {
    MoviesTheme {
        ResetPasswordScreen(
            uiState = ResetPasswordUiState(code = "123456"),
            onCodeChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onBackClick = {},
        )
    }
}
```

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply
git add feature/auth/impl
git commit -m "feat(auth): add password recovery by emailed code"
```

---

### Task 9: Wire the auth stack and gate the app

**Files:**
- Create: `feature/auth/impl/src/main/kotlin/com/practice/thenewmovies/feature/auth/impl/navigation/AuthEntries.kt`
- Create: `app/src/main/kotlin/com/practice/thenewmovies/ui/AppViewModel.kt`
- Create: `app/src/main/kotlin/com/practice/thenewmovies/ui/AuthHost.kt`
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Write the entries**

```kotlin
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
    entry<ResetPasswordNavKey> {
        ResetPasswordScreen(onBackClick = onBack)
    }
}
```

`ResetPasswordViewModel` reads `email` from its `SavedStateHandle`; Navigation 3's ViewModel
decorator puts the `@Serializable` key's properties there, which is why the key's property is
named `email` and the handle is read with the same string.

- [ ] **Step 2: Add the app dependencies**

In `app/build.gradle.kts`, inside `dependencies`:

```kotlin
    implementation(projects.core.data)
    implementation(projects.feature.auth.api)
    implementation(projects.feature.auth.impl)
```

`core:data` is new for `:app` and is what lets `AppViewModel` inject `AuthRepository` — the same
arrangement Now in Android uses.

- [ ] **Step 3: Write `AppViewModel`**

```kotlin
package com.practice.thenewmovies.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading,
        )
}
```

`SharingStarted.Eagerly`, not `WhileSubscribed`: the session must not be re-read every time the
app is backgrounded and resumed, which would flash the spinner.

- [ ] **Step 4: Write `AuthHost`**

```kotlin
package com.practice.thenewmovies.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.practice.thenewmovies.feature.auth.api.LoginNavKey
import com.practice.thenewmovies.feature.auth.impl.navigation.authEntries

@Composable
fun AuthHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(LoginNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            authEntries(
                onNavigate = { key -> backStack.add(key) },
                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            )
        },
    )
}
```

At `LoginNavKey` the stack has one entry, so `NavDisplay` does not dispatch back and the system
back press exits the app — which is correct here: there is nothing behind a login screen. That is
the same `NavDisplay` behaviour the tabbed app works around with a `BackHandler`.

- [ ] **Step 5: Branch in `MoviesApp`**

Rename the existing `MoviesApp` body to `SignedInApp` and add the branch. In
`app/src/main/kotlin/com/practice/thenewmovies/ui/MoviesApp.kt`:

```kotlin
@Composable
fun MoviesApp(viewModel: AppViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        SessionState.Loading -> LoadingScreen()
        SessionState.SignedOut -> AuthHost()
        is SessionState.SignedIn -> SignedInApp()
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SignedInApp() {
    // ...the entire previous body of MoviesApp, unchanged...
}
```

Five imports to add — `Box`, `background`, `fillMaxSize`, `MaterialTheme`, `Alignment` and
`Modifier` are already there for the bottom bar:

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.practice.thenewmovies.core.model.SessionState
```

- [ ] **Step 6: Verify the graph and build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. A `MissingBinding` for `AuthRepository` means `:app` is missing the
`core:data` dependency from Step 2.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add app feature/auth/impl
git commit -m "feat(app): gate the app on the Supabase session"
```

---

### Task 10: Sign out

**Files:**
- Modify: `feature/watchlist/impl/src/main/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistViewModel.kt`
- Modify: `feature/watchlist/impl/src/main/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistScreen.kt`
- Test: `feature/watchlist/impl/src/test/kotlin/com/practice/thenewmovies/feature/watchlist/impl/WatchlistViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Add to the existing `WatchlistViewModelTest`:

```kotlin
    @Test
    fun `signing out delegates to the auth repository`() = runTest {
        val authRepository = TestAuthRepository()
        val viewModel = WatchlistViewModel(
            watchlistRepository = TestWatchlistRepository(),
            authRepository = authRepository,
        )

        viewModel.onSignOutClick()

        assertEquals(1, authRepository.signOutCount)
    }
```

Add the imports it needs: `com.practice.thenewmovies.core.testing.repository.TestAuthRepository`
and, if the existing tests construct the ViewModel positionally, update those call sites to pass
the new argument.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :feature:watchlist:impl:testDebugUnitTest`
Expected: FAIL — the constructor takes one argument, and `onSignOutClick` does not exist.

- [ ] **Step 3: Extend the ViewModel**

```kotlin
@HiltViewModel
internal class WatchlistViewModel @Inject constructor(
    watchlistRepository: WatchlistRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<WatchlistUiState> = watchlistRepository.getWatchlist()
        .map { movies ->
            if (movies.isEmpty()) WatchlistUiState.Empty else WatchlistUiState.Success(movies)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WatchlistUiState.Loading,
        )

    fun onSignOutClick() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
```

New imports: `com.practice.thenewmovies.core.data.repository.AuthRepository` and
`kotlinx.coroutines.launch`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :feature:watchlist:impl:testDebugUnitTest`
Expected: PASS, including the existing tests.

- [ ] **Step 5: Add the button**

In `WatchlistScreen.kt`, pass the action through to the existing top bar parameters:

```kotlin
        MoviesTopAppBar(
            title = "Watch list",
            onBackClick = onBackClick,
            modifier = Modifier.padding(top = 16.dp),
            actionIcon = MoviesIcons.Logout,
            onActionClick = onSignOutClick,
        )
```

Thread `onSignOutClick: () -> Unit` through both `WatchlistScreen` overloads — the stateful one
passes `viewModel::onSignOutClick`, and the two `@Preview` functions pass `{}`. Add
`import com.practice.thenewmovies.core.designsystem.icon.MoviesIcons`.

- [ ] **Step 6: Build and commit**

```bash
./gradlew :feature:watchlist:impl:assembleDebug
./gradlew spotlessApply
git add feature/watchlist/impl
git commit -m "feat(watchlist): add sign out to the top bar"
```

---

### Task 11: Full verification, on a device

- [ ] **Step 1: Run everything**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run the instrumented tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: PASS. Confirm the counts in
`*/build/outputs/androidTest-results/connected/**/*.xml` — `tests="0"` is a failure to find
tests, not a pass.

- [ ] **Step 3: Install and walk the real flows**

Run: `./gradlew installDebug`

This is the step that actually proves Slice A's Supabase calls work. Nothing before it does.
Walk all of it against the live project:

- [ ] Cold start with no session → brief spinner, then the sign-in screen. No flash of the tabbed app.
- [ ] Create an account with a fresh address → lands straight in Home. Confirm the row exists in
      Dashboard → Authentication → Users.
- [ ] Force-quit and relaunch → straight into Home, no login. This proves session persistence.
- [ ] Sign out from the Watch List tab → back to sign-in. Press back → the app exits; it does not
      return to Home.
- [ ] Sign in with the wrong password → "Wrong email or password", not "Something went wrong". A
      generic message here means `toAuthError`'s code names do not match this supabase-kt release;
      log the exception and fix the mapping.
- [ ] Sign up again with the same address → "That email already has an account".
- [ ] Forgot password → the code arrives by email → enter it with a new password → signed in.
      Then sign out and sign in with the new password.
- [ ] Enter a wrong code → "That code is wrong or has expired".
- [ ] Turn off Wi-Fi and mobile data, try to sign in → "No connection", not a crash.

- [ ] **Step 4: Commit anything Spotless changed**

```bash
git status --short
git add -A && git commit -m "style: apply spotless" || echo "nothing to commit"
```

## Slice B done when

- `./gradlew build` and `connectedDebugAndroidTest` are green
- Every box in Task 11 Step 3 is checked against the live Supabase project
- The watchlist still works exactly as before — it is still device-local until Slice C
