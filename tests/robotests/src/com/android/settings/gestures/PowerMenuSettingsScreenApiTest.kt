/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.gestures

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidPreferenceException
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class PowerMenuSettingsScreenApiTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val apiTester = ApiTester(PowerMenuSettingsScreenApi())

    @Before
    fun setup() {
        SettingsShadowResources.reset()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(apiTester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(apiTester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_settingAvailable_isNotNull() {
        setLongPressPowerSettingAvailability(true)

        assertThat(apiTester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_settingUnavailable_fails() {
        setLongPressPowerSettingAvailability(false)

        val failure = assertFailsWith<HardwareUnsupportedException> { apiTester.getLaunchIntent() }
        assertThat(failure.reason)
            .isEqualTo(
                "This device does not support setting the power button to open assistant on long press"
            )
    }

    @Test
    fun longPressPower_notAvailable_categoryPreferenceNotAllowed() {
        setLongPressPowerSettingAvailability(false)

        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                apiTester.get<LongPressPowerActions>(CATEGORY_PREFERENCE_KEY)
            }
        assertThat(failure.reason)
            .isEqualTo(
                "This device does not support setting the power button to open assistant on long press"
            )
    }

    @Test
    fun longPressPower_notAvailable_sensitivityPreferenceNotAllowed() {
        setLongPressPowerSettingAvailability(false)

        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY)
            }
        assertThat(failure.reason)
            .isEqualTo(
                "This device does not support setting the power button to open assistant on long press"
            )
    }

    @Test
    fun longPressPowerCategory_assistant_returnsCorrectValue() {
        setLongPressPowerSettingAvailability(true)

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        assertThat(apiTester.get<LongPressPowerActions>(CATEGORY_PREFERENCE_KEY))
            .isEqualTo(LongPressPowerActions.ASSISTANT.asApiValue)
    }

    @Test
    fun longPressPowerCategory_powerMenu_returnsCorrectValue() {
        setLongPressPowerSettingAvailability(true)

        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(context)

        assertThat(apiTester.get<LongPressPowerActions>(CATEGORY_PREFERENCE_KEY))
            .isEqualTo(LongPressPowerActions.POWER_MENU.asApiValue)
    }

    @Test
    fun longPressPowerCategory_setToAssistant() {
        setLongPressPowerSettingAvailability(true)

        apiTester.set(CATEGORY_PREFERENCE_KEY, LongPressPowerActions.ASSISTANT.asApiValue)

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(context)).isTrue()
    }

    @Test
    fun longPressPowerCategory_setToPowerMenu() {
        setLongPressPowerSettingAvailability(true)

        apiTester.set(CATEGORY_PREFERENCE_KEY, LongPressPowerActions.POWER_MENU.asApiValue)

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(context)).isFalse()
    }

    @Test
    fun assistantLongPressSensitivity_powerMenuSet_invalidPreference() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(TEST_DURATIONS)

        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(context)

        val failure =
            assertFailsWith<InvalidPreferenceException> {
                apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY)
            }
        assertThat(failure.reason)
            .isEqualTo(
                "The device is currently set to launch the Power Menu on long press of the power button"
            )
    }

    @Test
    fun assistantLongPressSensitivity_emptyDurations_NotAllowed() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(IntArray(0))

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY)
            }

        assertThat(failure.reason)
            .isEqualTo(
                "The device does not support setting alternative sensitivities for long pressing power button for Assistant"
            )
    }

    @Test
    fun assistantLongPressSensitivity_singleDuration_NotAllowed() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(IntArray(1))

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY)
            }

        assertThat(failure.reason)
            .isEqualTo(
                "The device does not support setting alternative sensitivities for long pressing power button for Assistant"
            )
    }

    @Test
    fun assistantLongPressSensitivity_getsCurrentValue() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(TEST_DURATIONS)

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        TEST_DURATIONS.forEach { duration ->
            SettingsGlobalStore.get(context).setInt(DURATION_SETTINGS_KEY, duration)

            assertWithMessage("Wrong duration when set to $duration in Settings")
                .that(apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY))
                .isEqualTo(duration)
        }
    }

    @Test
    fun assistantLongPressSensitivity_setToValue() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(TEST_DURATIONS)

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        TEST_DURATIONS.forEach { duration ->
            apiTester.set(SENSITIVITY_PREFERENCE_KEY, duration)

            assertWithMessage("Wrong duration when set to $duration")
                .that(SettingsGlobalStore.get(context).getInt(DURATION_SETTINGS_KEY))
                .isEqualTo(duration)
        }
    }

    @Test
    fun assistantLongPressSensitivity_defaultValue() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(TEST_DURATIONS)

        val defaultDuration = TEST_DURATIONS[3]

        SettingsShadowResources.overrideResource(
            com.android.internal.R.integer.config_longPressOnPowerDurationMs,
            defaultDuration,
        )

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        assertThat(apiTester.get<Int>(SENSITIVITY_PREFERENCE_KEY))
            .isEqualTo(defaultDuration)
    }

    @Test
    fun assistantLongPressSensitivity_setToClosest() {
        setLongPressPowerSettingAvailability(true)
        setSensitivityDurations(TEST_DURATIONS)

        PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)

        apiTester.set(SENSITIVITY_PREFERENCE_KEY, TEST_DURATIONS[0])
        assertThat(SettingsGlobalStore.get(context).getInt(DURATION_SETTINGS_KEY))
            .isEqualTo(TEST_DURATIONS[0])

        apiTester.set(SENSITIVITY_PREFERENCE_KEY, TEST_DURATIONS[1])
        assertThat(SettingsGlobalStore.get(context).getInt(DURATION_SETTINGS_KEY))
            .isEqualTo(TEST_DURATIONS[1])
    }

    private fun setLongPressPowerSettingAvailability(available: Boolean) {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable,
            available,
        )
        if (available) {
            SettingsShadowResources.overrideResource(
                PowerMenuSettingsUtils.POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE,
                PowerMenuSettingsUtils.LONG_PRESS_POWER_GLOBAL_ACTIONS,
            )
        }
    }

    private fun setSensitivityDurations(array: IntArray) {
        SettingsShadowResources.overrideResource(DURATIONS_ARRAY_ID, array)
    }

    private companion object {
        const val CATEGORY_PREFERENCE_KEY = "gesture_power_menu_long_press_category"
        const val SENSITIVITY_PREFERENCE_KEY =
            "gesture_power_menu_long_press_for_assist_sensitivity"
        const val DURATIONS_ARRAY_ID =
            com.android.internal.R.array.config_longPressOnPowerDurationSettings
        const val DURATION_SETTINGS_KEY = Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS
        val TEST_DURATIONS = listOf(100, 200, 300, 500).toIntArray()
    }
}
