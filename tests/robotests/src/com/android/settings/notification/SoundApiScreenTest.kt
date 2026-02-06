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

package com.android.settings.notification

import android.content.Context
import android.os.Vibrator
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Secure
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

@RunWith(AndroidJUnit4::class)
class SoundApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(SoundApiScreen())

    @Before
    fun setUp() {
        Secure.putInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, 0)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @Config(shadows = [ShadowVibrator::class])
    fun get_vibrateIcon_whenHardwareSupported_returnsValueFromSettings() {
        // Hardware supports vibration
        val vibrator = context.getSystemService(Vibrator::class.java)
        shadowOf(vibrator).setHasVibrator(true)

        Secure.putInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, 1)
        assertThat(tester.get<Boolean>("vibrate_icon")).isTrue()

        Secure.putInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, 0)
        assertThat(tester.get<Boolean>("vibrate_icon")).isFalse()
    }

    @Test
    @Config(shadows = [ShadowVibrator::class])
    fun set_vibrateIcon_whenHardwareSupported_updatesSettings() {
        //Hardware supports vibration
        val vibrator = context.getSystemService(Vibrator::class.java)
        shadowOf(vibrator).setHasVibrator(true)

        tester.set("vibrate_icon", true)
        assertThat(Secure.getInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON))
            .isEqualTo(1)

        tester.set("vibrate_icon", false)
        assertThat(Secure.getInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON))
            .isEqualTo(0)
    }

    @Test(expected = HardwareUnsupportedException::class)
    @Config(shadows = [ShadowVibrator::class])
    fun get_vibrateIcon_whenHardwareNotSupported_throwsHardwareUnsupportedException() {
        // Hardware does NOT support vibration
        val vibrator = context.getSystemService(Vibrator::class.java)
        shadowOf(vibrator).setHasVibrator(false)

        tester.get<Boolean>("vibrate_icon")
    }

    @Test(expected = HardwareUnsupportedException::class)
    @Config(shadows = [ShadowVibrator::class])
    fun set_vibrateIcon_whenHardwareNotSupported_throwsHardwareUnsupportedException() {
        // Hardware does NOT support vibration
        val vibrator = context.getSystemService(Vibrator::class.java)
        shadowOf(vibrator).setHasVibrator(false)

        tester.set("vibrate_icon", true)
    }
}