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

import android.app.Application
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.flags.Flags
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.ipc.MessengerServiceRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionWebContentFiltersBrowserSupportedAppsScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val browserSupportedAppsScreen =
        SupervisionWebContentFiltersBrowserSupportedAppsScreen()
    private val packageName: String = "com.android.supervision"
    private val mockRoleManager: RoleManager = mock {
        on { getRoleHolders(ROLE_SYSTEM_SUPERVISION) }.thenReturn(listOf(packageName))
    }

    @get:Rule(order = 0) val setFlagsRule = SetFlagsRule()
    @get:Rule(order = 1)
    val serviceRule =
        MessengerServiceRule<MessengerServiceClient>(TestSupervisionMessengerService::class.java)

    @Before
    fun setUp() {
        (Shadow.extract((context as Application).baseContext) as ShadowContextImpl).apply {
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
        }
    }

    @Test
    fun key() {
        assertThat(browserSupportedAppsScreen.key)
            .isEqualTo(SupervisionWebContentFiltersBrowserSupportedAppsScreen.KEY)
    }

    @Test
    fun screenTitle() {
        assertThat(browserSupportedAppsScreen.screenTitle)
            .isEqualTo(R.string.supervision_web_content_filters_browser_filter_title)
    }

    @Test
    fun isIndexable() {
        assertThat(browserSupportedAppsScreen.indexable).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagEnabled() {
        assertThat(browserSupportedAppsScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun flagDisabled() {
        assertThat(browserSupportedAppsScreen.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun getMetricsCategory() {
        assertThat(browserSupportedAppsScreen.getMetricsCategory())
            .isEqualTo(SettingsEnums.SUPERVISION_WEB_CONTENT_FILTERS_BROWSER_SUPPORTED_APPS)
    }
}
