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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkTemplate
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.AppInfoBase
import com.android.settings.datausage.AppDataUsage
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.android.settings.wifi.WifiAppDataUsageScreenApi.Companion.KEY_APP_PACKAGE_NAME
import com.android.settings.wifi.WifiAppDataUsageScreenApi.Companion.getPackageUid
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiAppDataUsageScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private val mockPm = mock<PackageManager>()
    private lateinit var screen: WifiAppDataUsageScreenApi
    private lateinit var tester: ApiTester

    @Before
    fun setUp() = runTest {
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        context.stub {
            on { applicationContext } doReturn context
            on { packageManager } doReturn mockPm
        }
        val factory = FakeFeatureFactory.setupForTest()
        FeatureFactory.setFactory(context, factory)

        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.android.settings"
                uid = 1000
            }
        mockPm.stub {
            on {
                getInstalledApplicationsAsUser(
                    any<PackageManager.ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn
                listOf(appInfo)
        }

        screen = WifiAppDataUsageScreenApi()
        tester = ApiTester(screen, context)
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
    fun key_isCorrect() {
        assertThat(screen.key).isEqualTo(WifiAppDataUsageScreenApi.KEY)
    }

    @Test
    fun topLevelSettingsCategory_isApps() {
        assertThat(screen.topLevelSettingsCategory).isEqualTo(Category.APPS)
    }

    @Test
    fun fragmentClass_isAppDataUsage() {
        assertThat(screen.fragmentClass()).isEqualTo(AppDataUsage::class.java)
    }

    @Test
    fun purpose_isCorrect() {
        assertThat(context.resources.getResourceEntryName(screen.purpose))
            .isEqualTo("non_carrier_app_data_usage_screen_purpose")
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchScreenExtra_returnsCorrectExtras() = runTest {
        val packageName = "com.android.settings"
        val testUid = 1234
        mockPm.stub { on { getPackageUid(packageName, 0) } doReturn testUid }

        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val schema = screen.parametersSchema!!
        val validated = schema.prepare(parameters.values)
        screen.initializeParameters(validated)

        val extras = screen.launchScreenExtra
        assertThat(extras.getString(KEY_APP_PACKAGE_NAME)).isEqualTo(packageName)
        assertThat(extras.getInt(AppInfoBase.ARG_PACKAGE_UID)).isEqualTo(testUid)
        val template =
            extras.getParcelable<NetworkTemplate>(WifiAppDataUsageScreenApi.ARG_NETWORK_TEMPLATE)
        assertThat(template).isNotNull()
        val wifiTemplate = NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
        assertThat(template).isEqualTo(wifiTemplate)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchScreenExtra_nullPackageName_returnsEmptyExtras() = runTest {
        val schema = screen.parametersSchema!!
        val validated = schema.prepare(emptyMap())
        screen.initializeParameters(validated)

        val resultExtras = screen.launchScreenExtra

        assertThat(resultExtras.isEmpty).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getPossibleParameters_returnsFlow() = runTest {
        val possibleParameters = screen.getAllPossibleParameters(context).first()
        assertThat(possibleParameters).isNotNull()
    }

    @Test
    fun getPackageUid_packageFound_returnsUid() {
        val packageName = "com.test.app"
        val testUid = 5678
        mockPm.stub { on { getPackageUid(packageName, 0) } doReturn testUid }

        assertThat(context.getPackageUid(packageName)).isEqualTo(testUid)
    }

    @Test
    fun getPackageUid_packageNotFound_returnsMinusOne() {
        val packageName = "com.unknown.app"
        mockPm.stub {
            on { getPackageUid(packageName, 0) } doThrow PackageManager.NameNotFoundException()
        }

        assertThat(context.getPackageUid(packageName)).isEqualTo(-1)
    }
}
