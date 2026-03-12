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

package com.android.settings.accessibility.setupwizard

import android.app.Application
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.hardware.input.Flags.FLAG_ENABLE_COLOR_INVERSION_KEY_GESTURES
import com.android.hardware.input.Flags.FLAG_ENABLE_SELECT_TO_SPEAK_KEY_GESTURES
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.server.accessibility.Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [EditKeyboardShortcutController]. */
@Config(shadows = [SettingsShadowResources::class, ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class EditKeyboardShortcutControllerTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val item = IllustrationCheckBoxItem()

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
        setHardwareKeyboard(true)
    }

    @Test
    fun bindData_multipleTargets_setsNullSummaryAndNoImage() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME, "talkback"))

        controller.bindData(item)

        assertThat(item.summary).isNull()
        assertThat(item.imageResId).isEqualTo(Resources.ID_NULL)
    }

    @Test
    fun bindData_magnificationTarget_setsSummaryAndImage() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.imageResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_keyboard_magnification)
    }

    @Test
    fun bindData_colorInversionTarget_setsSummaryAndImage() {
        val target = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val controller = createController(setOf(target))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.imageResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_keyboard_colorinversion)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SELECT_TO_SPEAK_KEY_GESTURES)
    fun bindData_selectToSpeakTarget_withFlagEnabled_setsSummaryAndImage() {
        val controller = createController(setOf("select_to_speak"))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.imageResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_keyboard_selecttospeak)
    }

    @Test
    fun bindData_unknownTarget_setsSummaryButNoImage() {
        val controller = createController(setOf("unknown_target"))

        controller.bindData(item)

        assertSummaryContainsMetaKey()
        assertThat(item.imageResId).isEqualTo(Resources.ID_NULL)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun bindData_magnificationTarget_showsItem() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun bindData_magnificationTarget_withoutKeyboard_hidesItem() {
        setHardwareKeyboard(false)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun bindData_magnificationTarget_withFlagDisabled_hidesItem() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun bindData_unknownTarget_hidesItem() {
        val controller = createController(setOf("unknown_target"))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    fun bindData_talkbackTarget_showsItem() {
        val controller = createController(setOf("talkback"))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    @EnableFlags(
        FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS,
        FLAG_ENABLE_SELECT_TO_SPEAK_KEY_GESTURES,
    )
    fun bindData_selectToSpeakTarget_withFlagsEnabled_showsItem() {
        val controller = createController(setOf("select_to_speak"))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    @EnableFlags(
        FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS,
        FLAG_ENABLE_COLOR_INVERSION_KEY_GESTURES,
    )
    fun bindData_colorInversionTarget_withFlagsEnabled_showsItem() {
        val target = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val controller = createController(setOf(target))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditKeyboardShortcutController.create(appContext, item, targets)

    private fun setHardwareKeyboard(hasConnectedKeyboard: Boolean) =
        if (hasConnectedKeyboard) {
            ShadowInputDevice.makeFullKeyboardInputDevicebyId(1).run {
                ShadowInputDevice.addDevice(id, this)
            }
        } else {
            ShadowInputDevice.reset()
        }

    private fun assertSummaryContainsMetaKey() {
        val metaKeyLabel = appContext.getString(R.string.modifier_keys_meta)
        assertThat(item.summary.toString()).contains(metaKeyLabel)
    }
}
