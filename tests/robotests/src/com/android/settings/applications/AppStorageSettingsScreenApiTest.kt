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

package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.android.settingslib.applications.StorageStatsSource
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppStorageSettingsScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var apiScreen: AppStorageSettingsScreenApi
    private lateinit var tester: ApiTester

    private val mockAppStorageStats = mock<StorageStatsSource.AppStorageStats>()

    private val testPackageName = "com.android.test"
    private val testCodeBytes = 2048L
    private val testCacheBytes = 1024L
    private val testDataBytes = 4096L

    private val packageManager = mock<PackageManager>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext()) {
            on { packageManager } doReturn packageManager
        }

        // Fake the installation of the test application
        val testAppInfo = ApplicationInfo().apply { packageName = testPackageName }
        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(testAppInfo)
        }

        doReturn(testCodeBytes).`when`(mockAppStorageStats).codeBytes
        doReturn(testDataBytes).`when`(mockAppStorageStats).dataBytes
        doReturn(testCacheBytes).`when`(mockAppStorageStats).cacheBytes
        doReturn(testCodeBytes + testDataBytes).`when`(mockAppStorageStats).totalBytes

        // Use an anonymous subclass to safely override the system boundary method.
        // This avoids Mockito 'spy' state issues when executing the Catalyst DSL.
        apiScreen =
            object : AppStorageSettingsScreenApi() {
                override fun getAppStorageStats(
                    context: Context,
                    packageName: String?,
                    userId: Int,
                ): StorageStatsSource.AppStorageStats? {
                    return if (packageName == testPackageName) mockAppStorageStats else null
                }
            }

        tester = ApiTester(apiScreen, context)
        tester.initializeScreenParameters(
            Parameters(AppStorageSettingsScreenApi.PARAM_PACKAGE to testPackageName)
        )
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
    fun appStorageScreen_producesCorrectExtras() {
        val extras = tester.getLaunchScreenExtras()
        assertThat(extras.getString("package")).isEqualTo(testPackageName)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getAppSize_returnsFormattedString() {
        val expectedKb = (testCodeBytes / 1024L).toInt()
        val result = tester.get<Int>(AppStorageSettingsScreenApi.KEY_APP_SIZE)
        assertThat(result).isEqualTo(expectedKb)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getCacheSize_returnsFormattedString() {
        val expectedKb = (testCacheBytes / 1024L).toInt()
        val result = tester.get<Int>(AppStorageSettingsScreenApi.KEY_CACHE_SIZE)
        assertThat(result).isEqualTo(expectedKb)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getDataSize_subtractsCache_returnsFormattedString() {
        val expectedKb = ((testDataBytes - testCacheBytes) / 1024L).toInt()
        val result = tester.get<Int>(AppStorageSettingsScreenApi.KEY_DATA_SIZE)
        assertThat(result).isEqualTo(expectedKb)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getTotalSize_sumsCorrectly_returnsFormattedString() {
        val expectedTotalBytes = testCodeBytes + testDataBytes
        val expectedKb = (expectedTotalBytes / 1024L).toInt()

        val result = tester.get<Int>(AppStorageSettingsScreenApi.KEY_TOTAL_SIZE)
        assertThat(result).isEqualTo(expectedKb)
    }
}
