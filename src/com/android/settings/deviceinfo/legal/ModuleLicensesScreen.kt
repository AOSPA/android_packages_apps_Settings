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

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(ModuleLicensesScreen.KEY)
open class ModuleLicensesScreen :
    PreferenceScreenMixin, PreferenceBinding, PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)


    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.module_license_purpose

    override val title: Int
        get() = R.string.module_license_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_about_device

    override fun getMetricsCategory() = SettingsEnums.MODULE_LICENSES_DASHBOARD

    override fun isFlagEnabled(context: Context) = Flags.catalystMigration26q2()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ModuleLicensesDashboard::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override val availabilityDescription =
        "The device must expose module licenses."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        val modules = context.packageManager.getInstalledModules(/* flags= */ 0)
        return modules.any {
            try {
                ModuleLicenseProvider.getPackageAssetManager(context.packageManager, it.packageName)
                    .list("")
                    ?.contains(ModuleLicenseProvider.GZIPPED_LICENSE_FILE_NAME) == true
            } catch (e: Exception) {
                false
            }
        }
    }

    // TODO(b/469578681) Clean up the extra adding attributes
    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (isFlagEnabled(preference.context)) {
            preference.isEnabled = true
            preference.isVisible = true
        }
    }

    companion object {
        const val KEY = "module_license"
    }
}
// LINT.ThenChange(ModuleLicensesListPreferenceController.java,
//                 ModuleLicensesDashboard.java)
