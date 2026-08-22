# Supabase Auth — Slice A: Transport and Repository Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a `core:supabase` transport module and an `AuthRepository` in `core:data` that exposes session state and email/password auth to the rest of the app, with a fake for tests. No UI changes — the app builds and behaves exactly as before.

**Architecture:** `core:supabase` owns the `SupabaseClient` and its Hilt module, mirroring how `core:network` owns Retrofit. `core:data` gains `AuthRepository` (public interface) and `SupabaseAuthRepository` (`internal`, bound with `@Binds`), which maps supabase-kt's `sessionStatus` flow onto a `SessionState` from `core:model` and maps supabase exceptions onto a typed `AuthError`. No feature module ever sees a Supabase type.

**Tech Stack:** supabase-kt (BOM, `auth-kt`, `postgrest-kt`), Ktor OkHttp engine, kotlinx-serialization, Hilt, Kotlin coroutines/Flow, JUnit4 + MockK.

Spec: [`../specs/2026-08-22-supabase-auth-design.md`](../specs/2026-08-22-supabase-auth-design.md)

---

## Before you start

Two things must exist or every Gradle invocation in this plan fails, the same way a missing
`TMDB_API_KEY` fails today:

1. A Supabase project. Dashboard → Project Settings → API gives the **Project URL** and the
   **anon public** key.
2. Those two values in `local.properties` (git-ignored, never committed):

```properties
TMDB_API_KEY=<already there>
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<anon public key>
```

Dashboard settings this slice depends on: **Authentication → Sign In / Providers → uncheck
"Confirm email"**. With confirmation on, `signUpWith(Email)` returns a user but no session, and
Slice B's sign-up screen will appear to do nothing.

## File structure

| File | Responsibility |
| --- | --- |
| `gradle/libs.versions.toml` | version + library coordinates for supabase-kt and Ktor |
| `settings.gradle.kts` | include `:core:supabase` |
| `core/supabase/build.gradle.kts` | module config, `BuildConfig` fields from `local.properties` |
| `core/supabase/src/main/kotlin/.../core/supabase/di/SupabaseModule.kt` | provides the singleton `SupabaseClient` |
| `core/model/src/main/kotlin/.../core/model/AuthUser.kt` | `AuthUser`, `SessionState` |
| `core/model/src/main/kotlin/.../core/model/AuthResult.kt` | `AuthResult`, `AuthError` |
| `core/data/src/main/kotlin/.../core/data/repository/AuthRepository.kt` | public interface |
| `core/data/src/main/kotlin/.../core/data/repository/SupabaseAuthRepository.kt` | `internal` implementation + exception mapping |
| `core/data/src/main/kotlin/.../core/data/di/DataModule.kt` | `@Binds` for the new repository (file already exists) |
| `core/data/src/test/kotlin/.../core/data/repository/AuthErrorMappingTest.kt` | unit tests for the exception mapper |
| `core/testing/src/main/kotlin/.../core/testing/repository/TestAuthRepository.kt` | fake for ViewModel tests |

**One deliberate deviation from `core:network`:** `core:supabase` exposes supabase-kt with `api`,
not `implementation`, so `core:data` can call `client.auth` and catch `AuthRestException`.
supabase-kt *is* the client API; wrapping it in a DTO layer the way `core:network` wraps Retrofit
would be ceremony with one caller. `core:data` remains the only module that depends on
`core:supabase`, so the encapsulation that matters — features never see Supabase — still holds.

---

### Task 1: Version catalog entries

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add the versions**

In the `[versions]` block, after `datastore = "1.1.4"`:

```toml
supabase = "3.1.4"
ktor = "3.1.3"
```

- [ ] **Step 2: Add the libraries**

In the `[libraries]` block, after the `datastore-preferences` line:

```toml
# Supabase
supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
```

`supabase-auth` and `supabase-postgrest` carry no version on purpose — the BOM supplies it.

- [ ] **Step 3: Verify the catalog still parses**

Run: `./gradlew help -q`
Expected: no output and exit code 0. A malformed catalog fails here with
`Invalid catalog definition`.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add supabase-kt and ktor to the version catalog"
```

---

### Task 2: The `core:supabase` module

**Files:**
- Create: `core/supabase/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Register the module**

