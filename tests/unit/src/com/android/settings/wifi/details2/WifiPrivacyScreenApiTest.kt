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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiPrivacyScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockSavedNetworkRepository = mock<SavedNetworkRepository>()
    private lateinit var provider: WifiFeatureProvider
    private lateinit var screen: WifiPrivacyScreenApi
    private lateinit var tester: ApiTester

    private val fakeInfos = listOf(SavedNetworkInfo(ssid = TEST_SSID, key = TEST_KEY))

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

        assertThat(options).containsExactly(TEST_LOOKUP_KEY)
    }

    private companion object {
        const val TEST_SSID = "Test_SSID"
        const val TEST_KEY = "Test_Key"
        val TEST_LOOKUP_KEY = "${TEST_KEY.hashCode()}_${TEST_SSID}"
    }
}
