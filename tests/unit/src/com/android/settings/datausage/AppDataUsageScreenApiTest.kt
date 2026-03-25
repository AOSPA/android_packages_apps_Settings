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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.AppInfoBase
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.KEY_APP_BACKGROUND_DATA_SWITCH
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.KEY_APP_PACKAGE_NAME
import com.android.settings.datausage.AppDataUsageScreenApi.Companion.KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.android.settings.wifi.repository.DataUsageRepository
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppDataUsageScreenApiTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var repository: DataUsageRepository

    private lateinit var context: Context
    private lateinit var screen: AppDataUsageScreenApi
    private lateinit var tester: ApiTester

    private val packageName = "com.android.settings"
    private val testUid = 1000

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext<Context>()
        val factory = FakeFeatureFactory.setupForTest()
        FeatureFactory.setFactory(context.applicationContext, factory)

        factory.mWifiFeatureProvider.stub { on { dataUsageRepository } doReturn repository }

        repository.stub { onBlocking { getPackageUid(packageName) } doReturn testUid }

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
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun backgroundDataSwitch_sensitivityLevel_isNoSensitivity() {
        val preference = screen.preferences.first { it.key == KEY_APP_BACKGROUND_DATA_SWITCH }

        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.DO_NOT_EXPOSE)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataSwitch_policyIsNotReject_returnsTrue() = runTest {
        repository.stub { onBlocking { isPolicyReject(packageName) } doReturn false }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val result = tester.get<Boolean>(KEY_APP_BACKGROUND_DATA_SWITCH, parameters)

        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getBackgroundDataSwitch_policyIsReject_returnsFalse() = runTest {
        repository.stub { onBlocking { isPolicyReject(packageName) } doReturn true }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val result = tester.get<Boolean>(KEY_APP_BACKGROUND_DATA_SWITCH, parameters)

        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBackgroundDataSwitch_setValueTrue_setPolicyRejectFalse() = runTest {
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        tester.set(KEY_APP_BACKGROUND_DATA_SWITCH, true, parameters)

        verify(repository).setPolicyReject(packageName, false)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setBackgroundDataSwitch_setValueFalse_setPolicyRejectTrue() = runTest {
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        tester.set(KEY_APP_BACKGROUND_DATA_SWITCH, false, parameters)

        verify(repository).setPolicyReject(packageName, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun unrestrictedMobileDataSwitch_sensitivityLevel_isNoSensitivity() {
        val preference =
            screen.preferences.first { it.key == KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH }

        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.DO_NOT_EXPOSE)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun unrestrictedMobileDataSwitchPreconditions_policyAllowAvailable_isAllowed() = runTest {
        repository.stub { onBlocking { isPolicyAllowAvailable(packageName) } doReturn true }
        val preference =
            screen.preferences.first { it.key == KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH }
        val parameters = screen.parametersSchema!!.prepare(KEY_APP_PACKAGE_NAME to packageName)
        val operationContext = ApiOperationContext(context, parameters = parameters)

        val result = preference.preconditions?.check?.invoke(operationContext)

        assertThat(result).isEqualTo(Allowed)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun unrestrictedMobileDataSwitchPreconditions_policyAllowNotAvailable_isNotAllowed() = runTest {
        repository.stub { onBlocking { isPolicyAllowAvailable(packageName) } doReturn false }
        val preference =
            screen.preferences.first { it.key == KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH }
        val parameters = screen.parametersSchema!!.prepare(KEY_APP_PACKAGE_NAME to packageName)
        val operationContext = ApiOperationContext(context, parameters = parameters)

        val result = preference.preconditions?.check?.invoke(operationContext)

        assertThat(result).isNotEqualTo(Allowed)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getUnrestrictedMobileDataSwitch_policyIsAllow_returnsTrue() = runTest {
        repository.stub {
            onBlocking { isPolicyAllowAvailable(packageName) } doReturn true
            onBlocking { isPolicyAllow(packageName) } doReturn true
        }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val result = tester.get<Boolean>(KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH, parameters)

        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getUnrestrictedMobileDataSwitch_policyIsNotAllow_returnsFalse() = runTest {
        repository.stub {
            onBlocking { isPolicyAllowAvailable(packageName) } doReturn true
            onBlocking { isPolicyAllow(packageName) } doReturn false
        }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        val result = tester.get<Boolean>(KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH, parameters)

        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setUnrestrictedMobileDataSwitch_setValueTrue_setPolicyAllowTrue() = runTest {
        repository.stub { onBlocking { isPolicyAllowAvailable(packageName) } doReturn true }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        tester.set(KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH, true, parameters)

        verify(repository).setPolicyAllow(packageName, true)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setUnrestrictedMobileDataSwitch_setValueFalse_setPolicyAllowFalse() = runTest {
        repository.stub { onBlocking { isPolicyAllowAvailable(packageName) } doReturn true }
        val parameters = Parameters(KEY_APP_PACKAGE_NAME to packageName)

        tester.set(KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH, false, parameters)

        verify(repository).setPolicyAllow(packageName, false)
    }
}