In `settings.gradle.kts`, add in alphabetical position (after `include(":core:network")`):

```kotlin
include(":core:supabase")
```

- [ ] **Step 2: Write the build file**

`core/supabase/build.gradle.kts` — the `local.properties` reading mirrors
`core/network/build.gradle.kts` exactly, including the fail-loud `require()`:

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    require(file.exists()) {
        "local.properties is missing; add SUPABASE_URL=<project url> and SUPABASE_ANON_KEY=<anon key>"
    }
    file.inputStream().use { load(it) }
}

val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL").orEmpty()
val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY").orEmpty()

require(supabaseUrl.isNotBlank()) { "SUPABASE_URL is missing from local.properties" }
require(supabaseAnonKey.isNotBlank()) { "SUPABASE_ANON_KEY is missing from local.properties" }

android {
    namespace = "com.practice.thenewmovies.core.supabase"

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // `api`, not `implementation`: core:data calls client.auth / client.from() directly and
    // catches supabase exception types. See the deviation note in the plan.
    api(platform(libs.supabase.bom))
    api(libs.supabase.auth)
    api(libs.supabase.postgrest)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 3: Verify it configures and the dependencies resolve**

Run: `./gradlew :core:supabase:dependencies --configuration debugCompileClasspath -q | grep -i supabase`
Expected: `io.github.jan-tennert.supabase:auth-kt` and `postgrest-kt` listed with version `3.1.4`
resolved from the BOM. If resolution fails, check the version against
https://central.sonatype.com/artifact/io.github.jan-tennert.supabase/bom and pin the newest
stable (non-`beta`, non-`rc`) release.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts core/supabase/build.gradle.kts
git commit -m "build(supabase): add the core:supabase module"
```

---

### Task 3: The Supabase client

**Files:**
- Create: `core/supabase/src/main/kotlin/com/practice/thenewmovies/core/supabase/di/SupabaseModule.kt`

Every new `.kt` file needs the Apache license header — `./gradlew spotlessApply` inserts it, so
write the file without it and let Spotless fix it in Step 3.

- [ ] **Step 1: Write the module**

```kotlin
package com.practice.thenewmovies.core.supabase.di

import com.practice.thenewmovies.core.supabase.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun providesSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        // Session persistence needs no code: Auth stores the session through
        // multiplatform-settings, which resolves to SharedPreferences on Android.
        install(Auth)
        install(Postgrest)
    }
}
```

Note this module is `object SupabaseModule`, not `internal object` — `core:data` lives in a
different module and Hilt needs to see the binding.

- [ ] **Step 2: Compile it**

Run: `./gradlew :core:supabase:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

If an import is unresolved, the package layout changed between supabase-kt releases. Do not
guess: run
`./gradlew :core:supabase:dependencies --configuration debugCompileClasspath` to confirm the
resolved version, then check that version's README at
https://github.com/supabase-community/supabase-kt for the correct import paths. In 2.x the auth
plugin lived at `io.github.jan.supabase.gotrue.GoTrue`; 3.x renamed it to
`io.github.jan.supabase.auth.Auth`.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add core/supabase
git commit -m "feat(supabase): provide the Supabase client with Auth and Postgrest"
```

---

### Task 4: Auth models

**Files:**
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/AuthUser.kt`
- Create: `core/model/src/main/kotlin/com/practice/thenewmovies/core/model/AuthResult.kt`

These are pure data declarations with no behaviour, so they get no tests of their own — the
repository and ViewModel tests in later tasks exercise them.

- [ ] **Step 1: Write `AuthUser.kt`**

```kotlin
package com.practice.thenewmovies.core.model

data class AuthUser(
    val id: String,
    val email: String,
)

sealed interface SessionState {
    /** The stored session has not been read yet. Render a spinner, never a login form. */
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val user: AuthUser) : SessionState
}
```

- [ ] **Step 2: Write `AuthResult.kt`**

