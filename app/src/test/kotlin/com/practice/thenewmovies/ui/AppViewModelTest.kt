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
package com.practice.thenewmovies.ui

import com.practice.thenewmovies.core.model.SessionState
import com.practice.thenewmovies.core.testing.MainDispatcherRule
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import com.practice.thenewmovies.core.testing.repository.TestWatchlistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in the loading state`() = runTest {
        val viewModel = AppViewModel(
            authRepository = TestAuthRepository(),
            watchlistRepository = TestWatchlistRepository(),
        )

        assertEquals(SessionState.Loading, viewModel.sessionState.value)
    }

    @Test
    fun `signing out delegates to the auth repository`() = runTest {
        val authRepository = TestAuthRepository()
        val viewModel = AppViewModel(
            authRepository = authRepository,
            watchlistRepository = TestWatchlistRepository(),
        )

        viewModel.signOut()

        assertEquals(1, authRepository.signOutCount)
    }

    @Test
    fun `a session beginning triggers one sync`() = runTest {
        val authRepository = TestAuthRepository()
        val watchlistRepository = TestWatchlistRepository()
        val viewModel = AppViewModel(
            authRepository = authRepository,
            watchlistRepository = watchlistRepository,
        )

        authRepository.emitSignedIn(id = "user-1")

        assertEquals(1, watchlistRepository.syncCount)
    }

    @Test
    fun `a token refresh does not sync again`() = runTest {
        // Supabase re-emits SignedIn(same id) on every token refresh, with no SignedOut
        // in between. A sign-out cycle for the same user is a different case, covered by
        // `signing back in as the same user syncs again` below.
        val authRepository = TestAuthRepository()
        val watchlistRepository = TestWatchlistRepository()
        val viewModel = AppViewModel(
            authRepository = authRepository,
            watchlistRepository = watchlistRepository,
        )

        authRepository.emitSignedIn(id = "user-1")
        authRepository.emitSignedIn(id = "user-1")

        assertEquals(1, watchlistRepository.syncCount)
    }

    @Test
    fun `signing back in as the same user syncs again`() = runTest {
        val authRepository = TestAuthRepository()
        val watchlistRepository = TestWatchlistRepository()
        val viewModel = AppViewModel(
            authRepository = authRepository,
            watchlistRepository = watchlistRepository,
        )

        authRepository.emitSignedIn(id = "user-1")
        authRepository.emitSignedOut()
        authRepository.emitSignedIn(id = "user-1")

        assertEquals(2, watchlistRepository.syncCount)
    }

    @Test
    fun `a different user syncs again`() = runTest {
        val authRepository = TestAuthRepository()
        val watchlistRepository = TestWatchlistRepository()
        val viewModel = AppViewModel(
            authRepository = authRepository,
            watchlistRepository = watchlistRepository,
        )

        authRepository.emitSignedIn(id = "user-1")
        authRepository.emitSignedIn(id = "user-2")

        assertEquals(2, watchlistRepository.syncCount)
    }
}
