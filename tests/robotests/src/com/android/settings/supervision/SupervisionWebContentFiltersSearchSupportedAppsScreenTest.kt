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
package com.android.settings.supervision

import android.app.settings.SettingsEnums
import android.app.supervision.flags.Flags
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionWebContentFiltersSearchSupportedAppsScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val searchSupportedAppsScreen = SupervisionWebContentFiltersSearchSupportedAppsScreen()

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()

    @Test
    fun key() {
        assertThat(searchSupportedAppsScreen.key)
            .isEqualTo(SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY)
    }

    @Test
    fun screenTitle() {
        assertThat(searchSupportedAppsScreen.screenTitle)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_title)
    }

    @Test
    fun isIndexable() {
        assertThat(searchSupportedAppsScreen.indexable).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled() {
        assertThat(searchSupportedAppsScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled() {
        assertThat(searchSupportedAppsScreen.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun getMetricsCategory() {
        assertThat(searchSupportedAppsScreen.getMetricsCategory())
            .isEqualTo(SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS_SEARCH_SUPPORTED_APPS)
    }
}
