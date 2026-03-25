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

package com.android.settings.accessibility.colorcorrection.ui

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.InvalidPreferenceException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ColorCorrectionApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(ColorCorrectionApiFirstScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        setMainSwitch(ON)
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
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getRadioPreference_deuteranomalyMode_returnDeuteranomaly() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
            DEUTERANOMALY_VALUE,
        )

        assertThat(tester.get<DaltonizerMode>(ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DaltonizerMode.DEUTERANOMALY.asApiValue)
    }

    @Test
    fun getRadioPreference_protanomalyMode_returnProtanomaly() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
            PROTANOMALY_VALUE,
        )

        assertThat(tester.get<DaltonizerMode>(ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DaltonizerMode.PROTANOMALY.asApiValue)
    }

    @Test
    fun getRadioPreference_tritanomalyMode_returnTritanomaly() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
            TRITANOMALY_VALUE,
        )

        assertThat(tester.get<DaltonizerMode>(ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DaltonizerMode.TRITANOMALY.asApiValue)
    }

    @Test
    fun getRadioPreference_grayscaleMode_returnGrayscale() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
            GRAYSCALE_VALUE,
        )

        assertThat(tester.get<DaltonizerMode>(ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DaltonizerMode.GRAYSCALE.asApiValue)
    }

    @Test
    fun setRadioPreference_asDeuteranomaly_returnDeuteranomalyValue() {
        tester.set(
            ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY,
            DaltonizerMode.DEUTERANOMALY.asApiValue,
        )

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                )
            )
            .isEqualTo(DEUTERANOMALY_VALUE)
    }

    @Test
    fun setRadioPreference_asProtanomaly_returnProtanomalyValue() {
        tester.set(
            ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY,
            DaltonizerMode.PROTANOMALY.asApiValue,
        )

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                )
            )
            .isEqualTo(PROTANOMALY_VALUE)
    }

    @Test
    fun setRadioPreference_asTritanomaly_returnTritanomalyValue() {
        tester.set(
            ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY,
            DaltonizerMode.TRITANOMALY.asApiValue,
        )

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                )
            )
            .isEqualTo(TRITANOMALY_VALUE)
    }

    @Test
    fun setRadioPreference_asGrayscale_returnGrayscaleValue() {
        tester.set(
            ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY,
            DaltonizerMode.GRAYSCALE.asApiValue,
        )

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                )
            )
            .isEqualTo(GRAYSCALE_VALUE)
    }

    @Test
    fun getRadioPreference_disabled_throwsInvalidPreferenceException() {
        setMainSwitch(OFF)

        assertFailsWith<InvalidPreferenceException> {
            tester.get<DaltonizerMode>(ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY)
        }
    }

    @Test
    fun setRadioPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set(
                ColorCorrectionApiFirstScreen.RADIO_PREFERENCE_KEY,
                DaltonizerMode.DEUTERANOMALY.asApiValue,
            )
        }
    }

    private fun setMainSwitch(value: Int) =
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
            value,
        )

    companion object {
        private const val ON = 1
        private const val OFF = 0

        // Daltonizer type values: 12, 11, 13, 0 in the resource.
        private const val DEUTERANOMALY_VALUE = 12
        private const val PROTANOMALY_VALUE = 11
        private const val TRITANOMALY_VALUE = 13
        private const val GRAYSCALE_VALUE = 0
    }
}
