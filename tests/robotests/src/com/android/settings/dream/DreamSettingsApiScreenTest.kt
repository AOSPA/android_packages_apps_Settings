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

package com.android.settings.dream

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.flags.Flags
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settingslib.dream.DreamBackend
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class DreamSettingsApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DreamSettingsApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsShowWhenToDreamSetting,
            true
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsEnabledOnBattery,
            true
        )
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values,
            arrayOf(
                "while_charging_only",
                "while_docked_only",
                "either_charging_or_docked",
                "while_postured_only",
                "never"
            )
        )
        SettingsShadowResources.overrideResource(
            R.bool.config_posturing_supported,
            true
        )
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

    enum class WhenToDreamTestCase(
        val dock: Boolean,
        val sleep: Boolean,
        val postured: Boolean,
        val expected: DreamSettingsApiScreen.WhenToDream
    ) {
        NEVER(
            false,
            false,
            false,
            DreamSettingsApiScreen.WhenToDream.NEVER
        ),
        WHILE_CHARGING(
            false,
            true,
            false,
            DreamSettingsApiScreen.WhenToDream.WHILE_CHARGING
        ),
        WHILE_DOCKED(
            true,
            false,
            false,
            DreamSettingsApiScreen.WhenToDream.WHILE_DOCKED
        ),
        EITHER(
            true,
            true,
            false,
            DreamSettingsApiScreen.WhenToDream.WHILE_CHARGING_OR_DOCKED
        )
    }

    @Test
    fun getWhenToDream_returnsCorrectValue(
        @TestParameter testCase: WhenToDreamTestCase
    ) {
        setDreamSettings(dock = testCase.dock, sleep = testCase.sleep, postured = testCase.postured)

        val value = tester.get<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        )

        assertThat(value).isEqualTo(testCase.expected.asApiValue)
    }

    enum class SetWhenToDreamTestCase(
        val value: DreamSettingsApiScreen.WhenToDream,
        val expectedEnabled: Boolean,
        val expectedDock: Boolean?,
        val expectedSleep: Boolean?
    ) {
        NEVER(
            DreamSettingsApiScreen.WhenToDream.NEVER,
            false,
            null,
            null
        ),
        WHILE_CHARGING(
            DreamSettingsApiScreen.WhenToDream.WHILE_CHARGING,
            true,
            false,
            true
        ),
        WHILE_DOCKED(
            DreamSettingsApiScreen.WhenToDream.WHILE_DOCKED,
            true,
            true,
            false
        )
    }

    @Test
    fun setWhenToDream_updatesSettings(
        @TestParameter testCase: SetWhenToDreamTestCase
    ) {
        tester.set(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM,
            testCase.value.asApiValue
        )

        val backend = DreamBackend.getInstance(context)
        assertThat(backend.isEnabled).isEqualTo(testCase.expectedEnabled)

        if (testCase.expectedDock != null) {
            assertThat(isActivatedOnDock()).isEqualTo(testCase.expectedDock)
        }
        if (testCase.expectedSleep != null) {
            assertThat(isActivatedOnSleep()).isEqualTo(testCase.expectedSleep)
        }
    }

    @Test
    fun setWhenToDream_unavailable_throwsFailedPreconditionException() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsShowWhenToDreamSetting,
            false
        )

        assertFailsWith<FailedPreconditionException> {
            tester.get<Int>(
                DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
            )
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getWhenToDreamOptions_filtersOptionsBasedOnConfig() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsEnabledOnBattery,
            true
        )
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values,
            arrayOf(
                "while_charging_only",
                "while_docked_only",
                "while_postured_only",
                "never"
            )
        )

        // Case 1: Posturing not supported
        SettingsShadowResources.overrideResource(
            R.bool.config_posturing_supported,
            false
        )

        var options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).contains(
            DreamSettingsApiScreen.WhenToDream.WHILE_CHARGING.asApiValue)
        assertThat(options).contains(
            DreamSettingsApiScreen.WhenToDream.WHILE_DOCKED.asApiValue)
        assertThat(options).doesNotContain(
            DreamSettingsApiScreen.WhenToDream.WHILE_POSTURED.asApiValue)
        assertThat(options).contains(
            DreamSettingsApiScreen.WhenToDream.NEVER.asApiValue
        )

        // Case 2: Posturing supported
        SettingsShadowResources.overrideResource(
            R.bool.config_posturing_supported,
            true
        )

        options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).contains(DreamSettingsApiScreen.WhenToDream.WHILE_POSTURED.asApiValue)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getWhenToDreamOptions_filtersOptionsBasedOnResourceValues() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsEnabledOnBattery,
            true
        )

        // Case 1: Only allow "never"
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values,
            arrayOf("never")
        )

        var options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).containsExactly(DreamSettingsApiScreen.WhenToDream.NEVER.asApiValue)

        // Case 2: Allow "while_charging_only" and "never"
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values,
            arrayOf("while_charging_only", "never")
        )

        options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).containsExactly(
            DreamSettingsApiScreen.WhenToDream.WHILE_CHARGING.asApiValue,
            DreamSettingsApiScreen.WhenToDream.NEVER.asApiValue
        )

        // Case 3: Allow "while_docked_only" and "never"
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values,
            arrayOf("while_docked_only", "never")
        )

        options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).containsExactly(
            DreamSettingsApiScreen.WhenToDream.WHILE_DOCKED.asApiValue,
            DreamSettingsApiScreen.WhenToDream.NEVER.asApiValue
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getWhenToDreamOptions_disabledOnBattery_usesNoBatteryResource() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_dreamsEnabledOnBattery,
            false
        )
        SettingsShadowResources.overrideResource(
            R.array.when_to_start_screensaver_values_no_battery,
            arrayOf("while_docked_only", "never")
        )

        val options = runBlocking { tester.getPreferenceOptions<Int>(
            DreamSettingsApiScreen.PREF_KEY_WHEN_TO_DREAM
        ).map { it.first } }

        assertThat(options).containsExactly(
            DreamSettingsApiScreen.WhenToDream.WHILE_DOCKED.asApiValue,
            DreamSettingsApiScreen.WhenToDream.NEVER.asApiValue
        )
    }

    private fun setDreamSettings(
        dock: Boolean,
        sleep: Boolean,
        postured: Boolean
    ) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK,
            if (dock) 1 else 0
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.SCREENSAVER_ACTIVATE_ON_SLEEP,
            if (sleep) 1 else 0
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.SCREENSAVER_ACTIVATE_ON_POSTURED,
            if (postured) 1 else 0
        )
    }

    private fun isActivatedOnDock(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK,
            0
        ) == 1
    }

    private fun isActivatedOnSleep(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.SCREENSAVER_ACTIVATE_ON_SLEEP,
            0
        ) == 1
    }

    enum class LowLightModeTestCase(
        val settingValue: Int,
        val expected: Boolean
    ) {
        ENABLED(1, true),
        DISABLED(0, false)
    }

    @Test
    @EnableFlags(android.os.Flags.FLAG_LOW_LIGHT_DREAM_BEHAVIOR)
    fun getLowLightMode_returnsCorrectValue(@TestParameter testCase: LowLightModeTestCase) {
        setupLowLightResources()
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_ENABLED,
            testCase.settingValue
        )

        val value = tester.get<Boolean>(DreamSettingsApiScreen.PREF_KEY_LOW_LIGHT_MODE)

        assertThat(value).isEqualTo(testCase.expected)
    }

    enum class SetLowLightModeTestCase(
        val value: Boolean,
        val expectedSetting: Int
    ) {
        TRUE(true, 1),
        FALSE(false, 0)
    }

    @Test
    @EnableFlags(android.os.Flags.FLAG_LOW_LIGHT_DREAM_BEHAVIOR)
    fun setLowLightMode_updatesSettings(@TestParameter testCase: SetLowLightModeTestCase) {
        setupLowLightResources()
        tester.set(DreamSettingsApiScreen.PREF_KEY_LOW_LIGHT_MODE, testCase.value)

        val value = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_ENABLED,
            2 // Default not 0 or 1
        )
        assertThat(value).isEqualTo(testCase.expectedSetting)
    }

    @Test
    @DisableFlags(android.os.Flags.FLAG_LOW_LIGHT_DREAM_BEHAVIOR)
    fun getLowLightMode_flagDisabled_throwsFailedPreconditionException() {
        setupLowLightResources()
        assertFailsWith<FailedPreconditionException> {
            tester.get<Boolean>(DreamSettingsApiScreen.PREF_KEY_LOW_LIGHT_MODE)
        }
    }

    @Test
    @EnableFlags(android.os.Flags.FLAG_LOW_LIGHT_DREAM_BEHAVIOR)
    fun getLowLightMode_noBehaviors_throwsFailedPreconditionException() {
        SettingsShadowResources.overrideResource(
            R.array.low_light_display_behavior_supported_values,
            arrayOf<String>()
        )
        SettingsShadowResources.overrideResource(
            R.array.low_light_display_behavior_values,
            arrayOf<String>()
        )

        assertFailsWith<FailedPreconditionException> {
            tester.get<Boolean>(DreamSettingsApiScreen.PREF_KEY_LOW_LIGHT_MODE)
        }
    }

    private fun setupLowLightResources() {
        SettingsShadowResources.overrideResource(
            R.array.low_light_display_behavior_supported_values,
            arrayOf("behavior")
        )
        SettingsShadowResources.overrideResource(
            R.array.low_light_display_behavior_values,
            arrayOf("behavior")
        )
    }
}