```kotlin
package com.practice.thenewmovies.core.model

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(val error: AuthError) : AuthResult
}

enum class AuthError {
    InvalidCredentials,
    EmailAlreadyRegistered,
    WeakPassword,
    InvalidCode,
    Network,
    Unknown,
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :core:model:compileKotlin`
Expected: BUILD SUCCESSFUL. Note `core:model` is a JVM library (`themovies.jvm.library`), so it
has no `compileDebugKotlin` task — that name only exists on the Android modules.

- [ ] **Step 4: Format and commit**

```bash
./gradlew spotlessApply
git add core/model
git commit -m "feat(model): add AuthUser, SessionState, AuthResult and AuthError"
```

---

### Task 5: `AuthRepository` interface and the exception mapper

The mapper is the only part of this slice that can be unit-tested without a live server, so it is
a separate file and gets real tests.

**Files:**
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/AuthRepository.kt`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/AuthErrorMapping.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository/AuthErrorMappingTest.kt`
- Modify: `core/data/build.gradle.kts`

- [ ] **Step 1: Add the module dependency**

In `core/data/build.gradle.kts`, inside `dependencies`, after
`implementation(projects.core.network)`:

```kotlin
    implementation(projects.core.supabase)
```

- [ ] **Step 2: Write the interface**

`AuthRepository.kt`:

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits [SessionState.Loading] until the stored session has been read. */
    val sessionState: Flow<SessionState>

    suspend fun signUp(email: String, password: String): AuthResult

    suspend fun signIn(email: String, password: String): AuthResult

    /** Sends a recovery mail containing a 6-digit code. */
    suspend fun sendPasswordReset(email: String): AuthResult

    /**
     * Verifies the recovery code, then sets [newPassword] on the account.
     *
     * Verifying the code authenticates the session before the password is changed, so a failure
     * from the second step leaves the user signed in with their old password still valid —
     * callers must not assume [AuthResult.Failure] means nothing happened.
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult

    /**
     * Clears the session. Deliberately returns no result: a revoked or expired token makes the
     * server call fail while the local session is cleared anyway, and surfacing that would trap
     * the user in a signed-in shell they cannot leave.
     */
    suspend fun signOut()
}
```

- [ ] **Step 3: Write the failing test**

`AuthErrorMappingTest.kt`:

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.AuthError
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class AuthErrorMappingTest {

    @Test
    fun `an IO failure maps to Network`() {
        assertEquals(AuthError.Network, IOException("no route to host").toAuthError())
    }

    @Test
    fun `an unknown host maps to Network`() {
        assertEquals(AuthError.Network, UnknownHostException("db.supabase.co").toAuthError())
    }

    @Test
    fun `an unrecognised failure maps to Unknown`() {
        assertEquals(AuthError.Unknown, IllegalStateException("what").toAuthError())
    }

    @Test
    fun `cancellation is rethrown, never mapped`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("scope died").toAuthError()
        }
    }
}
```

The cancellation case matters: swallowing `CancellationException` into an `AuthError` would make
a cancelled ViewModel scope look like a failed login and leave the UI showing an error for a
request nobody is waiting for.

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*AuthErrorMappingTest*'`
Expected: FAIL — `Unresolved reference: toAuthError`.

- [ ] **Step 5: Write the mapper**

`AuthErrorMapping.kt`:

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.AuthError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Maps a failure from supabase-kt onto the app's error vocabulary.
 *
 * An unmapped or newly added supabase code falls through to [AuthError.Unknown]. A code this app
 * does map, that a future supabase-kt release renames or removes, will fail compilation on
 * purpose, so the mapping gets revisited instead of silently degrading to a generic message.
 * These paths are also walked by hand against the live project; see the checklist in Slice B.
 */
