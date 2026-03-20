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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.shared.ui.SelectorWithImagePreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.BooleanValuePreferenceBinding

sealed class MagnificationModeSelectorPreference(private val store: MagnificationModeStorage) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    SelectorWithImagePreference.OnClickListener {

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as SelectorWithImagePreference).setOnClickListener(this)
    }

    override fun onRadioButtonClicked(emiter: SelectorWithImagePreference) {
        emiter.isChecked = true
    }

    override fun storage(context: Context): KeyValueStore = store

    override fun getReadPermissions(context: Context): Permissions? =
        MagnificationModeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context): Permissions? =
        MagnificationModeStorage.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true
    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY
}

/** The "Magnify full screen" preference. */
class FullScreenModeSelectorPreference(store: MagnificationModeStorage) :
    MagnificationModeSelectorPreference(store) {
    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.full_screen_mode_preference_purpose

    override val title
        get() = R.string.accessibility_magnification_mode_dialog_option_full_screen

    override fun createWidget(context: Context) =
        SelectorWithImagePreference(context, R.drawable.accessibility_magnification_mode_fullscreen)

    companion object {
        const val KEY = "full_screen_mode_preference"
    }
}

/** The "Magnify part of screen" preference. */
class WindowModeSelectorPreference(store: MagnificationModeStorage) :
    MagnificationModeSelectorPreference(store) {
    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.window_mode_preference_purpose

    override val title
        get() = R.string.accessibility_magnification_mode_dialog_option_window

    override fun createWidget(context: Context) =
        SelectorWithImagePreference(context, R.drawable.accessibility_magnification_mode_window)

    companion object {
        const val KEY = "window_mode_preference"
    }
}

/** The "Switch between full and partial screen" preference. */
class AllModeSelectorPreference(store: MagnificationModeStorage) :
    MagnificationModeSelectorPreference(store) {
    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.all_mode_preference_purpose

    override val title
        get() = R.string.accessibility_magnification_mode_dialog_option_switch

    override val summary
        get() = R.string.accessibility_magnification_area_settings_mode_switch_summary

    override fun createWidget(context: Context) =
        SelectorWithImagePreference(context, R.drawable.accessibility_magnification_mode_switch)

    companion object {
        const val KEY = "all_mode_preference"
    }
}
