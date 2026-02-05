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

package com.android.settings.vpn2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnSettingsScreenTest {
    private val preferenceScreenCreator = VpnSettingsScreen()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(VpnSettingsScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(Settings.VpnSettingsActivity::class.java.getName())
    }

    @Test
    fun restrictionKeys_containsDisallowConfigVpn() {
        assertThat(preferenceScreenCreator.restrictionKeys)
            .asList()
            .contains(android.os.UserManager.DISALLOW_CONFIG_VPN)
    }
}
