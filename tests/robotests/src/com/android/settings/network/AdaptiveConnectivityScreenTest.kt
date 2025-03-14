/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.preference.SwitchPreferenceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.network.AdaptiveConnectivitySettings.ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED
import com.android.settings.network.AdaptiveConnectivitySettings.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class AdaptiveConnectivityScreenTest : CatalystScreenTestCase() {
    override val preferenceScreenCreator = AdaptiveConnectivityScreen()
    override val flagName
        get() = Flags.FLAG_CATALYST_ADAPTIVE_CONNECTIVITY

    override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(AdaptiveConnectivityScreen.KEY)
    }

    @Test
    fun flagDefaultDisabled_noSwitchPreferenceCompatExists() {
        // create fragment
        val fragment: AdaptiveConnectivitySettings =
            preferenceScreenCreator.fragmentClass().newInstance()
        // check if switch preference exists
        assertSwitchPreferenceCompatIsNull(ADAPTIVE_CONNECTIVITY_WIFI_ENABLED, fragment)
        assertSwitchPreferenceCompatIsNull(ADAPTIVE_CONNECTIVITY_MOBILE_NETWORK_ENABLED, fragment)
    }

    private fun assertSwitchPreferenceCompatIsNull(
        key: String,
        fragment: AdaptiveConnectivitySettings
    ) {
        val switchPreference = fragment.findPreference<SwitchPreferenceCompat>(key)
        assertThat(switchPreference).isNull()
    }

}
