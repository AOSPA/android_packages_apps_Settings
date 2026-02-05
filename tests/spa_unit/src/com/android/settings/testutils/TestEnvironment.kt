/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.testutils

import androidx.lifecycle.testing.TestLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Provides the environment tools for a test execution.
 *
 * This interface exposes helper methods to control coroutine execution and access the test
 * lifecycle owner.
 */
interface TestEnvironment {
    /**
     * The [CoroutineDispatcher] used for this test (specifically a [StandardTestDispatcher]). Use
     * this when instantiating objects that require a dispatcher dependency.
     */
    val testDispatcher: CoroutineDispatcher

    /**
     * Advances the [StandardTestDispatcher] until all scheduled tasks are executed. Use this to
     * simulate the passage of time or completion of pending coroutines.
     */
    fun advanceUntilIdle()

    /**
     * A [TestLifecycleOwner] initialized with the same dispatcher as the test. Useful for testing
     * components that observe Lifecycle events.
     */
    val testLifecycleOwner: TestLifecycleOwner
}

/**
 * Executes a unit test with a dedicated [StandardTestDispatcher] and replaced Main dispatcher.
 *
 * This helper reduces boilerplate by:
 * 1. Creating a [StandardTestDispatcher].
 * 2. Setting [Dispatchers.Main] to that dispatcher.
 * 3. Ensuring [Dispatchers.Main] is reset after the test completes (via try/finally).
 *
 * @param testBody The test logic. It runs with [TestEnvironment] as the receiver.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runTestWithDispatcher(testBody: suspend TestEnvironment.() -> Unit) = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val testEnvironment =
        object : TestEnvironment {
            override val testDispatcher = testDispatcher

            override fun advanceUntilIdle() {
                testScheduler.advanceUntilIdle()
            }

            override val testLifecycleOwner =
                TestLifecycleOwner(coroutineDispatcher = testDispatcher)
        }

    Dispatchers.setMain(testDispatcher)
    try {
        testEnvironment.testBody()
    } finally {
        Dispatchers.resetMain()
    }
}
