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

package com.android.settings.accessibility.actiontimeout.ui

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.widget.SelectorWithWidgetPreference

/**
 * A preference class that represents an option for the action timeout setting.
 *
 * This preference uses a [SelectorWithWidgetPreference] for its UI and stores its boolean value in
 * a [KeyValueStore].
 */
class ActionTimeoutOptionPreference(
    override val key: String,
    override val title: Int,
    private val dataStoreProvider: () -> KeyValueStore,
) : BooleanValuePreference, BooleanValuePreferenceBinding {
    override val purpose: Int
        get() = R.string.a11y_action_timeout_option_purpose

    private val dataStore by lazy { dataStoreProvider.invoke() }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override val sensitivityLevel: Int
        get() = SensitivityLevel.DEEP_LINK_ONLY

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun createWidget(context: Context): Preference {
        return SelectorWithWidgetPreference(context, null).apply {
            setOnClickListener { emitter -> emitter.isChecked = true }
        }
    }
}
