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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settingslib.net.DataUsageController
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class WifiDataUsageScreenApiTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var featureFactory: FakeFeatureFactory
    private lateinit var repository: WifiDataUsageRepository
    private lateinit var mockDataUsageController: DataUsageController
    private lateinit var tester: ApiTester

    @Before
    fun setUp() {
        featureFactory = FakeFeatureFactory.setupForTest()

        // Reset static cache in repository
        val field = WifiDataUsageRepository::class.java.getDeclaredField("cachedHasFeatureWifi")
        field.isAccessible = true
        field.set(null, null)

        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = WifiDataUsageRepository(context)
        mockDataUsageController = mock<DataUsageController>()
        repository.dataUsageController = mockDataUsageController

        whenever(featureFactory.mWifiFeatureProvider.wifiDataUsageRepository).thenReturn(repository)
        tester = ApiTester(WifiDataUsageScreenApi())
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
    fun getLaunchIntent_repositoryAvailable_returnsIntent() {
        whenever(mockDataUsageController.getHistoricalUsageLevel(any())).thenReturn(1L)

        val intent = tester.getLaunchIntent()

        assertThat(intent.action).isEqualTo("com.android.settings.action.LAUNCH_SETTINGS_PAGES")
        assertThat(intent.getStringExtra("screen_key")).isEqualTo(WifiDataUsageScreenApi.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_repositoryUnavailable_throwsException() {
        whenever(mockDataUsageController.getHistoricalUsageLevel(any())).thenReturn(0L)

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }
}
