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
package com.practice.thenewmovies.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * The verbs features use to navigate. Feature `api` modules add typed extensions on this type,
 * which is how a feature can navigate to another feature without depending on its `impl`.
 */
class Navigator(val state: NavigationState) {

    fun navigate(key: NavKey) {
        when {
            key == state.currentTopLevelKey -> state.clearSubStack()
            key in state.topLevelKeys -> state.goToTopLevel(key)
            else -> state.push(key)
        }
    }

    /** Returns false when there is nothing left to pop. */
    fun goBack(): Boolean = state.pop()
}
