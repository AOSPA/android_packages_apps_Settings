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
import android.content.res.Resources
import android.os.PersistableBundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteManager
import com.android.settings.R
import com.android.settings.flags.Flags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDeviceConfig
import org.robolectric.shadows.ShadowSatelliteManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteTileStateReceiverTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var pendingResult: BroadcastReceiver.PendingResult
    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var resources: Resources

    private lateinit var context: Context
    private var componentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    private lateinit var receiver: SatelliteTileStateReceiver
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private val testDispatcher = StandardTestDispatcher()
    private val SUB_ID = 1

    @Before
    fun setUp() {
        context = spy(RuntimeEnvironment.getApplication())
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SATELLITE))
            .thenReturn(true)
        `when`(context.resources).thenReturn(resources)
        `when`(resources.getBoolean(R.bool.config_show_satellite_tile)).thenReturn(true)
        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))
        receiver = spy(SatelliteTileStateReceiver(testDispatcher))
        `when`(receiver.goAsync()).thenReturn(pendingResult)

        // Reset the singleton state of SatelliteSupportedStateChangeHandler to ensure test
        // isolation
        SatelliteSupportedStateChangeHandler.reset()

        componentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        `when`(packageManager.getComponentEnabledSetting(any())).thenAnswer {
            componentEnabledState
        }
        `when`(packageManager.setComponentEnabledSetting(any(), anyInt(), anyInt())).then {
            componentEnabledState = it.getArgument(1)
        }
    }

    @After
    fun tearDown() {
        ShadowDeviceConfig.reset()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_flagDisabled_doesNothing() = runTest {
        sendBootCompletedBroadcast()

        verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
        verify(pendingResult, never()).finish()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_configIsFalse_doesNothing() = runTest {
        `when`(resources.getBoolean(R.bool.config_show_satellite_tile)).thenReturn(false)

        sendBootCompletedBroadcast()

        verify(receiver, never()).goAsync()
        verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_bootCompleted_isLteBasedNtnSupported_enablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(true)

            sendBootCompletedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_bootCompleted_isNbIotBasedNtnSupported_enablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(true, null)

            sendBootCompletedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_bootCompleted_noNtnSupport_disablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(false, null)

            sendBootCompletedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_bootCompleted_isNbIotBasedNtnRequestFailed_disablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            val exception =
                SatelliteManager.SatelliteException(
                    SatelliteManager.SATELLITE_RESULT_REQUEST_FAILED
                )
            shadowSatelliteManager.setIsSupportedResponse(false, exception)

            sendBootCompletedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_noSatelliteFeature_doesNothing() = runTest {
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SATELLITE))
            .thenReturn(false)

        sendBootCompletedBroadcast()

        verify(receiver, never()).goAsync()
        verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_unknownAction_doesNothing() = runTest {
        val intent = Intent("com.android.settings.UNKNOWN_ACTION")
        receiver.onReceive(context, intent)

        verify(receiver, never()).goAsync()
        verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_bootCompleted_registersCallbackOnlyOnce() =
        runTest(testDispatcher) {
            // Arrange: initial state is unsupported
            shadowSatelliteManager.setIsSupportedResponse(false, null)

            // Act 1: Send first broadcast, which should register the callback.
            sendBootCompletedBroadcast()
            advanceUntilIdle()

            // Act 2: Send second broadcast. This should not register the callback again.
            val anotherPendingResult = mock(BroadcastReceiver.PendingResult::class.java)
            `when`(receiver.goAsync()).thenReturn(anotherPendingResult)
            sendBootCompletedBroadcast()
            advanceUntilIdle()

            // Assert: Trigger the callback. If it was registered only once, the tile state
            // should be updated only once.
            clearInvocations(packageManager)
            shadowSatelliteManager.triggerOnSupportedStateChanged(true)
            advanceUntilIdle()
            // Verify that the tile was enabled exactly once, confirming the callback was not
            // registered multiple times.
            verify(packageManager)
                .setComponentEnabledSetting(
                    any(),
                    eq(PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                    anyInt(),
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_simCardStateChanged_isLteBasedNtnSupported_enablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(true)

            sendSimCardStateChangedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_simCardStateChanged_noNtnSupport_disablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(false, null)

            sendSimCardStateChangedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_carrierConfigChanged_isLteBasedNtnSupported_enablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(true)

            sendCarrierConfigChangedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_carrierConfigChanged_noNtnSupport_disablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(false, null)

            sendCarrierConfigChangedBroadcast()
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            verify(pendingResult).finish()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onReceive_carrierConfigChanged_noNtnSupport_callbackNotRegistered() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(false, null)

            sendCarrierConfigChangedBroadcast()
            advanceUntilIdle()

            // Verify that the callback was not registered
            clearInvocations(packageManager)
            shadowSatelliteManager.triggerOnSupportedStateChanged(true)
            advanceUntilIdle()
            verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onSupportedStateChanged_ntnBecomesSupported_enablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            sendBootCompletedBroadcast()
            advanceUntilIdle()

            // Simulate the state changing to supported
            shadowSatelliteManager.triggerOnSupportedStateChanged(true)
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onSupportedStateChanged_ntnBecomesUnsupported_disablesTile() =
        runTest(testDispatcher) {
            setLteNtnSupported(false)
            shadowSatelliteManager.setIsSupportedResponse(true, null)
            sendBootCompletedBroadcast()
            advanceUntilIdle()
            // Verify that the tile is initially enabled when NB-IoT NTN is supported before
            // testing the state change.
            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)

            // Simulate the state changing to not supported
            shadowSatelliteManager.triggerOnSupportedStateChanged(false)
            advanceUntilIdle()

            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun onSupportedStateChanged_lteNtnSupported_tileStateUnchanged() =
        runTest(testDispatcher) {
            setLteNtnSupported(true)
            sendBootCompletedBroadcast()
            advanceUntilIdle()

            // Verify that the tile is initially enabled
            verifyTileEnabledState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            clearInvocations(packageManager)

            // Simulate the NB-IoT state changing to not supported
            shadowSatelliteManager.triggerOnSupportedStateChanged(false)
            advanceUntilIdle()

            // The tile should remain enabled because LTE NTN is supported
            verify(packageManager, never()).setComponentEnabledSetting(any(), anyInt(), anyInt())
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SATELLITE_TILE)
    fun registerForSupportedStateChanged_throwsException_doesNotCrash() {
        val mockSatelliteManager = mock(SatelliteManager::class.java)
        `when`(context.getSystemService(SatelliteManager::class.java))
            .thenReturn(mockSatelliteManager)
        `when`(mockSatelliteManager.registerForSupportedStateChanged(any(), any()))
            .thenThrow(IllegalStateException("Registration failed"))

        // This should not crash
        SatelliteSupportedStateChangeHandler.register(context, testDispatcher)
    }

    private fun sendBootCompletedBroadcast() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
    }

    private fun sendSimCardStateChangedBroadcast() {
        val intent = Intent(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED)
        receiver.onReceive(context, intent)
    }

    private fun sendCarrierConfigChangedBroadcast() {
        val intent = Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)
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

    /**
     * Configures the test environment to simulate whether LTE NTN is supported.
     *
     * @param isSupported `true` to simulate that LTE NTN is supported, `false` otherwise.
     */
    private fun setLteNtnSupported(isSupported: Boolean) {
        // A non-empty set of restriction reasons means that attach is restricted.
        val reasons = if (isSupported) emptySet() else setOf(1)
        shadowSatelliteManager.setAttachRestrictionReasonsForCarrier(SUB_ID, reasons)

        val config =
            PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, isSupported)
                putInt(
                    CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                    CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                )
            }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, config)
    }
}
