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

package com.android.settings.notification.modes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.notification.modes.ZenModeApiScreen.Companion.MODE_NAME
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModesBackend
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ZenModesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zenModesBackend = mock<ZenModesBackend>()

    @Before
    fun setUp() {
        ZenModesBackend.setInstance(zenModesBackend)
    }

    @Test
    fun getAllPossibleParameters_withZenModes_returnParameters() {
        val screen =
            object :
                PreferencesApiScreen(
                    key = "ApiScreen",
                    topLevelSettingsCategory = Category.PRIORITY_MODES,
                    fragment = PreferenceFragment::class,
                    purpose = R.string.preference_screen_purpose,
                ) {
                init {
                    parameters {
                        parameter(
                            name = MODE_NAME,
                            purpose = R.string.parameter_purpose,
                            required = true,
                            type = ZenModes,
                        )
                    }
                }
            }

        val mode1 = mock<ZenMode> { on { name } doReturn "Do Not Disturb" }
        val mode2 = mock<ZenMode> { on { name } doReturn "Bedtime" }
        zenModesBackend.stub { on { modes } doReturn listOf(mode1, mode2) }

        val allPossibleParameters = runBlocking {
            screen.getAllPossibleParameters(context).toList()
        }
        assertThat(allPossibleParameters).hasSize(2)

        val possibleParameterPairs =
            allPossibleParameters.flatMap { it.values.entries }.map { it.key to it.value }
        assertThat(possibleParameterPairs)
            .containsExactly(MODE_NAME to "Do Not Disturb", MODE_NAME to "Bedtime")
    }

    @Test
    fun getDescription_returnsCorrectString() {
        assertThat(ZenModes.getDescription(context))
            .isEqualTo(context.getString(R.string.zen_mode_type_description))
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(ZenModes.getKey()).isEqualTo("ZenModes")
    }
}
