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

package com.android.settings.network

import android.app.settings.SettingsEnums.ACTION_AIRPLANE_MODE_SYNC_TOGGLE
import android.content.Context
import android.provider.Settings
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AirplaneModeSyncPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AirplaneModeSyncPreference()

    @Test
    fun key_returnsCorrectKey() {
        assertThat(preference.key).isEqualTo(AirplaneModeSyncPreference.KEY)
    }

    @Test
    fun title_returnsCorrectTitle() {
        assertThat(preference.title).isEqualTo(R.string.sync_across_devices_title)
    }

    @Test
    fun getReadPermissions_returnsSettingsGlobalStorePermissions() {
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(SettingsGlobalStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsGlobalStorePermissions() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(SettingsGlobalStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun storage_returnsSettingsGlobalStore() {
        assertThat(preference.storage(context)).isInstanceOf(SettingsGlobalStore::class.java)
    }

    @Test
    fun sensitivityLevel_isNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.HIGH_SENSITIVITY)
    }

    @Test
    fun preferenceActionMetrics_returnsCorrectValue() {
        assertThat(preference.preferenceActionMetrics).isEqualTo(ACTION_AIRPLANE_MODE_SYNC_TOGGLE)
    }

    @Test
    fun switch_isChecked_whenSettingIsTrue() {
        Settings.Global.putInt(context.contentResolver, AirplaneModeSyncPreference.KEY, 1)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        assertThat(switchPreference.isChecked).isTrue()
    }

    @Test
    fun switch_isNotChecked_whenSettingIsNotSet() {
        Settings.Global.putString(context.contentResolver, AirplaneModeSyncPreference.KEY, null)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun switch_isNotChecked_whenSettingIsFalse() {
        Settings.Global.putInt(context.contentResolver, AirplaneModeSyncPreference.KEY, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        assertThat(switchPreference.isChecked).isFalse()
    }

    @Test
    fun performClick_togglesSetting() {
        Settings.Global.putInt(context.contentResolver, AirplaneModeSyncPreference.KEY, 0)
        val switchPreference = preference.createAndBindWidget<SwitchPreferenceCompat>(context)

        switchPreference.performClick()
        assertThat(
                Settings.Global.getInt(context.contentResolver, AirplaneModeSyncPreference.KEY, 0)
            )
            .isEqualTo(1)
        assertThat(switchPreference.isChecked).isTrue()

        switchPreference.performClick()
        assertThat(
                Settings.Global.getInt(context.contentResolver, AirplaneModeSyncPreference.KEY, 1)
            )
            .isEqualTo(0)
        assertThat(switchPreference.isChecked).isFalse()
    }
}