internal fun Throwable.toAuthError(): AuthError {
    if (this is CancellationException) throw this
    return when (this) {
        is AuthRestException -> when (errorCode) {
            AuthErrorCode.InvalidCredentials -> AuthError.InvalidCredentials
            AuthErrorCode.UserAlreadyExists,
            AuthErrorCode.EmailExists,
            -> AuthError.EmailAlreadyRegistered

            AuthErrorCode.WeakPassword -> AuthError.WeakPassword
            AuthErrorCode.OtpExpired, AuthErrorCode.OtpDisabled -> AuthError.InvalidCode
            else -> AuthError.Unknown
        }

        is IOException -> AuthError.Network
        else -> AuthError.Unknown
    }
}
```

Verified against auth-kt 3.1.4: `AuthRestException` is at that import, `errorCode` is a nullable
`AuthErrorCode`, and all six constants above exist (of roughly seventy). If one does not resolve,
the release renamed it — find the current name in the artifact rather than guessing.

**Also add tests for these six branches.** `AuthRestException(errorCode: String,
errorDescription: String, response: HttpResponse)` is public, `HttpResponse` has only abstract
accessors so `mockk<HttpResponse>(relaxed = true)` satisfies it, and `mockk` is already on this
module's test classpath — no live server needed. The constructor resolves the enum through
`AuthErrorCode.Companion.fromValue()` against **snake_case wire values**, so read the real values
out of the artifact instead of assuming them. Cover each mapped code, one real-but-unmapped code,
and one unrecognised string — the last two pin the `else` branch.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*AuthErrorMappingTest*'`
Expected: PASS, 4 tests.

- [ ] **Step 7: Format and commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): add AuthRepository and map supabase failures to AuthError"
```

---

### Task 6: `SupabaseAuthRepository`

**Files:**
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/SupabaseAuthRepository.kt`
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt`

- [ ] **Step 1: Write the implementation**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.common.network.Dispatcher
import com.practice.thenewmovies.core.common.network.MoviesDispatchers
import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseClient,
    @Dispatcher(MoviesDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val sessionState: Flow<SessionState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated ->
                // A session with no user record is unusable for anything the app needs, so it
                // is folded into the same SignedOut bucket as SessionStatus.NotAuthenticated.
                status.session.toAuthUser()?.let(SessionState::SignedIn) ?: SessionState.SignedOut

            is SessionStatus.Initializing -> SessionState.Loading
            is SessionStatus.NotAuthenticated -> SessionState.SignedOut
            is SessionStatus.RefreshFailure -> SessionState.SignedOut
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult = attempt {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = attempt {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = attempt {
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AuthResult = attempt {
        client.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = code)
        client.auth.updateUser { password = newPassword }
    }

    override suspend fun signOut() {
        // The result is dropped on purpose: a revoked or expired token fails the server call
        // while the local session is cleared anyway, and surfacing that would trap the user in a
        // signed-in shell. Going through attempt() rather than runCatching matters — runCatching
        // would swallow CancellationException too, breaking structured concurrency.
        attempt { client.auth.signOut() }
    }

    private suspend fun attempt(block: suspend () -> Unit): AuthResult =
        withContext(ioDispatcher) {
            try {
                block()
                AuthResult.Success
            } catch (exception: Exception) {
                AuthResult.Failure(exception.toAuthError())
            }
        }

    private fun UserSession.toAuthUser(): AuthUser? = user?.let { user ->
        AuthUser(id = user.id, email = user.email.orEmpty())
    }
}
```

Two notes for the engineer:

- The catch is `Exception`, not `Throwable`, on purpose: an `OutOfMemoryError` or a
  `NoClassDefFoundError` from a missing transitive dependency must not become
  `AuthError.Unknown` and invite the user to retry. `CancellationException` is a
  `RuntimeException`, so it is still caught here — and `toAuthError()` rethrows it before mapping
  anything, so cancellation still propagates.
- `signOut()` deliberately ignores failures. A revoked or already-expired token makes the server
  call fail while the local session is cleared anyway; surfacing that would trap the user in a
  signed-in shell they cannot leave. It routes through `attempt` and discards the result rather
  than using `runCatching`, which catches `CancellationException` as well and would swallow a
  cancelled scope.

- [ ] **Step 2: Compile and fix the `when` branches**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

`SessionStatus` is a sealed interface, so if a branch name is wrong the compiler prints the real
subtypes in its error (`'when' expression must be exhaustive, add necessary 'is X' branch`) —
use exactly the names it lists. supabase-kt 2.x named these `LoadingFromStorage` and
`NetworkError`; 3.x renamed them to `Initializing` and `RefreshFailure`. Map any
"still loading" state to `SessionState.Loading` and every failure state to
`SessionState.SignedOut`.

- [ ] **Step 3: Bind it**

In `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/di/DataModule.kt`, add the
import and the binding alongside the existing ones:

