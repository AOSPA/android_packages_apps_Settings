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

package com.android.settings.datausage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.NetworkPolicyManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.AppInfoBase
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.APP_BACKGROUND_DATA_SWITCH_KEY
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.KEY_APP_PACKAGE_NAME
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.getBackgroundDataEnabled
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.getPackageUid
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.setBackgroundDataEnabled
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AppDataUsageScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private val mockPm = mock<PackageManager>()
    private val mockNpm = mock<NetworkPolicyManager>()
    private val mockDataSaverBackend = mock<DataSaverBackend>()
    private lateinit var screen: AppDataUsageScreenApi
    private lateinit var tester: ApiTester

    private val packageName = "com.android.settings"
    private val testUid = 1000
    private val unknownPackage = "unknown.package"
    private val unknownAppInfo =
        ApplicationInfo().apply {
            packageName = unknownPackage
            uid = -1
        }

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
                this.packageName = packageName
                this.uid = testUid
            }
        mockPm.stub {
            on { getInstalledApplications(any<PackageManager.ApplicationInfoFlags>()) } doReturn
                listOf(appInfo)
            on { getPackageUid(eq(packageName), any<Int>()) } doReturn testUid
        }

        screen = AppDataUsageScreenApi()
        tester = ApiTester(screen)
        tester.initializeScreenParameters(Parameters(KEY_APP_PACKAGE_NAME to packageName))
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
        assertThat(screen.key).isEqualTo(AppDataUsageScreenApi.KEY)
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
            .isEqualTo("app_data_usage_screen_purpose")
    }

    @Test
    fun alreadyPartiallyMigrated_isDataUsageAppDetailScreen() {
        assertThat(screen.alreadyPartiallyMigrated).isEqualTo(DataUsageAppDetailScreen::class)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchScreenExtra_returnsCorrectExtras() = runTest {
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val schema = screen.parametersSchema!!
        val validated = schema.prepare(parameters.values)
        screen.initializeParameters(validated)

        val extras = screen.launchScreenExtra
        assertThat(extras.getString(KEY_APP_PACKAGE_NAME)).isEqualTo(packageName)
        assertThat(extras.getInt(AppInfoBase.ARG_PACKAGE_UID)).isEqualTo(testUid)
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
        assertThat(context.getPackageUid(packageName)).isEqualTo(testUid)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun appBackgroundDataSwitch_sensitivityLevel_isNoSensitivity() {
        val preference = screen.preferences.first { it.key == APP_BACKGROUND_DATA_SWITCH_KEY }
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataEnabled_policySet_returnsFalse() {
        mockNpm.stub {
            on { getUidPolicy(testUid) } doReturn
                NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
        }

        val result = context.getBackgroundDataEnabled(packageName, mockNpm)

        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataEnabled_policyUnset_returnsTrue() {
        mockNpm.stub { on { getUidPolicy(testUid) } doReturn NetworkPolicyManager.POLICY_NONE }

        val result = context.getBackgroundDataEnabled(packageName, mockNpm)

        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataEnabled_packageNotFound_returnsTrue() {
        mockNpm.stub {
            on { getUidPolicy(testUid) } doReturn
                NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
        }
        mockPm.stub {
            on { getPackageUid(eq(unknownPackage), any<Int>()) } doThrow
                PackageManager.NameNotFoundException()
        }

        val result = context.getBackgroundDataEnabled(unknownPackage, mockNpm)

        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataEnabled_policyManagerIsNull_returnsTrue() {
        mockNpm.stub {
            on { getUidPolicy(testUid) } doReturn
                NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
        }
        val result = context.getBackgroundDataEnabled(packageName, null)

        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBackgroundDataEnabled_true_enablesBackgroundData() {
        context.setBackgroundDataEnabled(packageName, true, mockDataSaverBackend)

        verify(mockDataSaverBackend).setIsDenylisted(testUid, packageName, false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBackgroundDataEnabled_false_disablesBackgroundData() {
        context.setBackgroundDataEnabled(packageName, false, mockDataSaverBackend)

        verify(mockDataSaverBackend).setIsDenylisted(testUid, packageName, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBackgroundDataEnabled_packageNotFound_doesNothing() {
        val unknownPackageName = "com.unknown.app"
        mockPm.stub {
            on { getPackageUid(unknownPackageName, 0) } doThrow
                PackageManager.NameNotFoundException()
        }

        context.setBackgroundDataEnabled(unknownPackageName, true, mockDataSaverBackend)

        verify(mockDataSaverBackend, never()).setIsDenylisted(any(), any(), any())
    }
}
