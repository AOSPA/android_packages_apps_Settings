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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController
import com.android.settings.supervision.SupervisionDashboardActivity.Companion.INSTALL_SUPERVISION_APP_ACTION

/** Controller for the top level Supervision settings Preference item. */
class TopLevelSupervisionPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == preferenceKey) {
            val intent = Intent(mContext, SupervisionDashboardActivity::class.java)
            mContext.startActivity(intent)
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }

    override fun getAvailabilityStatus(): Int {
        // Hide the supervision entry in settings if the necessary supervision component is not
        // available and can't be fixed by user.
        val hasNecessarySupervisionComponent =
            mContext.hasNecessarySupervisionComponent(matchAll = true)
        if (
            !Flags.enableSupervisionSettingsScreen() ||
                !hasNecessarySupervisionComponent && !supervisionAppSupportsInstallAction()
        ) {
            return UNSUPPORTED_ON_DEVICE
        }

        return AVAILABLE
    }

    private fun supervisionAppSupportsInstallAction(): Boolean {
        if (mContext.systemSupervisionPackageName == null) {
            return false
        }

        val intent =
            Intent(INSTALL_SUPERVISION_APP_ACTION).setPackage(mContext.systemSupervisionPackageName)
        return mContext.packageManager
            .queryIntentActivitiesAsUser(intent, 0, mContext.userId)
            .isNotEmpty()
    }
}
