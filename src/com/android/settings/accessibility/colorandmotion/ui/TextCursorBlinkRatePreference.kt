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

package com.android.settings.accessibility.colorandmotion.ui

import android.content.Context
import android.provider.Settings
import android.view.View
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.colorandmotion.data.TextCursorBlinkRateDataStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding

class TextCursorBlinkRatePreference(context: Context) :
    SliderPreferenceBinding, PreferenceAvailabilityProvider, IntRangeValuePreference {
    private val dataStore by lazy { TextCursorBlinkRateDataStore(context) }

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.accessibility_text_cursor_blink_rate_purpose

    override val title: Int
        get() = R.string.accessibility_text_cursor_blink_rate_title

    override val summary: Int
        get() = R.string.accessibility_text_cursor_blink_rate_summary

    override val keywords: Int
        get() = R.string.keywords_text_cursor_blink_rate

    override fun getMinValue(context: Context): Int {
        return 0
    }

    override fun getMaxValue(context: Context): Int {
        return storage(context).durationValues.size - 1
    }

    override fun getIncrementStep(context: Context) = 1

    override fun getUnitOfMeasurement() = "milliseconds"

    override fun createWidget(context: Context): SliderPreference {
        return TextCursorBlinkRateSliderPreference(context, null).apply {
            setExtraChangeListener { slider, value, fromUser ->
                handlerSliderChange(context, this, value)
            }
            setResetClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        if (view == null) return
                        storage(context).resetToDefault()
                    }
                }
            )
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as TextCursorBlinkRateSliderPreference).setSliderStateDescription(
            storage(preference.context).durationLabels[preference.value]
        )
    }

    private fun handlerSliderChange(
        context: Context,
        preference: TextCursorBlinkRateSliderPreference,
        value: Float,
    ) {
        preference.setSliderStateDescription(storage(context).durationLabels[value.toInt()])
    }

    override fun storage(context: Context) = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.Companion.ALLOW

    override fun getWritePermit(context: Context, value: Int?, callingPid: Int, callingUid: Int) =
        ReadWritePermit.Companion.ALLOW

    override val supportsWrite = true
    override val availabilityDescription =
        "The device must have text cursor blinking user setting."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        return Flags.textCursorBlinkingUserSetting()
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS
    }
}
