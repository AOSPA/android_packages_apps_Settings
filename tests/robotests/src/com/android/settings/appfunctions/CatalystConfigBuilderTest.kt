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

package com.android.settings.appfunctions

import com.android.settings.testutils.appfunctions.CatalystConfigBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalystConfigBuilderTest {

    @Test
    fun buildConfig_createsCorrectConfig() {
        val screenKey = "test_screen"
        val pref1 = "pref1"
        val pref2 = "pref2"
        val additionalDescription = "test description"

        val config = CatalystConfigBuilder.buildConfig(
            screenKey,
            listOf(pref1, pref2),
            additionalDescription
        )

        assertThat(config.screenConfigs).hasSize(1)
        assertThat(config.screenConfigs[0].screenKey).isEqualTo(screenKey)
        assertThat(config.screenConfigs[0].enabled).isTrue()
        assertThat(config.screenConfigs[0].additionalDescription).isEqualTo(additionalDescription)

        assertThat(config.deviceStateItems).hasSize(2)
        assertThat(config.deviceStateItems[0].settingKey).isEqualTo(pref1)
        assertThat(config.deviceStateItems[0].settingScreenKey).isEqualTo(screenKey)
        assertThat(config.deviceStateItems[1].settingKey).isEqualTo(pref2)
        assertThat(config.deviceStateItems[1].settingScreenKey).isEqualTo(screenKey)
    }

    @Test
    fun buildConfig_noAdditionalDescription_createsCorrectConfig() {
        val screenKey = "test_screen"
        val pref1 = "pref1"

        val config = CatalystConfigBuilder.buildConfig(
            screenKey,
            listOf(pref1)
        )

        assertThat(config.screenConfigs[0].additionalDescription).isNull()
    }
}
