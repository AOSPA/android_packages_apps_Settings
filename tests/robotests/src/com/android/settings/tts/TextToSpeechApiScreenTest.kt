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

package com.android.settings.tts

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextToSpeechApiScreenTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val tester = ApiTester(TextToSpeechApiScreen(), context)

    @get:Rule
    val setFlagsRule = SetFlagsRule()

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun speechRate_get_returnsDefaultValue() {
        val defaultValue = TextToSpeech.Engine.DEFAULT_RATE
        Settings.Secure.putInt(context.contentResolver, Settings.Secure.TTS_DEFAULT_RATE, defaultValue)
        assertThat(tester.get<Int>("tts_default_rate")).isEqualTo(defaultValue)
    }

    @Test
    fun speechRate_set_updatesValue() {
        val newValue = 200
        tester.set("tts_default_rate", newValue)
        val updatedValue = Settings.Secure.getInt(context.contentResolver, Settings.Secure.TTS_DEFAULT_RATE, 0)
        assertThat(updatedValue).isEqualTo(newValue)
    }

    @Test
    fun pitch_get_returnsDefaultValue() {
        val defaultValue = TextToSpeech.Engine.DEFAULT_PITCH
        Settings.Secure.putInt(context.contentResolver, Settings.Secure.TTS_DEFAULT_PITCH, defaultValue)
        assertThat(tester.get<Int>("tts_default_pitch")).isEqualTo(defaultValue)
    }

    @Test
    fun pitch_set_updatesValue() {
        val newValue = 200
        tester.set("tts_default_pitch", newValue)
        val updatedValue = Settings.Secure.getInt(context.contentResolver, Settings.Secure.TTS_DEFAULT_PITCH, 0)
        assertThat(updatedValue).isEqualTo(newValue)
    }
}