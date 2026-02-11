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

package com.android.settings.applications.specialaccess.zenaccess

import android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
import com.android.settings.R
import com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

// LINT.IfChange
@ProvidePreferenceScreen(ZenAccessDetailsApiScreen.KEY, parameterized = true)
class ZenAccessDetailsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = ZenAccessDetails::class,
        purpose = R.string.zen_access_detail_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = ARG_PACKAGE_NAME,
                purpose = R.string.zen_access_parameter_purpose,
                required = true,
                type = InstalledPackageName(heldPermissions = PERM),
            )

            prepareScreenExtras { keyParameters, extras ->
                extras.putString(ARG_PACKAGE_NAME, keyParameters[ARG_PACKAGE_NAME])
            }
        }
    }

    companion object {
        const val KEY = "zen_access_detail"
        private val PERM = arrayOf(ACCESS_NOTIFICATION_POLICY)
    }
}
// LINT.ThenChange(ZenAccessDetails.java, ZenAccessController.java)
