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

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class DoubleTapPowerMainSwitchPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val preference = DoubleTapPowerMainSwitchPreference()

    @SuppressLint("VisibleForTests")
    private fun getSwitchPreference(): MainSwitchPreference =
        preference.createAndBindWidget(context)

    private val key = DoubleTapPowerMainSwitchPreference.KEY

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_configIsMultiTarget_shouldReturnTrue() {
        SettingsShadowResources.overrideResource(
            R.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_MULTI_TARGET_MODE,
        )

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_configIsNotMultiTarget_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(
            R.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_DISABLED_MODE,
        )

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isChecked_ResetAsDefault_shouldReturnTrue() {
        SettingsSecureStore.get(context).setInt(key, null)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun isChecked_setStorageOn_shouldReturnTrue() {
        preference.storage(context).setBoolean(key, true)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun isChecked_setStorageOff_shouldReturnFalse() {
        preference.storage(context).setBoolean(key, false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun performClick_setChecked_shouldBecomeUnchecked() {
        preference.storage(context).setBoolean(key, true)

        getSwitchPreference().performClick()

        assertThat(preference.storage(context).getBoolean(key)).isFalse()
    }

    @Test
    fun performClick_setUnchecked_shouldBecomeChecked() {
        preference.storage(context).setBoolean(key, false)

        getSwitchPreference().performClick()

        assertThat(preference.storage(context).getBoolean(key)).isTrue()
    }
}
// LINT.ThenChange(DoubleTapPowerMainSwitchPreferenceControllerTest.java)
