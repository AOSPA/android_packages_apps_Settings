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

package com.android.settings.wifi.repository

import android.content.ContextWrapper
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wifitrackerlib.SavedNetworkTracker
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/** Unit tests for [SavedNetworkRepository]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SavedNetworkRepositoryTest {

    private val mockWifiManager = mock<WifiManager>()
    private val mockTracker = mock<SavedNetworkTracker>()
    private val mockWifiEntry = mock<WifiEntry>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                if (name == getSystemServiceName(WifiManager::class.java)) mockWifiManager
                else super.getSystemService(name)
        }

    private lateinit var repository: SavedNetworkRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Prevent Mockito state corruption during background lifecycle calls
        mockTracker.stub {
            on { onStart() } doAnswer {}
            on { onStop() } doAnswer {}
            on { savedWifiEntries } doReturn emptyList()
            on { subscriptionWifiEntries } doReturn emptyList()
        }

        mockWifiManager.stub { on { configuredNetworks } doReturn listOf(mock(), mock(), mock()) }

        mockWifiEntry.stub {
            on { title } doReturn TEST_SSID
            on { key } doReturn TEST_KEY
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getSavedNetworksInfo_dataReady_returnsMappedInfo() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)

        val result = repository.getSavedNetworksInfo()

        assertThat(result).hasSize(1)
        assertThat(result[0].ssid).isEqualTo(TEST_SSID)
    }

    @Test
    fun getSavedNetworksInfo_pollingSuccess_returnsAfterRetry() = runTest {
        var callCount = 0
        mockTracker.stub {
            on { savedWifiEntries } doAnswer
                {
                    callCount++
                    if (callCount <= 1) emptyList() else listOf(mockWifiEntry)
                }
        }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)

        val deferredResult = async { repository.getSavedNetworksInfo() }

        runCurrent()
        // Advance time to trigger retry (POLL_INTERVAL_MS = 50ms)
        advanceTimeBy(60)
        runCurrent()

        val result = deferredResult.await()

        assertThat(result).hasSize(1)
        assertThat(result[0].ssid).isEqualTo(TEST_SSID)
        assertThat(callCount).isAtLeast(2)
    }

    @Test
    fun findSavedNetworkInfo_validKey_returnsCorrectItem() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.findSavedNetworkInfo(TEST_LOOKUP_KEY)

        assertThat(result).isNotNull()
        assertThat(result?.ssid).isEqualTo(TEST_SSID)
    }

    @Test
    fun fetchSavedNetworksInfo_syncCall_returnsData() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.fetchSavedNetworksInfo()

        assertThat(result).isNotEmpty()
        assertThat(result[0].key).isEqualTo(TEST_KEY)
    }

    @Test
    fun getWifiConfiguration_validKey_returnsConfig() = runTest {
        val mockWifiConfiguration = mock<WifiConfiguration>()
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        mockWifiEntry.stub { on { wifiConfiguration } doReturn mockWifiConfiguration }
        mockWifiManager.stub {
            on { privilegedConfiguredNetworks } doReturn listOf(mockWifiConfiguration)
        }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.getWifiConfiguration(TEST_LOOKUP_KEY)

        assertThat(result).isSameInstanceAs(mockWifiConfiguration)
    }

    @Test
    fun getWifiConfiguration_invalidKey_returnsNull() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn emptyList() }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.getWifiConfiguration(TEST_LOOKUP_KEY)

        assertThat(result).isNull()
    }

    @Test
    fun getWifiConfiguration_nullKey_returnsNull() = runTest {
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.getWifiConfiguration(null)

        assertThat(result).isNull()
    }

    @Test
    fun getWifiConfiguration_entryNotFound_returnsNull() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn emptyList() }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.getWifiConfiguration(TEST_LOOKUP_KEY)

        assertThat(result).isNull()
    }

    @Test
    fun getWifiConfiguration_configNotFound_returnsNull() = runTest {
        val mockWifiConfiguration = mock<WifiConfiguration>()
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        mockWifiEntry.stub { on { wifiConfiguration } doReturn mockWifiConfiguration }
        mockWifiManager.stub { on { privilegedConfiguredNetworks } doReturn emptyList() }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        val result = repository.getWifiConfiguration(TEST_LOOKUP_KEY)

        assertThat(result).isNull()
    }

    @Test
    fun setWifiConfiguration_updatesNetwork() = runTest {
        val mockWifiConfiguration = mock<WifiConfiguration>()
        mockTracker.stub { on { savedWifiEntries } doReturn listOf(mockWifiEntry) }
        mockWifiEntry.stub { on { wifiConfiguration } doReturn mockWifiConfiguration }
        mockWifiManager.stub {
            on { privilegedConfiguredNetworks } doReturn listOf(mockWifiConfiguration)
        }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        repository.setWifiConfiguration(TEST_LOOKUP_KEY) { config -> config.SSID = "newSSID" }

        verify(mockWifiManager).save(mockWifiConfiguration, null)
    }

    @Test
    fun setWifiConfiguration_nullKey_doesNothing() = runTest {
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        repository.setWifiConfiguration(null) { config -> config.SSID = "newSSID" }

        verify(mockWifiManager, never()).save(any(), any())
    }

    @Test
    fun setWifiConfiguration_configNotFound_doesNothing() = runTest {
        mockTracker.stub { on { savedWifiEntries } doReturn emptyList() }
        repository = SavedNetworkRepository(context, backgroundScope, mockTracker)
        runCurrent()

        repository.setWifiConfiguration(TEST_LOOKUP_KEY) { config -> config.SSID = "newSSID" }

        verify(mockWifiManager, never()).save(any(), any())
    }

    private companion object {
        const val TEST_SSID = "Test_SSID"
        const val TEST_KEY = "Test_Key"
        val TEST_LOOKUP_KEY = "${TEST_KEY.hashCode()}_$TEST_SSID"
    }
}
