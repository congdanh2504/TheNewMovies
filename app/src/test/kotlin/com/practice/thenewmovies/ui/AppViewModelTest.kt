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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in the loading state`() = runTest {
        val viewModel = AppViewModel(TestAuthRepository())

        assertEquals(SessionState.Loading, viewModel.sessionState.value)
    }

    @Test
    fun `signing out delegates to the auth repository`() = runTest {
        val authRepository = TestAuthRepository()
        val viewModel = AppViewModel(authRepository)

        viewModel.signOut()

        assertEquals(1, authRepository.signOutCount)
    }
}
