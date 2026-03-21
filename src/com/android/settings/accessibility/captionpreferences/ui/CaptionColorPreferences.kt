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

package com.android.settings.accessibility.captionpreferences.ui

import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.captionpreferences.data.CaptionBackgroundColorDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionTextColorDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionWindowColorDataStore
import com.android.settings.accessibility.captionpreferences.data.ColorOpacityDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

/** Base class for caption color-related preferences. */
abstract class BaseCaptionColorPreference :
    PersistentPreference<Int>, DiscreteIntValue, PreferenceBinding, CustomCaptionPreference {
    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override fun createWidget(context: Context): Preference =
        createColorWidget(context, values, valuesDescription)

    protected abstract val dataStore: ColorOpacityDataStore

    override fun storage(context: Context): KeyValueStore = dataStore
}

/** Base class for caption opacity-related preferences. */
abstract class BaseCaptionOpacityPreference(private val dependentColorPrefKey: String) :
    BaseCaptionColorPreference() {
    override val values: Int
        get() = R.array.captioning_opacity_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_opacity_selector_titles

    override fun getEnabledDescription(): String = "A valid caption color must be selected."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context): Boolean = dataStore.hasValidColor()

    override fun dependencies(context: Context): Array<String> =
        super.dependencies(context) + dependentColorPrefKey
}

// Text Color & Opacity
// LINT.IfChange(caption_text_color_pref)
class CaptionTextColorPreference(context: Context) : BaseCaptionColorPreference() {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_text_color_purpose

    override val title: Int
        get() = R.string.captioning_foreground_color

    override val values: Int
        get() = R.array.captioning_color_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_color_selector_titles

    override val dataStore by lazy {
        CaptionTextColorDataStore(context.applicationContext, isColor = true)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_foreground_color"
    }
}
// LINT.ThenChange()

// LINT.IfChange(caption_text_opacity_pref)
class CaptionTextOpacityPreference(context: Context) :
    BaseCaptionOpacityPreference(CaptionTextColorPreference.KEY) {
    override val key: String
        get() = KEY

       override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_text_opacity_purpose

    override val title: Int
        get() = R.string.captioning_foreground_opacity

    override val dataStore by lazy {
        CaptionTextColorDataStore(context.applicationContext, isColor = false)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_foreground_opacity"
    }
}
// LINT.ThenChange()

// Background Color & Opacity
// LINT.IfChange(caption_background_color_pref)
class CaptionBackgroundColorPreference(context: Context) : BaseCaptionColorPreference() {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_background_color_purpose

    override val title: Int
        get() = R.string.captioning_background_color

    override val values: Int
        get() = R.array.captioning_color_selector_including_transparent_values

    override val valuesDescription: Int
        get() = R.array.captioning_color_selector_including_transparent_titles

    override val dataStore: ColorOpacityDataStore by lazy {
        CaptionBackgroundColorDataStore(context, isColor = true)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_background_color"
    }
}
// LINT.ThenChange()

// LINT.IfChange(caption_background_opacity_pref)
class CaptionBackgroundOpacityPreference(context: Context) :
    BaseCaptionOpacityPreference(CaptionBackgroundColorPreference.KEY) {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_background_opacity_purpose

    override val title: Int
        get() = R.string.captioning_background_opacity

    override val dataStore: ColorOpacityDataStore by lazy {
        CaptionBackgroundColorDataStore(context, isColor = false)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_background_opacity"
    }
}
// LINT.ThenChange()

// Window Color & Opacity
// LINT.IfChange(caption_window_color_pref)
class CaptionWindowColorPreference(context: Context) : BaseCaptionColorPreference() {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_window_color_purpose

    override val title: Int
        get() = R.string.captioning_window_color

    override val values: Int
        get() = R.array.captioning_color_selector_including_transparent_values

    override val valuesDescription: Int =
        R.array.captioning_color_selector_including_transparent_titles

    override val dataStore: ColorOpacityDataStore by lazy {
        CaptionWindowColorDataStore(context, isColor = true)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_window_color"
    }
}
// LINT.ThenChange()

// LINT.IfChange(caption_window_opacity_pref)
class CaptionWindowOpacityPreference(context: Context) :
    BaseCaptionOpacityPreference(CaptionWindowColorPreference.KEY) {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_window_opacity_purpose

    override val title: Int
        get() = R.string.captioning_window_opacity

    override val dataStore: ColorOpacityDataStore by lazy {
        CaptionWindowColorDataStore(context, isColor = false)
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = "captioning_window_opacity"
    }
}
// LINT.ThenChange()
