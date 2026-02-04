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
import android.graphics.Color
import android.provider.Settings
import android.view.accessibility.CaptioningManager.CaptionStyle
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptionEdgeDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun captionEdgeTypeDataStore_getValue_returnsFromSettings() {
        val dataStore = CaptionEdgeTypeDataStore(context)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
            CaptionStyle.EDGE_TYPE_DROP_SHADOW,
        )

        assertThat(dataStore.getValue(KEY, Int::class.javaObjectType))
            .isEqualTo(CaptionStyle.EDGE_TYPE_DROP_SHADOW)
    }

    @Test
    fun captionEdgeTypeDataStore_setValue_updatesSettingsAndEnablesCaptions() {
        val dataStore = CaptionEdgeTypeDataStore(context)
        dataStore.setValue(KEY, Int::class.javaObjectType, CaptionStyle.EDGE_TYPE_RAISED)

        assertThat(settingsSecureStore.getInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE))
            .isEqualTo(CaptionStyle.EDGE_TYPE_RAISED)
        assertThat(settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED))
            .isTrue()
    }

    @Test
    fun captionEdgeColorDataStore_getValue_returnsFromSettings() {
        val dataStore = CaptionEdgeColorDataStore(context)
        settingsSecureStore.setInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR, Color.RED)

        assertThat(dataStore.getValue(KEY, Int::class.javaObjectType)).isEqualTo(Color.RED)
    }

    @Test
    fun captionEdgeColorDataStore_setValue_updatesSettingsAndEnablesCaptions() {
        val dataStore = CaptionEdgeColorDataStore(context)
        dataStore.setValue(KEY, Int::class.javaObjectType, Color.BLUE)

        assertThat(settingsSecureStore.getInt(Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR))
            .isEqualTo(Color.BLUE)
        assertThat(settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED))
            .isTrue()
    }

    companion object {
        private const val KEY = "any_key"
    }
}
