/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.hearingdevices.data

import android.content.Context
import android.media.AudioAttributes
import android.media.audiopolicy.AudioProductStrategy
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants.RoutingValue
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBluetoothUtils::class])
class HearingDeviceAudioRoutingDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val cachedDeviceManager: CachedBluetoothDeviceManager = mock()
    private val cachedDevice: CachedBluetoothDevice = mock()
    private val audioRoutingHelper: HearingAidAudioRoutingHelper = mock()
    private val localBluetoothManager: LocalBluetoothManager = mock {
        on { cachedDeviceManager } doReturn cachedDeviceManager
    }

    private val settingsStore = SettingsSecureStore.get(context)
    private val audioAttribute = intArrayOf(AudioAttributes.USAGE_NOTIFICATION)
    private lateinit var dataStore: HearingDeviceAudioRoutingDataStore

    @Before
    fun setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = localBluetoothManager

        dataStore =
            HearingDeviceAudioRoutingDataStore(
                context,
                audioAttribute,
                KEY,
                PROVIDER_KEY,
                testScope,
                settingsStore,
                audioRoutingHelper,
            )
    }

    @Test
    fun getValue_settingHearingDevice_returnsTrue() {
        settingsStore.setInt(PROVIDER_KEY, RoutingValue.HEARING_DEVICE)

        assertThat(dataStore.getBoolean(KEY)).isTrue()
    }

    @Test
    fun getValue_settingAuto_returnsTrue() {
        settingsStore.setInt(PROVIDER_KEY, RoutingValue.AUTO)

        assertThat(dataStore.getBoolean(KEY)).isTrue()
    }

    @Test
    fun getValue_settingBuiltIn_returnsFalse() {
        settingsStore.setInt(PROVIDER_KEY, RoutingValue.BUILTIN_DEVICE)

        assertThat(dataStore.getBoolean(KEY)).isFalse()
    }

    @Test
    fun setValue_true_updatesSettingToAuto_andConfiguresRouting() =
        testScope.runTest {
            cachedDevice.stub {
                on { isHearingDevice } doReturn true
                on { isActiveDevice(any()) } doReturn true
            }
            cachedDeviceManager.stub { on { cachedDevicesCopy } doReturn listOf(cachedDevice) }
            audioRoutingHelper.stub {
                on { getMatchedHearingDeviceAttributesForOutput(cachedDevice) } doReturn mock()
                on { getSupportedStrategies(audioAttribute) } doReturn
                    listOf(mock<AudioProductStrategy>())
            }

            dataStore.setBoolean(KEY, true)
            advanceUntilIdle()

            assertThat(settingsStore.getInt(PROVIDER_KEY)).isEqualTo(RoutingValue.AUTO)
            verify(audioRoutingHelper)
                .configureRoutingStrategies(any(), any(), eq(RoutingValue.AUTO))
        }

    @Test
    fun setValue_false_updatesSettingToBuiltIn_andConfiguresRouting() =
        testScope.runTest {
            cachedDeviceManager.stub { on { cachedDevicesCopy } doReturn listOf(cachedDevice) }
            cachedDevice.stub {
                on { isHearingDevice } doReturn true
                on { isActiveDevice(any()) } doReturn true
            }
            audioRoutingHelper.stub {
                on { getMatchedHearingDeviceAttributesForOutput(cachedDevice) } doReturn mock()
                on { getSupportedStrategies(audioAttribute) } doReturn
                    listOf(mock<AudioProductStrategy>())
            }

            dataStore.setBoolean(KEY, false)
            advanceUntilIdle()

            assertThat(settingsStore.getInt(PROVIDER_KEY)).isEqualTo(RoutingValue.BUILTIN_DEVICE)
            verify(audioRoutingHelper)
                .configureRoutingStrategies(any(), any(), eq(RoutingValue.BUILTIN_DEVICE))
        }

    @Test
    fun setValue_noActiveDevice_doesNotConfigureRouting() =
        testScope.runTest {
            cachedDeviceManager.stub { on { cachedDevicesCopy } doReturn emptyList() }

            dataStore.setBoolean(KEY, true)
            advanceUntilIdle()

            assertThat(settingsStore.getInt(PROVIDER_KEY)).isEqualTo(RoutingValue.AUTO)
            verify(audioRoutingHelper, never()).configureRoutingStrategies(any(), any(), any())
        }

    companion object {
        private const val KEY = "test_key"
        private const val PROVIDER_KEY = "Settings.Secure.HEARING_AID_NOTIFICATION_ROUTING"
    }
}
