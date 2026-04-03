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
import android.view.accessibility.CaptioningManager
import androidx.core.content.getSystemService
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.CaptionHelper
import com.android.settings.accessibility.EdgeTypePreference as EdgeTypeWidget
import com.android.settings.accessibility.captionpreferences.data.CaptionEdgeColorDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionEdgeTypeDataStore
import com.android.settings.accessibility.captionpreferences.data.CaptionFontFamilyDataStore
import com.android.settings.accessibility.shared.utils.SummaryMap
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.DiscreteStringValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

/** Mixin for preferences only available when custom caption style is selected. */
interface CustomCaptionPreference : PreferenceMetadata, PreferenceAvailabilityProvider {

    override fun isAvailable(context: Context): Boolean =
        context.getSystemService<CaptioningManager>()?.rawUserStyle ==
            CaptioningManager.CaptionStyle.PRESET_CUSTOM

    override fun dependencies(context: Context): Array<String> =
        arrayOf(CustomCaptionOptionsPreference.KEY)
}

// LINT.IfChange(caption_font_family_pref)
class CaptionFontFamilyPreference(context: Context) :
    PersistentPreference<String>,
    PreferenceBinding,
    DiscreteStringValue,
    PreferenceSummaryProvider,
    CustomCaptionPreference {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_typeface

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_font_family_purpose

    override val values: Int
        get() = R.array.captioning_typeface_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_typeface_selector_titles

    override val valueType: Class<String>
        get() = String::class.javaObjectType

    private val dataStore by lazy { CaptionFontFamilyDataStore(context) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun storage(context: Context): KeyValueStore = dataStore

    private val summaryMap by lazy {
        SummaryMap(values, valuesDescription, useIntValues = false) { _, v -> v as String }
    }

    override fun createWidget(context: Context) = ListPreference(context)

    override fun getSummary(context: Context): CharSequence? =
        summaryMap.getSummary(context, storage(context).getString(key))

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    companion object {
        private const val KEY = "captioning_typeface"
    }
}

// LINT.ThenChange()

// LINT.IfChange(caption_edge_type_pref)
class CaptionEdgeTypePreference(context: Context) :
    PersistentPreference<Int>,
    PreferenceBinding,
    DiscreteIntValue,
    CustomCaptionPreference,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_edge_type

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_edge_type_purpose

    override val values: Int
        get() = R.array.captioning_edge_type_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_edge_type_selector_titles

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    private val summaryMap by lazy {
        SummaryMap(values, valuesDescription, useIntValues = true) { _, v -> v as Int }
    }

    private val dataStore by lazy { CaptionEdgeTypeDataStore(context) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun createWidget(context: Context): Preference = EdgeTypeWidget(context, null)

    override fun getSummary(context: Context): CharSequence? =
        summaryMap.getSummary(context, storage(context).getInt(key))

    companion object {
        const val KEY = "captioning_edge_type"
    }
}

// LINT.ThenChange()

// LINT.IfChange(caption_edge_color_pref)
class CaptionEdgeColorPreference(context: Context) :
    PersistentPreference<Int>, PreferenceBinding, DiscreteIntValue, CustomCaptionPreference {
    override val key: String
        get() = KEY

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override val title: Int
        get() = R.string.captioning_edge_color

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_custom_edge_color_purpose

    override val values: Int
        get() = R.array.captioning_color_selector_values

    override val valuesDescription: Int
        get() = R.array.captioning_color_selector_titles

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    private val dataStore by lazy { CaptionEdgeColorDataStore(context) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    private val captionHelper by lazy { CaptionHelper(context) }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun createWidget(context: Context): Preference =
        createColorWidget(context, values, valuesDescription)

    override fun getEnabledDescription(): String = "A caption edge type other than 'None' must be selected."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context): Boolean =
        captionHelper.edgeType != CaptioningManager.CaptionStyle.EDGE_TYPE_NONE

    override fun dependencies(context: Context): Array<String> =
        super<CustomCaptionPreference>.dependencies(context) + CaptionEdgeTypePreference.KEY

    companion object {
        const val KEY = "captioning_edge_color"
    }
}
// LINT.ThenChange()
