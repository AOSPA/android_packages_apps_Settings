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

import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.IntInRange


@ProvidePreferenceScreen(TextToSpeechApiScreen.KEY)
class TextToSpeechApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ACCESSIBILITY,
        fragment = TextToSpeechSettings::class,
        purpose = R.string.text_to_speech_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = "tts_default_rate",
            purpose = R.string.text_to_speech_rate_purpose,
            type = IntInRange(min = 10, max = 600),
        ) {
            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.TTS_DEFAULT_RATE,
                        TextToSpeech.Engine.DEFAULT_RATE)
                }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.TTS_DEFAULT_RATE,
                        value)
                }
            }
        }

        preference(
            key = "tts_default_pitch",
            purpose = R.string.text_to_speech_pitch_purpose,
            type = IntInRange(min = 25, max = 400),
        ) {
            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.TTS_DEFAULT_PITCH,
                        TextToSpeech.Engine.DEFAULT_PITCH)
                }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.TTS_DEFAULT_PITCH,
                        value)
                }
            }
        }
    }

    companion object {
        const val KEY = "text_to_speech_settings"
    }
}
