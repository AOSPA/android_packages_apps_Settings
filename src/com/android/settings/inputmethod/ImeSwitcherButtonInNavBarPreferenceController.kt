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

package com.android.settings.inputmethod

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.Flags
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

/**
 * Preference controller that manages the setting for displaying the IME Switcher button in the
 * navigation bar.
 */
class ImeSwitcherButtonInNavBarPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int {
        return if (Flags.imeSwitcherButtonInNavbarSetting()) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            mContext.contentResolver,
            Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
            SETTING_VALUE_ON,
        ) == SETTING_VALUE_ON
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        return Settings.Secure.putInt(
            mContext.contentResolver,
            Settings.Secure.IME_SWITCHER_BUTTON_IN_NAVBAR_ENABLED,
            if (isChecked) SETTING_VALUE_ON else SETTING_VALUE_OFF,
        )
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_system
    }

    companion object {
        @VisibleForTesting const val SETTING_VALUE_OFF = 0

        @VisibleForTesting const val SETTING_VALUE_ON = 1
    }
}
