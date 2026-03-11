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

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.PresetPreference
import com.android.settings.accessibility.captionpreferences.data.CaptionFontSizeDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionStyleDataStore
import com.android.settings.accessibility.shared.utils.SummaryMap
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.DiscreteStringValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

/** Preference for caption font size. */
// LINT.IfChange(caption_font_size_pref)
class CaptionFontSizePreference(context: Context) :
    PersistentPreference<String>,
    PreferenceBinding,
    DiscreteStringValue,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_text_size

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_caption_size_purpose

    override val values: Int
        get() = R.array.captioning_font_size_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_font_size_selector_titles

    override val valueType: Class<String>
        get() = String::class.javaObjectType

    private val summaryMap by lazy {
        SummaryMap(values, valuesDescription, useIntValues = false) { _, v -> v as String }
    }

    private val dataStore by lazy { CaptionFontSizeDataStore(context) }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun createWidget(context: Context) = ListPreference(context)

    override fun getSummary(context: Context): CharSequence? =
        summaryMap.getSummary(context, storage(context).getString(key))

    companion object {
        const val KEY = "captioning_font_size"
    }
}

// LINT.ThenChange()

/** Preference for caption style. */
// LINT.IfChange(caption_style_pref)
class CaptionStylePreference(context: Context) :
    PersistentPreference<Int>, DiscreteIntValue, PreferenceBinding, PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_preset

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_caption_style_purpose

    override val values: Int
        get() = R.array.captioning_preset_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_preset_selector_titles

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    private val summaryMap by lazy { SummaryMap(values, valuesDescription) { _, v -> v as Int } }

    private val dataStore by lazy { CaptionStyleDataStore(context) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun createWidget(context: Context): Preference = PresetPreference(context, null)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as PresetPreference).apply {
            setTitles(context.resources.getStringArray(valuesDescription))
            setValues(context.resources.getIntArray(values))
            // MUST suppress persistent when initializing the checked state:
            //   1. default value is written to datastore if not set (b/396260949)
            //   2. avoid redundant read to the datastore
            val suppressPersistent = isPersistent
            if (suppressPersistent) isPersistent = false
            value = preferenceDataStore!!.getInt(key, CaptionStyleDataStore.DEFAULT_STYLE)
            if (suppressPersistent) isPersistent = true
        }
    }

    override fun getSummary(context: Context): CharSequence? =
        summaryMap.getSummary(context, storage(context).getInt(key))

    companion object {
        const val KEY = "captioning_preset"
    }
}
// LINT.ThenChange()
