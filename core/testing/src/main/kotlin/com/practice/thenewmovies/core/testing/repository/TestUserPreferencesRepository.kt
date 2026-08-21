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
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.datastore.UserPreferences
import com.practice.thenewmovies.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestUserPreferencesRepository : UserPreferencesRepository {

    private val state = MutableStateFlow(UserPreferences())

    override val preferences: Flow<UserPreferences> = state

    override suspend fun setSelectedHomeTab(index: Int) {
        state.value = state.value.copy(selectedHomeTab = index)
    }
}
