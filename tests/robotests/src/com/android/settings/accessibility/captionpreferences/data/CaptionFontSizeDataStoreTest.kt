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
import android.view.accessibility.CaptioningManager
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class CaptionFontSizeDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val captioningManager: CaptioningManager = mock()
    private val observer: KeyedObserver<String> = mock()
    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
        }
    private val dataStore = CaptionFontSizeDataStore(context)
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun contains_returnsTrueWhenSettingContainsNonNullValue() {
        settingsSecureStore.setFloat(SETTING_KEY, 1.5f)
        assertThat(dataStore.contains(SETTING_KEY)).isTrue()
    }

    @Test
    fun contains_returnsFalseWhenSettingContainsNullValue() {
        settingsSecureStore.setString(SETTING_KEY, null)
        assertThat(dataStore.contains(SETTING_KEY)).isFalse()
    }

    @Test
    fun getDefaultValue_returnsOne() {
        assertThat(dataStore.getDefaultValue(SETTING_KEY, Float::class.javaObjectType))
            .isEqualTo(1f)
        assertThat(dataStore.getDefaultValue(SETTING_KEY, String::class.javaObjectType))
            .isEqualTo("1.0")
    }

    @Test
    fun getValue_returnsFromCaptioningManager() {
        captioningManager.stub { on { fontScale } doReturn 2.0f }
        assertThat(dataStore.getValue(SETTING_KEY, Float::class.javaObjectType))
            .isWithin(tolerance)
            .of(2.0f)
        assertThat(dataStore.getValue(SETTING_KEY, String::class.javaObjectType)).isEqualTo("2.0")
    }

    @Test
    fun setValue_updatesSettingsAndEnablesCaptions() {
        dataStore.setValue(SETTING_KEY, Float::class.javaObjectType, 1.25f)

        assertThat(settingsSecureStore.getFloat(SETTING_KEY)).isWithin(tolerance).of(1.25f)
        assertThat(settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED))
            .isTrue()
    }

    @Test
    fun setValue_stringType_updatesSettings() {
        dataStore.setValue(SETTING_KEY, String::class.javaObjectType, "1.5")

        assertThat(settingsSecureStore.getFloat(SETTING_KEY)).isWithin(tolerance).of(1.5f)
    }

    @Test
    fun observer_onFontScaleChanged_notifiesChange() {
        val listenerCaptor = argumentCaptor<CaptioningManager.CaptioningChangeListener>()
        dataStore.addObserver(SETTING_KEY, observer, HandlerExecutor.main)

        verify(captioningManager).addCaptioningChangeListener(listenerCaptor.capture())

        listenerCaptor.lastValue.onFontScaleChanged(1.5f)
        ShadowLooper.idleMainLooper()

        verify(observer).onKeyChanged(SETTING_KEY, DataChangeReason.UPDATE)
    }

    @Test
    fun removeObserver_unregistersFromCaptioningManager() {
        val listenerCaptor = argumentCaptor<CaptioningManager.CaptioningChangeListener>()
        dataStore.addObserver(SETTING_KEY, observer, HandlerExecutor.main)
        verify(captioningManager).addCaptioningChangeListener(listenerCaptor.capture())

        dataStore.removeObserver(SETTING_KEY, observer)
        verify(captioningManager).removeCaptioningChangeListener(listenerCaptor.lastValue)
    }

    companion object {
        private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE
        private const val tolerance = 1e-5f
    }
}
