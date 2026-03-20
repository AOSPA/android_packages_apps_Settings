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
package com.android.settings.network

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.network.AdaptiveConnectivityApiScreen.Companion.PREF_KEY_MOBILE
import com.android.settings.network.AdaptiveConnectivityApiScreen.Companion.PREF_KEY_WIFI
import com.android.settings.testutils2.ApiTester
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveConnectivityApiScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(AdaptiveConnectivityApiScreen())

    @Before
    fun setUp() {
        // Ensure clean state for secure settings before each test
        Settings.Secure.putString(context.contentResolver, PREF_KEY_WIFI, null)
        Settings.Secure.putString(context.contentResolver, PREF_KEY_MOBILE, null)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagsEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_migrationFlagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getWifiEnabled_notSet_returnsTrueDefault() {
        // Default is 1 (True)
        assertThat(tester.get<Boolean>(PREF_KEY_WIFI)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getWifiEnabled_setOff_returnsFalse() {
        Settings.Secure.putInt(context.contentResolver, PREF_KEY_WIFI, 0)
        assertThat(tester.get<Boolean>(PREF_KEY_WIFI)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setWifiEnabled_booleanToIntegerConversionIsCorrect() {
        tester.set(PREF_KEY_WIFI, true)
        assertThat(Settings.Secure.getInt(context.contentResolver, PREF_KEY_WIFI, -1)).isEqualTo(1)
        tester.set(PREF_KEY_WIFI, false)
        assertThat(Settings.Secure.getInt(context.contentResolver, PREF_KEY_WIFI, -1)).isEqualTo(0)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getMobileEnabled_notSet_returnsTrueDefault() {
        // Default is 1 (True)
        assertThat(tester.get<Boolean>(PREF_KEY_MOBILE)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getMobileEnabled_setOff_returnsFalse() {
        Settings.Secure.putInt(context.contentResolver, PREF_KEY_MOBILE, 0)
        assertThat(tester.get<Boolean>(PREF_KEY_MOBILE)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun setMobileEnabled_booleanToIntegerConversionIsCorrect() {
        tester.set(PREF_KEY_MOBILE, true)
        assertThat(Settings.Secure.getInt(context.contentResolver, PREF_KEY_MOBILE, -1))
            .isEqualTo(1)
        tester.set(PREF_KEY_MOBILE, false)
        assertThat(Settings.Secure.getInt(context.contentResolver, PREF_KEY_MOBILE, -1))
            .isEqualTo(0)
    }
}
