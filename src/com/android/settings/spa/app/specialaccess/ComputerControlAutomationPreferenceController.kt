/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.spa.app.specialaccess

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity

/**
 * Preference controller for the computer control automation page.
 *
 * This controller is responsible for navigating to the computer control automation page.
 */
class ComputerControlAutomationPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {
    override fun getAvailabilityStatus(): Int {
        return if (android.companion.virtualdevice.flags.Flags.computerControlAccess()) {
            AVAILABLE
        } else {
            UNSUPPORTED_ON_DEVICE
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference?): Boolean {
        return if (preference?.key == mPreferenceKey) {
            mContext.startSpaActivity(ComputerControlAutomationAppListProvider.getAppListRoute())
            true
        } else {
            false
        }
    }

    override fun getSummary() =
        if (android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()) {
            mContext.getString(
                R.string.computer_control_automation_page_summary_flag_per_app_consent
            )
        } else {
            mContext.getString(R.string.computer_control_automation_page_summary)
        }
}
