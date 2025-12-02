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
package com.android.settings.accessibility.systemcontrols.data

import android.content.Context
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [PowerButtonEndsCallDataStore] */
@RunWith(AndroidJUnit4::class)
class PowerButtonEndsCallDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dataStore = PowerButtonEndsCallDataStore(context)
    private val settingsStore = SettingsSecureStore.get(context)

    @Test
    fun getKeyValueStoreDelegate_equalsSettingsStore() {
        assertThat(dataStore.keyValueStoreDelegate).isEqualTo(settingsStore)
    }

    @Test
    fun getBoolean_hangupEnabled_shouldReturnTrue() {
        settingsStore.setInt(PowerButtonEndsCallDataStore.KEY, INCALL_POWER_BUTTON_BEHAVIOR_HANGUP)

        assertThat(dataStore.getBoolean(PowerButtonEndsCallDataStore.KEY)).isTrue()
    }

    @Test
    fun getBoolean_screenOffEnabled_shouldReturnFalse() {
        settingsStore.setInt(
            PowerButtonEndsCallDataStore.KEY,
            INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF,
        )

        assertThat(dataStore.getBoolean(PowerButtonEndsCallDataStore.KEY)).isFalse()
    }

    @Test
    fun setBoolean_true_shouldStoreHangup() {
        dataStore.setBoolean(PowerButtonEndsCallDataStore.KEY, true)

        assertThat(settingsStore.getInt(PowerButtonEndsCallDataStore.KEY))
            .isEqualTo(INCALL_POWER_BUTTON_BEHAVIOR_HANGUP)
    }

    @Test
    fun setBoolean_false_shouldStoreScreenOff() {
        dataStore.setBoolean(PowerButtonEndsCallDataStore.KEY, false)

        assertThat(settingsStore.getInt(PowerButtonEndsCallDataStore.KEY))
            .isEqualTo(INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF)
    }

    @Test
    fun setValue_true_shouldStoreHangup() {
        dataStore.setValue(PowerButtonEndsCallDataStore.KEY, Boolean::class.javaObjectType, true)

        assertThat(settingsStore.getInt(PowerButtonEndsCallDataStore.KEY))
            .isEqualTo(INCALL_POWER_BUTTON_BEHAVIOR_HANGUP)
    }

    @Test
    fun getValue_hangupEnabled_shouldReturnTrue() {
        settingsStore.setInt(PowerButtonEndsCallDataStore.KEY, INCALL_POWER_BUTTON_BEHAVIOR_HANGUP)

        assertThat(
                dataStore.getValue(PowerButtonEndsCallDataStore.KEY, Boolean::class.javaObjectType)
            )
            .isTrue()
    }
}
