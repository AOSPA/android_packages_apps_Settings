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

package com.android.settings.accessibility.shortcuts.ui

import android.app.Application
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.getAccessibilityFeatureName
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class, ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class KeyboardShortcutPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private lateinit var preference: KeyboardShortcutPreference

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultAccessibilityService,
            "talkback",
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSelectToSpeakService,
            "select_to_speak",
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultVoiceAccessService,
            "voice_access",
        )
        setHardwareKeyboard(true)
        preference =
            KeyboardShortcutPreference(
                context = appContext,
                targets = setOf(MAGNIFICATION_CONTROLLER_NAME),
            )
    }

    @Test
    fun title_returnsCorrectTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_keyboard)
    }

    @Test
    fun getSummary_returnsCorrectSummary() {
        val meta: String? = appContext.getString(R.string.modifier_keys_meta)
        val alt: String? = appContext.getString(R.string.modifier_keys_alt)
        val magnificationName =
            getAccessibilityFeatureName(appContext, MAGNIFICATION_CONTROLLER_NAME)
        assertThat(preference.getSummary(appContext))
            .isEqualTo(
                appContext.getString(
                    R.string.accessibility_shortcut_edit_dialog_summary_keyboard,
                    meta,
                    alt,
                    "M",
                    magnificationName,
                )
            )
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenFlagDisabled_returnsFalse() {
        preference = KeyboardShortcutPreference(context = appContext, targets = emptySet())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenNoTargets_returnsFalse() {
        preference = KeyboardShortcutPreference(context = appContext, targets = emptySet())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenNoKeyboard_returnsFalse() {
        setHardwareKeyboard(false)
        preference = KeyboardShortcutPreference(context = appContext, targets = emptySet())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenWrongTarget_returnsFalse() {
        preference =
            KeyboardShortcutPreference(context = appContext, targets = setOf("some.other.target"))

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenTargetIsMagnification_returnsTrue() {
        preference =
            KeyboardShortcutPreference(
                context = appContext,
                targets = setOf(MAGNIFICATION_CONTROLLER_NAME),
            )

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenTargetIsScreenReader_returnsTrue() {
        preference = KeyboardShortcutPreference(context = appContext, targets = setOf("talkback"))

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    @EnableFlags(
        com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS,
        com.android.hardware.input.Flags.FLAG_ENABLE_SELECT_TO_SPEAK_KEY_GESTURES,
    )
    fun isAvailable_whenTargetIsSelectToSpeak_returnsTrue() {
        preference =
            KeyboardShortcutPreference(context = appContext, targets = setOf("select_to_speak"))

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun isAvailable_whenTargetIsVoiceAccess_returnsTrue() {
        preference =
            KeyboardShortcutPreference(context = appContext, targets = setOf("voice_access"))

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    @EnableFlags(
        com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS,
        com.android.hardware.input.Flags.FLAG_ENABLE_COLOR_INVERSION_KEY_GESTURES,
    )
    fun isAvailable_whenTargetIsColorInversion_returnsTrue() {
        preference =
            KeyboardShortcutPreference(
                context = appContext,
                targets = setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString()),
            )

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    private fun setHardwareKeyboard(hasConnectedKeyboard: Boolean) {
        if (hasConnectedKeyboard) {
            val device = ShadowInputDevice.makeFullKeyboardInputDevicebyId(/* id= */ 1)
            ShadowInputDevice.addDevice(device.id, device)
        } else {
            ShadowInputDevice.reset()
        }
    }
}
