/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.deviceinfo.legal

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.annotation.StringRes
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

// LINT.IfChange
class LegalPreference(
    override val key: String,
    @StringRes override val purpose: Int,
    @StringRes val defaultTitle: Int = 0,
    val intentAction: String,
) : PreferenceMetadata, PreferenceTitleProvider, PreferenceAvailabilityProvider {

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? {
        val resolveInfo =
            findMatchingSpecificActivity(context) ?: return context.getText(defaultTitle)
        return resolveInfo.loadLabel(context.packageManager)
    }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = (findMatchingSpecificActivity(context) != null)

    override fun intent(context: Context) =
        findMatchingSpecificActivity(context)?.let {
            Intent()
                .setClassName(it.activityInfo.packageName, it.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun findMatchingSpecificActivity(context: Context): ResolveInfo? {
        val intent = Intent(intentAction)
        // Find the activity that is in the system image
        val list: List<ResolveInfo> = context.packageManager.queryIntentActivities(intent, 0)
        return list.firstOrNull {
            (it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }
    }
}
// LINT.ThenChange(LegalPreferenceController.java)
