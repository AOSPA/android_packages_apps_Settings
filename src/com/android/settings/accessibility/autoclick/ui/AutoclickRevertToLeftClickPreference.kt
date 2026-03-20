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

package com.android.settings.accessibility.autoclick.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.SwitchPreferenceBinding

class AutoclickRevertToLeftClickPreference : BooleanValuePreference, SwitchPreferenceBinding {
    override val key: String
        get() = SETTING_KEY

    override val purpose: Int
        get() = R.string.a11y_autoclick_revert_to_left_click_purpose

    override val title: Int
        get() = R.string.autoclick_revert_to_left_click_title

    override val summary: Int
        get() = R.string.autoclick_revert_to_left_click_summary

    override fun storage(context: Context): KeyValueStore =
        SettingsSecureStore.get(context).apply {
            setDefaultValue(
                SETTING_KEY,
                AccessibilityManager.AUTOCLICK_REVERT_TO_LEFT_CLICK_DEFAULT,
            )
        }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_AUTOCLICK_REVERT_TO_LEFT_CLICK
    }
}
