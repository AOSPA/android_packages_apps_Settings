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

import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceGroup
import com.android.settings.CatalystFragment
import com.android.settingslib.preference.forEachRecursively

/**
 * Fragment to display the Supervision settings landing page (Settings > Supervision).
 *
 * See [SupervisionDashboardScreen] for details on the page contents.
 */
class SupervisionDashboardFragment : CatalystFragment() {
    private var supervisionManager: SupervisionManager? = null
    private var originalClickListenerMap = mutableMapOf<String, OnPreferenceClickListener?>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        supervisionManager = context.getSystemService(SupervisionManager::class.java)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun refreshDashboardTiles(tag: String) {
        super.refreshDashboardTiles(tag)

        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return
        }

        // Refresh dynamic preference overrides.
        // Doing this here ensures that the values do not get overridden if there is
        // another tile refresh (e.g. when navigating back from SetupSupervisionActivity).
        val preferences = featurePreferences()
        val shouldRedirectToSetupSupervision = supervisionManager?.isSupervisionEnabled == false

        preferences.forEach { preference ->
            if (!originalClickListenerMap.containsKey(preference.key)) {
                // Store the original click listener, only if we haven't already done so.
                originalClickListenerMap.put(preference.key, preference.onPreferenceClickListener)
            }

            // If supervision is not enabled, clicks on feature tiles should redirect to
            // SetupSupervisionActivity. Once supervision is enabled, restore the
            // original click listener.
            if (shouldRedirectToSetupSupervision) {
                preference.setOnPreferenceClickListener {
                    startActivity(Intent(context, SetupSupervisionActivity::class.java))
                    true
                }
            } else {
                preference.onPreferenceClickListener = originalClickListenerMap?.get(preference.key)
            }
        }
    }

    override fun getPreferenceScreenBindingKey(context: Context) = SupervisionDashboardScreen.KEY

    private fun featurePreferences() =
        buildList<Preference> {
            SupervisionDashboardScreen.FEATURE_GROUP_KEYS.forEach { key ->
                findPreference<PreferenceGroup>(key)?.let { group ->
                    group.forEachRecursively { add(it) }
                }
            }
        }
}
