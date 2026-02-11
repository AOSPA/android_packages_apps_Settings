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

package com.android.settings.connecteddevice

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings.ConnectedDeviceDashboardActivity
import com.android.settingslib.widget.theme.flags.Flags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ConnectedDeviceDashboardScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val preferenceScreenCreator = ConnectedDeviceDashboardScreen()

    private val mockResources = mock<Resources>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ConnectedDeviceDashboardScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ConnectedDeviceDashboardActivity::class.java.getName())
    }

    @Test
    @EnableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_isExpressiveTheme() {
        assertThat(preferenceScreenCreator.getIcon(context))
            .isEqualTo(R.drawable.ic_homepage_connected_device)
    }

    @Test
    @DisableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_notExpressiveTheme() {
        assertThat(preferenceScreenCreator.getIcon(context))
            .isEqualTo(R.drawable.ic_devices_other_filled)
    }

    @Test
    fun isAvailable_flagIsTrue_returnTrue() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_top_level_connected_devices) } doReturn true
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(true)
    }

    @Test
    fun isAvailable_flagIsFalse_returnFalse() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_top_level_connected_devices) } doReturn false
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }
}
