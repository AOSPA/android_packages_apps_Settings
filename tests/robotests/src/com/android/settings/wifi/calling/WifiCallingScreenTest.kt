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
package com.android.settings.wifi.calling

import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settings.utils.putSubId
import com.android.settings.wifi.calling.WifiCallingSettingsForSub.EXTRA_SUB_ID
import com.android.settingslib.catalyst.flags.Flags as CatalystFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class WifiCallingScreenTest : SettingsCatalystTestCase() {

    @get:Rule val platformFlags = SetFlagsRule()

    private val testSubId = 2737

    override val preferenceScreenCreator =
        createScreen(Bundle().apply { putSubId(EXTRA_SUB_ID, testSubId) })

    override val flagName: String
        get() = Flags.FLAG_CATALYST_WIFI_CALLING

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_whenFlagIsTrue_isParsedFromString() {
        val bundle = Bundle().apply { putSubId(EXTRA_SUB_ID, testSubId) }
        val screen = createScreen(bundle)

        assertThat(screen.isAvailable(appContext)).isTrue()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE,
        CatalystFlags.FLAG_CATALYST_USE_KEY_PARAMETERS,
    )
    fun subId_whenFlagIsFalse_andSubIdIsString_getsTheSubIdFromString() {
        val bundle = Bundle().apply { putString(EXTRA_SUB_ID, testSubId.toString()) }
        val screen = createScreen(bundle)

        assertThat(screen.isAvailable(appContext)).isTrue()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE,
        CatalystFlags.FLAG_CATALYST_USE_KEY_PARAMETERS,
    )
    fun subId_whenFlagIsFalse_isParsedFromInt() {
        val bundle = Bundle().apply { putInt(EXTRA_SUB_ID, testSubId) }
        val screen = createScreen(bundle)

        assertThat(screen.isAvailable(appContext)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun subId_whenFlagIsTrue_andSubIdIsInt_getsTheSubIdFromInt() {
        val bundle = Bundle().apply { putSubId(EXTRA_SUB_ID, testSubId) }
        val screen = createScreen(bundle)

        assertThat(screen.isAvailable(appContext)).isTrue()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_CATALYST_USE_STRING_BUNDLE,
        CatalystFlags.FLAG_CATALYST_USE_KEY_PARAMETERS,
    )
    fun subId_whenKeyIsMissing_fallsBackToDefault() {
        val bundle = Bundle() // No subId in bundle
        val screen = createScreen(bundle)

        assertThat(screen.isAvailable(appContext)).isFalse()
    }

    private fun createScreen(args: Bundle): WifiCallingScreen {
        return if (CatalystFlags.catalystUseKeyParameters()) {
            WifiCallingScreen(WifiCallingScreen.parametersSchema.prepare(args))
        } else {
            WifiCallingScreen(args)
        }
    }

    @Test override fun migration() {}
}
