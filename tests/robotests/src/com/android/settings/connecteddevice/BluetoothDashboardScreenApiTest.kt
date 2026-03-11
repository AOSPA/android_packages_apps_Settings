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

package com.android.settings.connecteddevice

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
class BluetoothDashboardScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(BluetoothDashboardScreenApi())
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockBluetoothManager = mock<BluetoothManager>()
    private val mockBluetoothAdapter = mock<BluetoothAdapter>()

    @Before
    fun setUp() {
        val shadowContext = extract<ShadowContextImpl>((context as Application).baseContext)
        shadowContext.setSystemService(Context.BLUETOOTH_SERVICE, mockBluetoothManager)

        // By default, assume the device has a BluetoothAdapter
        `when`(mockBluetoothManager.adapter).thenReturn(mockBluetoothAdapter)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_adapterNull_throwsFailedPrecondition() {
        // Simulate a device with no Bluetooth hardware
        `when`(mockBluetoothManager.adapter).thenReturn(null)

        assertFailsWith<FailedPreconditionException> {
            tester.get<Boolean>(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_featureNotSupported_throwsFailedPrecondition() {
        // Simulate an adapter that doesn't support the Auto-On feature
        `when`(mockBluetoothAdapter.isAutoOnSupported).thenReturn(false)

        assertFailsWith<FailedPreconditionException> {
            tester.get<Boolean>(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_get_returnsTrue() {
        // Ensure preconditions pass
        `when`(mockBluetoothAdapter.isAutoOnSupported).thenReturn(true)

        // Mock the actual state
        `when`(mockBluetoothAdapter.isAutoOnEnabled).thenReturn(true)

        assertThat(tester.get<Boolean>(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY))
            .isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_get_returnsFalse() {
        // Ensure preconditions pass
        `when`(mockBluetoothAdapter.isAutoOnSupported).thenReturn(true)

        // Mock the actual state
        `when`(mockBluetoothAdapter.isAutoOnEnabled).thenReturn(false)

        assertThat(tester.get<Boolean>(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY))
            .isFalse()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_setTrue_callsAdapter() {
        // Ensure preconditions pass
        `when`(mockBluetoothAdapter.isAutoOnSupported).thenReturn(true)

        tester.set(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY, true)

        verify(mockBluetoothAdapter).setAutoOnEnabled(true)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoOn_setFalse_callsAdapter() {
        // Ensure preconditions pass
        `when`(mockBluetoothAdapter.isAutoOnSupported).thenReturn(true)

        tester.set(BluetoothDashboardScreenApi.BLUETOOTH_AUTO_ON_TOGGLE_KEY, false)

        verify(mockBluetoothAdapter).setAutoOnEnabled(false)
    }
}
