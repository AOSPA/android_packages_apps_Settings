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

package com.android.settings.wifi.details

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settings.wifi.repository.SavedNetworkInfo
import com.android.settings.wifi.repository.SavedNetworkRepository
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SavedNetworkTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockSavedNetworkRepository = mock<SavedNetworkRepository>()
    private lateinit var provider: WifiFeatureProvider
    private lateinit var savedNetwork: SavedNetwork

    private val fakeInfo1 = SavedNetworkInfo(ssid = "SSID1", key = "key1")
    private val fakeInfo2 = SavedNetworkInfo(ssid = "SSID2", key = "key2")

    @Before
    fun setUp() {
        provider = FakeFeatureFactory.setupForTest().wifiFeatureProvider
        provider.stub { on { savedNetworkRepository } doReturn mockSavedNetworkRepository }

        mockSavedNetworkRepository.stub {
            onBlocking { fetchSavedNetworksInfo() } doReturn listOf(fakeInfo1, fakeInfo2)
            onBlocking { findSavedNetworkInfo(fakeInfo1.lookupKey) } doReturn fakeInfo1
            onBlocking { findSavedNetworkInfo("invalid_key") } doReturn null
        }

        savedNetwork = SavedNetwork
    }

    @Test
    fun getType_returnsStringClass() {
        assertThat(savedNetwork.getType()).isEqualTo(String::class.java)
    }

    @Test
    fun getDescription_returnsCorrectString() {
        assertThat(savedNetwork.getDescription(context)).isEqualTo("A saved network")
    }

    @Test
    fun getKey_returnsSavedNetwork() {
        assertThat(savedNetwork.getKey()).isEqualTo("SavedNetwork")
    }

    @Test
    fun getOptions_returnsMappedNetworks() = runTest {
        val options = savedNetwork.getOptions(context)

        assertThat(options).hasSize(2)
        assertThat(options[0].first).isEqualTo(SafetyAnnotated.Unsafe(fakeInfo1.lookupKey))
        assertThat(options[0].second).isEqualTo(SafetyAnnotated.Unsafe(fakeInfo1.ssid))
        assertThat(options[1].first).isEqualTo(SafetyAnnotated.Unsafe(fakeInfo2.lookupKey))
        assertThat(options[1].second).isEqualTo(SafetyAnnotated.Unsafe(fakeInfo2.ssid))
    }

    @Test
    fun convertInternalToExternal_returnsLookupKey() {
        val external = savedNetwork.convertInternalToExternal(fakeInfo1)

        assertThat(external).isEqualTo(fakeInfo1.lookupKey)
    }

    @Test
    fun convertExternalToInternal_validKey_returnsNetworkInfo() {
        val internal = savedNetwork.convertExternalToInternal(fakeInfo1.lookupKey)

        assertThat(internal).isEqualTo(fakeInfo1)
    }

    @Test(expected = IllegalStateException::class)
    fun convertExternalToInternal_invalidKey_throwsError() {
        savedNetwork.convertExternalToInternal("invalid_key")
    }
}
