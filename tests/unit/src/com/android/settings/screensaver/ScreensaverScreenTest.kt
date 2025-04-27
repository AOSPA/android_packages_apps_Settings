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

package com.android.settings.screensaver

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.flags.Flags
import com.android.settingslib.dream.DreamBackend
import com.android.settingslib.preference.CatalystScreenTestCase
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class ScreensaverScreenTest: CatalystScreenTestCase() {
    private val mockResources = mock<Resources>()
    private val context = object : ContextWrapper(appContext) {
        override fun getResources(): Resources = mockResources
    }

    private val dreamBackend: DreamBackend = mock(DreamBackend::class.java)

    override val preferenceScreenCreator = ScreensaverScreen(context).also {
        it.setDreamBackend(dreamBackend)
    }

    override val flagName: String
        get() = Flags.FLAG_CATALYST_SCREENSAVER

    @Before
    fun setUp() {
        preferenceScreenCreator.setAmbientModeSuppressionProvider(
            object : ScreensaverScreen.AmbientModeSuppressionProvider {
                override fun isSuppressedByBedtime(context: Context) = false
            })

        preferenceScreenCreator.setSummaryStringsProvider(
            object : ScreensaverScreen.SummaryStringsProvider {
                override fun dreamOff(context: Context) = SCREENSAVER_SUMMARY_OFF

                override fun dreamOn(
                    context: Context,
                    activeDreamName: CharSequence
                ) = SCREENSAVER_SUMMARY_ON

                override fun dreamOffBedtime(context: Context) = SCREENSAVER_SUMMARY_OFF_BEDTIME
            }
        )
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ScreensaverScreen.KEY)
    }

    @Test
    fun getSummary_dreamsNotDisabledByAmbientModeSuppression_dreamsDisabled() {
        mockResources.stub {
            on {
                getBoolean(
                    com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig
                )
            } doReturn false
        }

        dreamBackend.stub {
            on { isEnabled } doReturn false
        }

        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(SCREENSAVER_SUMMARY_OFF)
    }

    @Test
    fun getSummary_dreamsNotDisabledByAmbientModeSuppression_dreamsEnabled() {
        mockResources.stub {
            on {
                getBoolean(
                    com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig
                )
            } doReturn false
        }

        dreamBackend.stub {
            on { isEnabled } doReturn true
            on { activeDreamName } doReturn ACTIVE_DREAM_NAME
        }

        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(SCREENSAVER_SUMMARY_ON)
    }

    @Test
    fun getSummary_dreamsDisabledByAmbientModeSuppression() {
        mockResources.stub {
            on {
                getBoolean(
                    com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig
                )
            } doReturn true
        }

        preferenceScreenCreator.setAmbientModeSuppressionProvider(
            object : ScreensaverScreen.AmbientModeSuppressionProvider {
                override fun isSuppressedByBedtime(context: Context) = true
            })

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(SCREENSAVER_SUMMARY_OFF_BEDTIME)
    }

    override fun migration() {}

    private companion object {
        const val SCREENSAVER_SUMMARY_OFF = "screensaver_summary_dream_off"
        const val SCREENSAVER_SUMMARY_ON = "screensaver_summary_on"
        const val SCREENSAVER_SUMMARY_OFF_BEDTIME = "screensaver_summary_off_bedtime"
        const val ACTIVE_DREAM_NAME = "dream"
    }
}
