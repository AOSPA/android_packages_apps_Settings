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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.Flags.FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [EditVolumeKeysShortcutController]. */
@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class EditVolumeKeysShortcutControllerTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val item = IllustrationCheckBoxItem()

    @Test
    fun bindData_setsSummary() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.summary)
            .isEqualTo(
                appContext.getString(R.string.accessibility_shortcut_edit_dialog_summary_hardware)
            )
    }

    @Test
    @DisableFlags(FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH)
    fun bindData_defaultAndFlagOff_setsMobileImage() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
    }

    @Test
    @DisableFlags(FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH)
    fun bindData_desktopSupportedAndFlagOff_setsMobileImage() {
        AccessibilityTestUtils.setDesktopSupported(appContext, true)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
    }

    @Test
    @DisableFlags(FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH)
    fun bindData_desktopNotSupportedAndFlagOff_setsMobileImage() {
        AccessibilityTestUtils.setDesktopSupported(appContext, false)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
    }

    @Test
    @EnableFlags(FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH)
    fun bindData_desktopSupportedAndFlagOn_setsDesktopImage() {
        AccessibilityTestUtils.setDesktopSupported(appContext, true)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_keyboard_volume_keys)
    }

    @Test
    @EnableFlags(FLAG_DESKTOP_MAGNIFICATION_SETTINGS_POLISH)
    fun bindData_desktopNotSupportedAndFlagOn_setsMobileImage() {
        AccessibilityTestUtils.setDesktopSupported(appContext, false)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditVolumeKeysShortcutController.create(appContext, item, targets)
}
