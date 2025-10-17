/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.settings.flags.Flags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteTileStateReceiverTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var pendingResult: BroadcastReceiver.PendingResult

    private lateinit var context: Context
    private lateinit var receiver: SatelliteTileStateReceiver
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = spy(RuntimeEnvironment.getApplication())
        `when`(context.packageManager).thenReturn(packageManager)

        receiver = spy(SatelliteTileStateReceiver(testDispatcher))

        `when`(receiver.goAsync()).thenReturn(pendingResult)
    }

    @Test
    fun onReceive_withWrongAction_doesNothing() {
        val intent = Intent("android.intent.action.WRONG_ACTION")

        receiver.onReceive(context, intent)

        verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_withBootCompletedAndFlagEnabled_enablesTile() =
        runTest(testDispatcher) {
            // Simulate the receiver receiving a boot completed broadcast.
            sendBootCompletedBroadcast()

            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            verify(pendingResult).finish()
        }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_withBootCompletedAndFlagDisabled_doesNothing() =
        runTest(testDispatcher) {
            // Simulate the receiver receiving a boot completed broadcast.
            sendBootCompletedBroadcast()

            advanceUntilIdle()

            verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
        }

    private fun sendBootCompletedBroadcast() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
    }

    private fun verifyTileEnabledState(expectedState: Int) {
        val componentName = ComponentName(context, SatelliteTileService::class.java)
        verify(packageManager)
            .setComponentEnabledSetting(
                eq(componentName),
                eq(expectedState),
                eq(PackageManager.DONT_KILL_APP),
            )
    }
}
