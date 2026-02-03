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
import android.provider.Settings
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.widget.MainSwitchPreferenceBinding

/** A setting that allows the user to turn on/off the captioning. */
class CaptioningMainSwitchPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.caption_preferences_main_switch_purpose,
        title = R.string.accessibility_captioning_primary_switch_title,
    ),
    MainSwitchPreferenceBinding {

    override fun storage(context: Context): KeyValueStore = SettingsSecureStore.get(context)

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED
    }
}
