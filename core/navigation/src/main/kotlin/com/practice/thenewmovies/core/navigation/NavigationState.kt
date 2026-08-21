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

import androidx.compose.runtime.MutableIntState
import androidx.navigation3.runtime.NavKey

/**
 * The app's navigation state: one back stack per top-level destination, plus which one is showing.
 *
 * Sub-stacks and the current index are passed in rather than created, so `:app` can hand over
 * state that survives process death while tests hand over plain snapshot state.
 *
 * @param subStacks ordered map of top-level key to that tab's stack; each stack must start with
 *   its own top-level key. Iteration order defines tab order, so pass a [LinkedHashMap].
 * @param currentIndex index into [topLevelKeys] of the visible tab.
 */
class NavigationState(
    private val subStacks: Map<NavKey, MutableList<NavKey>>,
    private val currentIndex: MutableIntState,
) {
    val topLevelKeys: List<NavKey> = subStacks.keys.toList()

    init {
        require(topLevelKeys.isNotEmpty()) { "NavigationState needs at least one top-level key" }
        require(subStacks.all { (key, stack) -> stack.firstOrNull() == key }) {
            "Each sub-stack must start with its own top-level key"
        }
        require(currentIndex.intValue in topLevelKeys.indices) {
            "currentIndex ${currentIndex.intValue} is out of bounds"
        }
    }

    val currentTopLevelKey: NavKey get() = topLevelKeys[currentIndex.intValue]

    /** The stack `NavDisplay` renders. Reads are snapshot-observed, so recomposition follows. */
    val backStack: MutableList<NavKey> get() = subStacks.getValue(currentTopLevelKey)

    val currentKey: NavKey get() = backStack.last()

    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun goToTopLevel(key: NavKey) {
        val index = topLevelKeys.indexOf(key)
        require(index >= 0) { "$key is not a top-level key" }
        currentIndex.intValue = index
    }

    fun clearSubStack() {
        val stack = backStack
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }

    /** Pops one entry, or falls back to the first tab. Returns false when already at the start. */
    fun pop(): Boolean {
        val stack = backStack
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (currentIndex.intValue != 0) {
            currentIndex.intValue = 0
            return true
        }
        return false
    }
}
