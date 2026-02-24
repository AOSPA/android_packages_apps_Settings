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

package com.android.settings.display

import android.app.UiModeManager
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings.Secure.CONTRAST_LEVEL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorContrastApiScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(ColorContrastApiScreen())
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getContrastLevel() {
        listOf(
                UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM,
                UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH,
                UiModeManager.ContrastUtils.CONTRAST_LEVEL_STANDARD,
            )
            .forEach { testContrast ->
                SettingsSecureStore.get(context)
                    .setFloat(
                        CONTRAST_LEVEL,
                        UiModeManager.ContrastUtils.fromContrastLevel(testContrast),
                    )
                assertThat(
                        tester.get<ContrastLevelApiWithRes>("color_contrast_selector")
                    )
                    .isEqualTo(testContrast)
            }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setContrastLevel() {
        listOf(
                ContrastLevelApiWithRes.MEDIUM,
                ContrastLevelApiWithRes.HIGH,
                ContrastLevelApiWithRes.DEFAULT,
            )
            .forEach { testContrast ->
                tester.set("color_contrast_selector", testContrast.asApiValue)
                val contrastFromSettings = SettingsSecureStore.get(context).getFloat(CONTRAST_LEVEL)
                assertThat(contrastFromSettings).isNotNull()
                assertThat(UiModeManager.ContrastUtils.toContrastLevel(contrastFromSettings!!))
                    .isEqualTo(testContrast.asApiValue)
            }
    }
}
