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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider

import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Rule

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class FollowKeyboardSwitchPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()

    private val context : Context = ApplicationProvider.getApplicationContext()
    private val preference = FollowKeyboardSwitchPreference()

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_KEYBOARD_ENABLED)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_screen_magnification_follow_keyboard_title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.summary)
            .isEqualTo(R.string.accessibility_screen_magnification_follow_keyboard_summary)
    }

    @Test
    fun performClick_switchOn_assertSwitchOff() {
        getStorage().setBoolean(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_KEYBOARD_ENABLED, true)
        val preferenceWidget = createFollowKeyboardWidget()
        assertThat(preferenceWidget.isChecked).isTrue()

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isFalse()
        assertThat(getStorage().getBoolean(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_KEYBOARD_ENABLED)).isFalse()
    }

    @Test
    fun performClick_switchOff_assertSwitchOn() {
        getStorage().setBoolean(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_KEYBOARD_ENABLED, false)
        val preferenceWidget = createFollowKeyboardWidget()
        assertThat(preferenceWidget.isChecked).isFalse()

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isTrue()
        assertThat(getStorage().getBoolean(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_KEYBOARD_ENABLED)).isTrue()
    }

    @Test
    fun isAvailable_inSetupWizard_isUnavailable() {
        assertIsAvailable(
            inSetupWizard = true,
            hasHardwareKeyboard = true,
            expectedAvailability = false,
        )
    }

    @Test
    fun isAvailable_noHardwareKeyboard_isUnavailable() {
        assertIsAvailable(
            inSetupWizard = false,
            hasHardwareKeyboard = false,
            expectedAvailability = false,
        )
    }

    @Test
    fun isAvailable_hasHardwareKeyboard_isAvailable() {
        assertIsAvailable(
            inSetupWizard = false,
            hasHardwareKeyboard = true,
            expectedAvailability = true,
        )
    }

    private fun assertIsAvailable(
        inSetupWizard: Boolean,
        hasHardwareKeyboard: Boolean,
        expectedAvailability: Boolean,
    ) {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                    ComponentActivity(),
                    Intent().apply { putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard) },
                )
                    .create()
                    .start()
                    .postCreate(null)
                    .resume()
            setHardwareKeyboard(hasHardwareKeyboard)
            assertThat(preference.isAvailable(activityController.get()))
                .isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun setHardwareKeyboard(hasConnectedMouse: Boolean) {
        if (hasConnectedMouse) {
            val device = ShadowInputDevice.makeFullKeyboardInputDevicebyId(/* id= */ 1)
            ShadowInputDevice.addDevice(device.id, device)
        } else {
            ShadowInputDevice.reset()
        }
    }

    private fun createFollowKeyboardWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}