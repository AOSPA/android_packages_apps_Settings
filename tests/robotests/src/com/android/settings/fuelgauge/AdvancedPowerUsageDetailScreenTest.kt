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

package com.android.settings.fuelgauge

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail.EXTRA_UID
import com.android.settings.fuelgauge.AdvancedPowerUsageDetailScreen.Companion.SWITCH_KEY
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
class AdvancedPowerUsageDetailScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mockFeatureFactory = mock<FeatureFactory>()
    private val mockPackageManager = mock<PackageManager>()

    private lateinit var context: Context
    private lateinit var tester: ApiTester

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = spy(baseContext)
        FeatureFactory.setFactory(context, mockFeatureFactory)
        context.stub { on { packageManager } doReturn mockPackageManager }

        val testAppInfo =
            ApplicationInfo().apply {
                this.packageName = PACKAGE_NAME
                this.uid = UID
            }
        mockPackageManager.stub {
            on { getPackageUidAsUser(eq(PACKAGE_NAME), anyInt()) } doReturn UID
            on { getInstalledApplicationsAsUser(any<ApplicationInfoFlags>(), anyInt()) } doReturn
                listOf(testAppInfo)
        }
        tester = ApiTester(AdvancedPowerUsageDetailScreen(), context)
    }

    @After
    fun cleanUp() {
        ShadowPackageManager.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q3)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q3)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getLaunchIntent_mutableMode_hasIntent() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(false)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(false)
        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))

        val extras = tester.getLaunchScreenExtras()
        assertThat(extras.keySet()).hasSize(2)
        assertThat(extras.getString(EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(extras.getInt(EXTRA_UID)).isEqualTo(UID)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getLaunchIntent_immutableMode_hasIntent() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(true)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(false)
        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))

        val extras = tester.getLaunchScreenExtras()
        assertThat(extras.keySet()).hasSize(2)
        assertThat(extras.getString(EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(extras.getInt(EXTRA_UID)).isEqualTo(UID)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getBackgroundUsageAllowabilitySwitch_returnExpectedResult() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(false)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(false)
        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))
        tester.getLaunchIntent()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isTrue()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_UNRESTRICTED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isTrue()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isFalse()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun setBackgroundUsageAllowabilitySwitch_disableForOptimizeModeOnly_throwFailedPreconditionException() {
        setupTesterAndShadows(disableForOptimizeModeOnly = true, isSystemOrDefaultApp = false)

        assertThrows(FailedPreconditionException::class.java) { tester.set(SWITCH_KEY, true) }
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun setBackgroundUsageAllowabilitySwitch_systemApp_throwFailedPreconditionException() {
        setupTesterAndShadows(disableForOptimizeModeOnly = false, isSystemOrDefaultApp = true)

        assertThrows(FailedPreconditionException::class.java) { tester.set(SWITCH_KEY, true) }
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun setBackgroundUsageAllowabilitySwitch_normalApp_returnExpectedResult() {
        setupTesterAndShadows(disableForOptimizeModeOnly = false, isSystemOrDefaultApp = false)

        // Original mode: Restricted + Toggle on
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_OPTIMIZED,
            Action.SETTINGS_API_APPLY,
        )

        // Original mode: Optimized + Toggle on
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_OPTIMIZED,
            Action.UNKNOWN,
        ) // skip for the same mode

        // Original mode: Unrestricted + Toggle on
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_UNRESTRICTED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_UNRESTRICTED,
            Action.UNKNOWN,
        ) // skip for the same mode

        // Original mode: Restricted + Toggle off
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        tester.set(SWITCH_KEY, false)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_RESTRICTED,
            Action.UNKNOWN,
        ) // skip for the same mode

        // Original mode: Optimized + Toggle off
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        tester.set(SWITCH_KEY, false)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_RESTRICTED,
            Action.SETTINGS_API_APPLY,
        )

        // Original mode: Unrestricted + Toggle off
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_UNRESTRICTED)
        tester.set(SWITCH_KEY, false)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_RESTRICTED,
            Action.SETTINGS_API_APPLY,
        )
    }

    private fun setupTesterAndShadows(
        disableForOptimizeModeOnly: Boolean = false,
        isSystemOrDefaultApp: Boolean = false,
    ) {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(disableForOptimizeModeOnly)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(isSystemOrDefaultApp)
        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))
        tester.getLaunchIntent()
    }

    companion object {
        private const val PACKAGE_NAME = "com.abc"
        private const val UID = 10123
    }
}
