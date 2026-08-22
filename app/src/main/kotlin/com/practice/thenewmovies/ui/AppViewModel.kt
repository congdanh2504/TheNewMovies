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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading,
        )

    init {
        viewModelScope.launch {
            // The id must be computed before distinctUntilChanged, not after: filtering to
            // SignedIn first collapses a sign-out into nothing, so distinctUntilChanged sees
            // "user-1", "user-1" across a sign-out/sign-in cycle for the same user and drops
            // the second sync. Mapping first makes sign-out a real, distinct `null` value in
            // the stream, so signing back in as the same user is seen as a change.
            sessionState
                .map { (it as? SessionState.SignedIn)?.user?.id }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { watchlistRepository.syncWatchlist() }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
