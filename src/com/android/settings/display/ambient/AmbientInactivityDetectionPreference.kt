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
package com.android.settings.display.ambient

import android.content.Context
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import android.provider.Settings.Secure.DOZE_ALWAYS_ON_INACTIVITY_DETECTION
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.SwitchPreference

class AmbientInactivityDetectionPreference(context: Context) :
    SwitchPreference(
        KEY,
        R.string.doze_always_on_inactivity_detection_title,
        R.string.doze_always_on_inactivity_detection_summary,
    ) {

    private val dozeAlwaysOnDataStore = AmbientDisplayStorage(context)

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun isEnabled(context: Context) = dozeAlwaysOnDataStore.getBoolean(DOZE_ALWAYS_ON)!!

    override fun storage(context: Context) = context.dataStore

    companion object {
        const val KEY = DOZE_ALWAYS_ON_INACTIVITY_DETECTION

        private val Context.dataStore: KeyValueStore
            get() = SettingsSecureStore.get(this).apply { setDefaultValue(KEY, false) }
    }
}
