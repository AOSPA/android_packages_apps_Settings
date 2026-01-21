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

package com.android.settings.network.tether

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.network.tether.TetherApiScreen.Companion.ETH_TETHER_KEY
import com.android.settings.network.tether.TetherApiScreen.Companion.USB_TETHER_KEY
import com.android.settings.network.tether.TetheringRepository.TetherType
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class TetherApiScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val mockTetheringRepository = mock<TetheringRepository>()
    private lateinit var provider: WifiFeatureProvider
    private lateinit var tester: ApiTester

    @Before
    fun setUp() {
        provider = FakeFeatureFactory.setupForTest().wifiFeatureProvider
        provider.stub { on { tetheringRepository } doReturn mockTetheringRepository }
        tester = ApiTester(TetherApiScreen())
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_noUsbTethering_returnFalse() {
        mockTetheringRepository.stub { onBlocking { isEnabled(TetherType.USB) } doReturn false }

        assertThat(tester.get<Boolean>(USB_TETHER_KEY)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_hasUsbTethering_returnTrue() {
        mockTetheringRepository.stub { onBlocking { isEnabled(TetherType.USB) } doReturn true }

        assertThat(tester.get<Boolean>(USB_TETHER_KEY)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_enableUsbTethering_verifyEnable() = runBlocking {
        tester.set(USB_TETHER_KEY, true)

        verify(mockTetheringRepository).setEnabled(TetherType.USB, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_disableUsbTethering_verifyDisable() = runBlocking {
        tester.set(USB_TETHER_KEY, false)

        verify(mockTetheringRepository).setEnabled(TetherType.USB, false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_noEthernetTethering_returnFalse() {
        mockTetheringRepository.stub {
            onBlocking { isEnabled(TetherType.ETHERNET) } doReturn false
        }

        assertThat(tester.get<Boolean>(ETH_TETHER_KEY)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBoolean_hasEthernetTethering_returnTrue() {
        mockTetheringRepository.stub { onBlocking { isEnabled(TetherType.ETHERNET) } doReturn true }

        assertThat(tester.get<Boolean>(ETH_TETHER_KEY)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_enableEthernetTethering_verifyEnable() = runBlocking {
        tester.set(ETH_TETHER_KEY, true)

        verify(mockTetheringRepository).setEnabled(TetherType.ETHERNET, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBoolean_disableEthernetTethering_verifyDisable() = runBlocking {
        tester.set(ETH_TETHER_KEY, false)

        verify(mockTetheringRepository).setEnabled(TetherType.ETHERNET, false)
    }
}
