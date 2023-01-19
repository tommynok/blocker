/*
 * Copyright 2023 Blocker
 * Copyright 2022 The Android Open Source Project
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

package com.merxury.blocker.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import com.merxury.blocker.core.testing.util.TestNetworkMonitor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests [BlockerAppState].
 *
 * Note: This could become an unit test if Robolectric is added to the project and the Context
 * is faked.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class BlockerAppStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Create the test dependencies.
    private val networkMonitor = TestNetworkMonitor()

    // Subject under test.
    private lateinit var state: BlockerAppState

    @Test
    fun niaAppState_currentDestination() = runTest {
        var currentDestination: String? = null

        composeTestRule.setContent {
            val navController = rememberTestNavController()
            state = remember(navController) {
                BlockerAppState(
                    windowSizeClass = getCompactWindowClass(),
                    navController = navController,
                    networkMonitor = networkMonitor,
                    coroutineScope = backgroundScope
                )
            }

            // Update currentDestination whenever it changes
            currentDestination = state.currentDestination?.route

            // Navigate to destination b once
            LaunchedEffect(Unit) {
                navController.setCurrentDestination("b")
            }
        }

        assertEquals("b", currentDestination)
    }

    @Test
    fun niaAppState_destinations() = runTest {
        composeTestRule.setContent {
            state = rememberBlockerAppState(
                windowSizeClass = getCompactWindowClass(),
                networkMonitor = networkMonitor
            )
        }

        assertEquals(3, state.topLevelDestinations.size)
        assertTrue(state.topLevelDestinations[0].name.contains("APP_LIST", true))
        assertTrue(state.topLevelDestinations[1].name.contains("ONLINE_RULES", true))
        assertTrue(state.topLevelDestinations[2].name.contains("GLOBAL_SEARCH", true))
    }

    @Test
    fun niaAppState_showBottomBar_compact() = runTest {
        composeTestRule.setContent {
            state = BlockerAppState(
                windowSizeClass = getCompactWindowClass(),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        assertTrue(state.shouldShowBottomBar)
        assertFalse(state.shouldShowNavRail)
    }

    @Test
    fun niaAppState_showNavRail_medium() = runTest {
        composeTestRule.setContent {
            state = BlockerAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 800.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        assertTrue(state.shouldShowNavRail)
        assertFalse(state.shouldShowBottomBar)
    }

    @Test
    fun niaAppState_showNavRail_large() = runTest {

        composeTestRule.setContent {
            state = BlockerAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(900.dp, 1200.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        assertTrue(state.shouldShowNavRail)
        assertFalse(state.shouldShowBottomBar)
    }

    @Test
    fun stateIsOfflineWhenNetworkMonitorIsOffline() = runTest(UnconfinedTestDispatcher()) {

        composeTestRule.setContent {
            state = BlockerAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(900.dp, 1200.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        backgroundScope.launch { state.isOffline.collect() }
        networkMonitor.setConnected(false)
        assertEquals(
            true,
            state.isOffline.value
        )
    }

    private fun getCompactWindowClass() = WindowSizeClass.calculateFromSize(DpSize(500.dp, 300.dp))
}

@Composable
private fun rememberTestNavController(): TestNavHostController {
    val context = LocalContext.current
    val navController = remember {
        TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = "a") {
                composable("a") { }
                composable("b") { }
                composable("c") { }
            }
        }
    }
    return navController
}