```kotlin
    @Binds
    internal abstract fun bindsAuthRepository(
        repository: SupabaseAuthRepository,
    ): AuthRepository
```

It goes inside the existing `internal abstract class DataModule`, next to
`bindsWatchlistRepository`. Add `import com.practice.thenewmovies.core.data.repository.AuthRepository`
and `...repository.SupabaseAuthRepository` to the imports.

- [ ] **Step 4: Verify Hilt can build the graph**

Run: `./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL. A `[Dagger/MissingBinding]` here means the `@Binds` did not land in
an aggregated module — check that `DataModule` is annotated `@InstallIn(SingletonComponent::class)`.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): implement AuthRepository against Supabase"
```

---

### Task 7: `TestAuthRepository`

Every auth ViewModel test in Slice B depends on this fake, so it ships here.

**Files:**
- Create: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestAuthRepository.kt`

- [ ] **Step 1: Write the fake**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.model.AuthError
import com.practice.thenewmovies.core.model.AuthResult
import com.practice.thenewmovies.core.model.AuthUser
import com.practice.thenewmovies.core.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestAuthRepository : AuthRepository {

    private val state = MutableStateFlow<SessionState>(SessionState.Loading)

    override val sessionState: Flow<SessionState> = state

    /** Set to non-null to make the next call of every method fail with this error. */
    var nextError: AuthError? = null

    val signUpCalls = mutableListOf<Pair<String, String>>()
    val signInCalls = mutableListOf<Pair<String, String>>()
    val resetEmails = mutableListOf<String>()
    val resetCalls = mutableListOf<Triple<String, String, String>>()
    var signOutCount = 0
        private set

    override suspend fun signUp(email: String, password: String): AuthResult {
        signUpCalls += email to password
        return result { emitSignedIn(email) }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += email to password
        return result { emitSignedIn(email) }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        resetEmails += email
        return result {}
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AuthResult {
        resetCalls += Triple(email, code, newPassword)
        return result {}
    }

    override suspend fun signOut() {
        signOutCount++
        state.value = SessionState.SignedOut
    }

    fun emitLoading() {
        state.value = SessionState.Loading
    }

    fun emitSignedOut() {
        state.value = SessionState.SignedOut
    }

    fun emitSignedIn(email: String = "user@example.com", id: String = "user-1") {
        state.value = SessionState.SignedIn(AuthUser(id = id, email = email))
    }

    private inline fun result(onSuccess: () -> Unit): AuthResult {
        val error = nextError
        return if (error == null) {
            onSuccess()
            AuthResult.Success
        } else {
            AuthResult.Failure(error)
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :core:testing:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. `core:testing` already declares `api(projects.core.data)`, so no
build-file change is needed.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add core/testing
git commit -m "test(testing): add TestAuthRepository fake"
```

---

### Task 8: Full verification

- [ ] **Step 1: Run everything**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — this includes `spotlessCheck`, lint, and every unit test.

- [ ] **Step 2: Confirm the app still runs unchanged**

Run: `./gradlew installDebug` and launch the app on a device or emulator.
Expected: Home loads exactly as before. This slice added no UI and no behaviour; a crash here
means the Hilt graph or the client construction is wrong, and the logcat stack trace names which.

- [ ] **Step 3: Confirm the unit test count moved**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS, including the 4 new `AuthErrorMappingTest` cases. If it reports 0 tests, you are
looking at an up-to-date task — add `--rerun-tasks`.

- [ ] **Step 4: Commit anything Spotless changed**

```bash
git status --short
git add -A && git commit -m "style: apply spotless" || echo "nothing to commit"
```

## Slice A done when

- `./gradlew build` is green
- `AuthRepository` is injectable anywhere in the app, and `sessionState` starts at `Loading`
- No feature module imports anything from `io.github.jan.supabase`
- The app's behaviour is byte-for-byte what it was before the slice

**Not verified yet, on purpose:** that sign-up, sign-in, and recovery actually work against the
live project. Nothing calls them until Slice B builds the screens, and the Moshi/R8 episode in
this directory's README is the standing reminder that a green build proves nothing about a real
call path. Slice B ends with a device checklist that covers it.
