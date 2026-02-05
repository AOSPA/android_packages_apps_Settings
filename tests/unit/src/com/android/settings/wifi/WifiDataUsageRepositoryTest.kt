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

package com.android.settings.wifi

import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkTemplate
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.net.DataUsageController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiDataUsageRepositoryTest {
    private val mockDataUsageController = mock<DataUsageController>()
    private val mockPackageManager =
        mock<PackageManager> { on { hasSystemFeature(PackageManager.FEATURE_WIFI) } doReturn true }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getPackageManager() } doReturn mockPackageManager
        }

    private lateinit var repository: WifiDataUsageRepository

    @Before
    fun setUp() {
        // Reset the static cache to ensure test independence
        val field = WifiDataUsageRepository::class.java.getDeclaredField("cachedHasFeatureWifi")
        field.isAccessible = true
        field.set(null, null)

        repository = WifiDataUsageRepository(context)
        repository.dataUsageController = mockDataUsageController
    }

    @Test
    fun isAvailable_hasFeatureWifi_usagePositive_returnTrue() {
        mockDataUsageController.stub { on { getHistoricalUsageLevel(WIFI_TEMPLATE) } doReturn 1L }

        assertThat(repository.isAvailable).isTrue()
    }

    @Test
    fun isAvailable_hasFeatureWifi_usageZero_returnFalse() {
        mockDataUsageController.stub { on { getHistoricalUsageLevel(WIFI_TEMPLATE) } doReturn 0L }

        assertThat(repository.isAvailable).isFalse()
    }

    @Test
    fun isAvailable_hasFeatureWifi_usageNegative_returnsFalse() {
        mockDataUsageController.stub { on { getHistoricalUsageLevel(WIFI_TEMPLATE) } doReturn -1L }

        assertThat(repository.isAvailable).isFalse()
    }

    @Test
    fun isAvailable_noFeatureWifi_usagePositive_returnFalse() {
        val field = WifiDataUsageRepository::class.java.getDeclaredField("cachedHasFeatureWifi")
        field.isAccessible = true
        field.set(null, null)
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_WIFI) } doReturn false
        }
        mockDataUsageController.stub { on { getHistoricalUsageLevel(WIFI_TEMPLATE) } doReturn 1L }
        // Re-instantiate to pick up the new mockPackageManager behavior for hasFeatureWifi
        repository = WifiDataUsageRepository(context)

        assertThat(repository.isAvailable).isFalse()
    }

    @Test
    fun hasWifiRadio_cachesResult() {
        // hasWifiRadio was already called once in repository's init

        // Call it again
        WifiDataUsageRepository.hasWifiRadio(context)

        // Verify that packageManager was only consulted once
        verify(mockPackageManager, times(1)).hasSystemFeature(PackageManager.FEATURE_WIFI)
    }

    @Test
    fun historicalUsageLevel_returnsValueFromController() {
        mockDataUsageController.stub { on { getHistoricalUsageLevel(WIFI_TEMPLATE) } doReturn 123L }

        assertThat(repository.historicalUsageLevel).isEqualTo(123L)
    }

    @Test
    fun wifiTemplate_isCorrect() {

        val template = WifiDataUsageRepository.WIFI_TEMPLATE

        assertThat(template.matchRule).isEqualTo(NetworkTemplate.MATCH_WIFI)
    }

    companion object {
        private val WIFI_TEMPLATE = NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
    }
}
