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

package com.android.settings.accessibility.captionpreferences.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class CaptionFontFamilyDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsSecureStore = SettingsSecureStore.get(context)
    private val dataStore = CaptionFontFamilyDataStore(context)
    private val observer: KeyedObserver<String> = mock()

    @Test
    fun contains_returnsCorrectValue() {
        settingsSecureStore.setString(SETTING_KEY, "sans-serif")
        assertThat(dataStore.contains(SETTING_KEY)).isTrue()

        settingsSecureStore.setString(SETTING_KEY, null)
        assertThat(dataStore.contains(SETTING_KEY)).isFalse()
    }

    @Test
    fun getDefaultValue_returnsEmptyString() {
        assertThat(dataStore.getDefaultValue(SETTING_KEY, String::class.javaObjectType))
            .isEqualTo("")
    }

    @Test
    fun getValue_returnsFromSettings() {
        settingsSecureStore.setString(SETTING_KEY, "serif")
        assertThat(dataStore.getValue(SETTING_KEY, String::class.javaObjectType)).isEqualTo("serif")
    }

    @Test
    fun setValue_updatesSettingsAndEnablesCaptions() {
        dataStore.setValue(SETTING_KEY, String::class.javaObjectType, "monospace")

        assertThat(settingsSecureStore.getString(SETTING_KEY)).isEqualTo("monospace")
        assertThat(settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED))
            .isEqualTo(true)
    }

    @Test
    fun addObserver_registersWithSettingsSecureStore() {
        dataStore.addObserver(SETTING_KEY, observer, HandlerExecutor.main)

        settingsSecureStore.notifyChange(SETTING_KEY, DataChangeReason.UPDATE)
        ShadowLooper.idleMainLooper()

        verify(observer).onKeyChanged(SETTING_KEY, DataChangeReason.UPDATE)
    }

    @Test
    fun removeObserver_unregistersFromSettingsSecureStore() {
        dataStore.addObserver(SETTING_KEY, observer, HandlerExecutor.main)
        dataStore.removeObserver(SETTING_KEY, observer)

        settingsSecureStore.notifyChange(SETTING_KEY, DataChangeReason.UPDATE)
        ShadowLooper.idleMainLooper()

        verify(observer, never()).onKeyChanged(SETTING_KEY, DataChangeReason.UPDATE)
    }

    companion object {
        private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE
    }
}
