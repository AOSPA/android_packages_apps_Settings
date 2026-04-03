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
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_LAUNCH_SOURCE
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_PACKAGE_NAME
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_UID
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.LaunchSourceType
import com.android.settings.fuelgauge.PowerBackgroundUsageDetailScreen.Companion.SWITCH_KEY
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(AndroidJUnit4::class)
class PowerBackgroundUsageDetailScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var tester: ApiTester

    private val mockFeatureFactory = mock<FeatureFactory>()
    private val mockPackageManager = mock<PackageManager>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = spy(baseContext)
        FeatureFactory.setFactory(context, mockFeatureFactory)
        context.stub { on { packageManager } doReturn mockPackageManager }

        val testAppInfo = ApplicationInfo().apply {
            this.packageName = PACKAGE_NAME
            this.uid = UID
        }
        mockPackageManager.stub {
            on { getPackageUidAsUser(eq(PACKAGE_NAME), anyInt()) } doReturn UID
            on {
                getInstalledApplicationsAsUser(
                    any<ApplicationInfoFlags>(),
                    anyInt()
                )
            } doReturn listOf(testAppInfo)
        }

        tester = ApiTester(PowerBackgroundUsageDetailScreen(), context)
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
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getLaunchIntent_modeMutable_hasIntent() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(false)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(false)

        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))

        val extras = tester.getLaunchScreenExtras()
        assertThat(extras.keySet()).hasSize(3)
        assertThat(extras.getString(EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
        assertThat(extras.getInt(EXTRA_UID)).isEqualTo(UID)
        assertThat(extras.getString(EXTRA_LAUNCH_SOURCE))
            .isEqualTo(LaunchSourceType.SETTINGS_API.name)
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getLaunchIntent_disableForOptimizeModeOnly_throwFailedPreconditionException() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(true)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(false)

        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))

        assertThat(tester.getLaunchScreenExtras().keySet()).hasSize(3)
        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getLaunchIntent_systemApp_throwFailedPreconditionException() {
        ShadowBatteryOptimizeUtils.setDisableForOptimizeModeOnly(false)
        ShadowBatteryOptimizeUtils.setSystemOrDefaultApp(true)

        tester.initializeScreenParameters(Parameters(EXTRA_PACKAGE_NAME to PACKAGE_NAME))

        assertThat(tester.getLaunchScreenExtras().keySet()).hasSize(3)
        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun getBackgroundUsageAllowabilitySwitch_returnExpectedResult() {
        setupTesterAndShadows()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isTrue()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_UNRESTRICTED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isTrue()

        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        assertThat(tester.get<Boolean>(SWITCH_KEY)).isFalse()
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun setBackgroundUsageAllowabilitySwitch_toggleOn_returnExpectedResult() {
        setupTesterAndShadows()

        // Original mode: Restricted
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_OPTIMIZED,
            Action.SETTINGS_API_APPLY,
        )

        // Original mode: Optimized
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_OPTIMIZED,
            Action.UNKNOWN,
        ) // skip for the same mode

        // Original mode: Unrestricted
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_UNRESTRICTED)
        tester.set(SWITCH_KEY, true)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_UNRESTRICTED,
            Action.UNKNOWN,
        ) // skip for the same mode
    }

    @Test
    @Config(shadows = [ShadowBatteryOptimizeUtils::class])
    fun setBackgroundUsageAllowabilitySwitch_toggleOff_returnExpectedResult() {
        setupTesterAndShadows()

        // Original mode: Restricted
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_RESTRICTED)
        tester.set(SWITCH_KEY, false)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_RESTRICTED,
            Action.UNKNOWN,
        ) // skip for the same mode

        // Original mode: Optimized
        ShadowBatteryOptimizeUtils.setMode(BatteryOptimizeUtils.MODE_OPTIMIZED)
        tester.set(SWITCH_KEY, false)
        ShadowBatteryOptimizeUtils.assertAppUsageState(
            BatteryOptimizeUtils.MODE_RESTRICTED,
            Action.SETTINGS_API_APPLY,
        )

        // Original mode: Unrestricted
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
        const val PACKAGE_NAME = "com.abc"
        const val UID = 10123
    }
}

@Implements(BatteryOptimizeUtils::class)
internal class ShadowBatteryOptimizeUtils {

    @Implementation
    fun isDisabledForOptimizeModeOnly(): Boolean {
        return disableForOptimizeModeOnly
    }

    @Implementation
    fun isSystemOrDefaultApp(): Boolean {
        return systemOrDefaultApp
    }

    @Implementation
    fun setAppUsageState(mode: Int, action: Action) {
        optMode = mode
        optAction = action
    }

    @Implementation
    fun getAppOptimizationMode(): Int {
        return optMode
    }

    companion object {
        private var disableForOptimizeModeOnly = false
        private var systemOrDefaultApp = false

        private var optMode = BatteryOptimizeUtils.MODE_OPTIMIZED
        private var optAction = Action.UNKNOWN

        fun assertAppUsageState(mode: Int, action: Action) {
            assertThat(optMode).isEqualTo(mode)
            assertThat(optAction).isEqualTo(action)
        }

        @JvmStatic
        internal fun setDisableForOptimizeModeOnly(disable: Boolean) {
            disableForOptimizeModeOnly = disable
        }

        @JvmStatic
        internal fun setSystemOrDefaultApp(isSystem: Boolean) {
            systemOrDefaultApp = isSystem
        }

        @JvmStatic
        internal fun setMode(mode: Int, action: Action = Action.UNKNOWN) {
            optMode = mode
            optAction = action
        }
    }
}
