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
import android.content.pm.PackageManager
import android.provider.Settings.Secure
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AutoclickNavigationSuggestionDialogFragment
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.MainSwitchPreferenceBinding

class AutoclickMainSwitchPreference :
    BooleanValuePreference, MainSwitchPreferenceBinding, PreferenceLifecycleProvider {
    override val key: String
        get() = SETTING_KEY

    override val purpose: Int
        get() = R.string.a11y_autoclick_main_switch_purpose

    override val title: Int
        get() = R.string.accessibility_autoclick_main_switch_title

    override fun storage(context: Context) = SettingsSecureStore.get(context).apply { setDefaultValue(SETTING_KEY, false) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        val mainSwitch = context.findPreference<Preference>(bindingKey) ?: return
        mainSwitch.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (
                    newValue == true &&
                        isGestureNavigationEnabled(context) &&
                        !isLaptopDevice(context)
                ) {
                    AutoclickNavigationSuggestionDialogFragment.newInstance()
                        .show(
                            context.childFragmentManager,
                            AutoclickNavigationSuggestionDialogFragment::class.java.name,
                        )
                }
                true
            }
    }

    override val sensitivityLevel
        get() = SensitivityLevel.DEEP_LINK_ONLY

    private fun isGestureNavigationEnabled(context: Context): Boolean =
        SettingsSecureStore.get(context).getInt(Secure.NAVIGATION_MODE) == NAV_BAR_MODE_GESTURAL

    /** Checks if the current device is a laptop device. */
    private fun isLaptopDevice(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PC)

    companion object {
        const val SETTING_KEY = Secure.ACCESSIBILITY_AUTOCLICK_ENABLED
    }
}
