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

package com.android.settings.wifi.details2

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settings.wifi.repository.SavedNetworkInfo
import com.android.settings.wifi.repository.SavedNetworkRepository
import com.google.common.truth.Truth.assertThat
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiPrivacyScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    // TODO(b/494298373): Replace SavedNetworkRepository with SaveNetwork.
    private val mockSavedNetworkRepository = mock<SavedNetworkRepository>()
    private val mockWifiConfiguration = mock<WifiConfiguration>()
    private lateinit var provider: WifiFeatureProvider
    private lateinit var screen: WifiPrivacyScreenApi
    private lateinit var tester: ApiTester

    @Before
    fun setUp() = runTest {
        provider = FakeFeatureFactory.setupForTest().wifiFeatureProvider
        provider.stub { on { savedNetworkRepository } doReturn mockSavedNetworkRepository }
        mockSavedNetworkRepository.stub {
            onBlocking { fetchSavedNetworksInfo() } doReturn fakeInfos
            onBlocking { findSavedNetworkInfo(TEST_LOOKUP_KEY) } doReturn fakeInfos.first()
        }

        screen = WifiPrivacyScreenApi()
        tester = ApiTester(screen)
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
    fun getSpaRoute_parameterized_returnsCorrectRoute() = runTest {
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        val route = screen.getSpaRoute()

        val expectedEncodedKey = Base64.getUrlEncoder().encodeToString(TEST_KEY.toByteArray())
        assertThat(route).isEqualTo("WifiPrivacy/$expectedEncodedKey")
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getParameterOptions_returnsCorrectOptions() = runTest {
        val options = tester.getParameterOptions(WifiPrivacyScreenApi.PARAMETER_KEY)

        assertThat(options).contains(TEST_LOOKUP_KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getSendDeviceName_valueIsTrue_returnsTrue() = runTest {
        mockWifiConfiguration.stub { on { isSendDhcpHostnameEnabled } doReturn true }
        mockSavedNetworkRepository.stub {
            on { getWifiConfiguration(TEST_LOOKUP_KEY) } doReturn mockWifiConfiguration
        }
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        val value = tester.get<Boolean>(WifiPrivacyScreenApi.SEND_DEVICES_NAME_SWITCH_KEY)

        assertThat(value).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getSendDeviceName_valueIsFalse_returnsFalse() = runTest {
        mockWifiConfiguration.stub { on { isSendDhcpHostnameEnabled } doReturn false }
        mockSavedNetworkRepository.stub {
            on { getWifiConfiguration(TEST_LOOKUP_KEY) } doReturn mockWifiConfiguration
        }
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        val value = tester.get<Boolean>(WifiPrivacyScreenApi.SEND_DEVICES_NAME_SWITCH_KEY)

        assertThat(value).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setSendDeviceName_valueIsTrue_setWifiConfiguration() = runTest {
        mockSavedNetworkRepository.stub {
            on { getWifiConfiguration(TEST_LOOKUP_KEY) } doReturn mockWifiConfiguration
        }
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        tester.set(WifiPrivacyScreenApi.SEND_DEVICES_NAME_SWITCH_KEY, true)

        verify(mockSavedNetworkRepository).setWifiConfiguration(eq(TEST_LOOKUP_KEY), any())
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setSendDeviceName_valueIsFalse_setWifiConfiguration() = runTest {
        mockSavedNetworkRepository.stub {
            on { getWifiConfiguration(TEST_LOOKUP_KEY) } doReturn mockWifiConfiguration
        }
        val allParameters = screen.getAllPossibleParameters(context).first()
        screen.initializeParameters(allParameters)

        tester.set(WifiPrivacyScreenApi.SEND_DEVICES_NAME_SWITCH_KEY, false)

        verify(mockSavedNetworkRepository).setWifiConfiguration(eq(TEST_LOOKUP_KEY), any())
    }

    private companion object {
        const val TEST_SSID = "SSID1"
        const val TEST_KEY = "key1"
        val TEST_LOOKUP_KEY = "${TEST_KEY.hashCode()}_${TEST_SSID}"

        private val fakeInfo1 = SavedNetworkInfo(ssid = TEST_SSID, key = TEST_KEY)
        private val fakeInfo2 = SavedNetworkInfo(ssid = "SSID2", key = "key2")
        private val fakeInfos = listOf(fakeInfo1, fakeInfo2)
    }
}
