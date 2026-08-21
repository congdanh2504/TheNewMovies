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

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private object Home : NavKey
    private object Search : NavKey
    private object Watchlist : NavKey
    private data class Detail(val movieId: Int) : NavKey

    private fun navigator(): Navigator {
        val state = NavigationState(
            subStacks = linkedMapOf(
                Home to mutableStateListOf<NavKey>(Home),
                Search to mutableStateListOf<NavKey>(Search),
                Watchlist to mutableStateListOf<NavKey>(Watchlist),
            ),
            currentIndex = mutableIntStateOf(0),
        )
        return Navigator(state)
    }

    @Test
    fun `starts on the first top level key`() {
        val navigator = navigator()

        assertEquals(Home, navigator.state.currentTopLevelKey)
        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `navigating to a non top level key pushes onto the current sub stack`() {
        val navigator = navigator()

        navigator.navigate(Detail(42))

        assertEquals(listOf(Home, Detail(42)), navigator.state.backStack)
        assertEquals(Detail(42), navigator.state.currentKey)
    }

    @Test
    fun `switching tabs preserves the previous tab history`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))

        navigator.navigate(Search)
        assertEquals(listOf(Search), navigator.state.backStack)

        navigator.navigate(Home)
        assertEquals(listOf(Home, Detail(42)), navigator.state.backStack)
    }

    @Test
    fun `re-selecting the current tab clears its sub stack`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))
        navigator.navigate(Detail(43))

        navigator.navigate(Home)

        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `going back pops the current sub stack`() {
        val navigator = navigator()
        navigator.navigate(Detail(42))

        assertTrue(navigator.goBack())

        assertEquals(listOf(Home), navigator.state.backStack)
    }

    @Test
    fun `going back from a tab root returns to the first tab`() {
        val navigator = navigator()
        navigator.navigate(Watchlist)

        assertTrue(navigator.goBack())

        assertEquals(Home, navigator.state.currentTopLevelKey)
    }

    @Test
    fun `going back at the start key does nothing and reports it`() {
        val navigator = navigator()

        assertFalse(navigator.goBack())

        assertEquals(Home, navigator.state.currentTopLevelKey)
        assertEquals(listOf(Home), navigator.state.backStack)
    }
}
