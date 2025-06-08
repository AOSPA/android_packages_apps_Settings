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

package com.android.settings.privacy

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.permission.flags.Flags
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController

/** PreferenceController which hides the App Function access if app functions aren't enabled */
class AppFunctionAccessPreferenceController(context: Context, preferenceKey: String)
    : BasePreferenceController(context, preferenceKey) {
    override fun getAvailabilityStatus(): Int {
        return if (isAppFunctionAccessEnabled(mContext)) {
            AVAILABLE
        } else {
            UNSUPPORTED_ON_DEVICE
        }
    }

    private fun isAppFunctionAccessEnabled(context: Context) : Boolean {
        val packageManager: PackageManager = context.getPackageManager()
        // TODO(b/414805948): Add app function access API flag here once exported
        return Flags.appFunctionAccessUiEnabled()
            && !packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
            && !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
            && !packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val pref = screen.findPreference<Preference>(preferenceKey)
        if (pref != null) {
            pref.intent = Intent(AppFunctionManager.ACTION_MANAGE_APP_FUNCTION_ACCESS)
                .setPackage(mContext.packageManager.permissionControllerPackageName)
        }
    }
}
