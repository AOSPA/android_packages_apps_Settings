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
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class TwoFingerDoubleTapShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private lateinit var preference: TwoFingerDoubleTapShortcutPreference

    @Before
    fun setUp() {
        preference =
            TwoFingerDoubleTapShortcutPreference(appContext, setOf(MAGNIFICATION_CONTROLLER_NAME))
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_two_finger_double_tap_pref")
    }

    @Test
    fun getTitle_returnsCorrectFormattedString() {
        val expectedTitle =
            appContext.getString(
                R.string.accessibility_shortcut_edit_screen_title_two_finger_double_tap,
                2,
            )

        assertThat(preference.getTitle(appContext).toString()).isEqualTo(expectedTitle)
    }

    @Test
    fun getSummary_returnsCorrectFormattedString() {
        val expectedSummary =
            appContext.getString(
                R.string.accessibility_shortcut_edit_screen_summary_two_finger_double_tap,
                2,
            )

        assertThat(preference.getSummary(appContext).toString()).isEqualTo(expectedSummary)
    }

    @Test
    fun bind_setsIntroImage() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageRawResId: Int = ReflectionHelpers.getField(widget, "mIntroImageRawResId")

        assertThat(imageRawResId).isEqualTo(R.raw.accessibility_shortcut_type_2finger_doubletap)
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    fun isAvailable_whenFlagIsDisabled_returnsFalse() {
        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    fun isAvailable_whenFlagIsEnabled_butNoTargets_returnsFalse() {
        preference = TwoFingerDoubleTapShortcutPreference(appContext, emptySet())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    fun isAvailable_whenFlagIsEnabled_butWrongTarget_returnsFalse() {
        preference = TwoFingerDoubleTapShortcutPreference(appContext, setOf("some.other.target"))

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    fun isAvailable_whenFlagIsEnabled_butMultipleTargets_returnsFalse() {
        preference =
            TwoFingerDoubleTapShortcutPreference(
                appContext,
                setOf("some.other.target", MAGNIFICATION_CONTROLLER_NAME),
            )

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    @Test
    fun isAvailable_whenFlagIsEnabled_andTargetIsMagnification_returnsTrue() {
        assertThat(preference.isAvailable(appContext)).isTrue()
    }
}
