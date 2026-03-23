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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TtsSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceGroup
import com.android.settings.CatalystFragment
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settingslib.preference.forEachRecursively
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment to display the Supervision settings landing page (Settings > Supervision).
 *
 * See [SupervisionDashboardScreen] for details on the page contents.
 */
class SupervisionDashboardFragment : CatalystFragment() {
    private var supervisionClient: SupervisionMessengerClient? = null
    private var supervisionManager: SupervisionManager? = null
    private var preferenceDataMap: Map<String, PreferenceData>? = null
    private var originalClickListenerMap = mutableMapOf<String, OnPreferenceClickListener?>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        supervisionClient = getSupervisionClient(context)
        supervisionManager = context.getSystemService(SupervisionManager::class.java)
    }

    public override fun onResume() {
        super.onResume()

        if (!Flags.enableSupervisionSettingsUiUpdates()) {
            return
        }

        // Fetch fresh preference data.
        val supervisionClient = getSupervisionClient(requireContext())
        lifecycleScope.launch {
            val preferences = featurePreferences()
            val preferenceKeys = preferences.map { it.key }
            preferenceDataMap =
                withContext(ioDispatcher) { supervisionClient.getPreferenceData(preferenceKeys) }
            preferences.forEach { preference ->
                val data = preferenceDataMap?.get(preference.key)
                if (data != null) {
                    updatePreferenceDataSummary(preference, data)
                }
            }
        }
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
        val supervisionClient = getSupervisionClient(requireContext())
        val preferences = featurePreferences()
        val preferenceKeys = preferences.map { it.key }
        val cachedData = supervisionClient.getCachedPreferenceData(preferenceKeys)
        if (cachedData.isNotEmpty()) {
            preferenceDataMap = cachedData
        }

        val isSupervisionEnabled = supervisionManager?.isSupervisionEnabled == true
        val isPinSet = supervisionManager?.createConfirmSupervisionCredentialsIntent() != null
        val shouldRedirectToSetupSupervision = !isSupervisionEnabled || !isPinSet

        preferences.forEach { preference ->
            val data = preferenceDataMap?.get(preference.key)
            if (data != null) {
                updatePreferenceDataSummary(preference, data)
            }

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

    private fun getSupervisionClient(context: Context) =
        supervisionClient ?: SupervisionMessengerClient(context).also { supervisionClient = it }

    private fun featurePreferences() =
        buildList<Preference> {
            SupervisionDashboardScreen.FEATURE_GROUP_KEYS.forEach { key ->
                findPreference<PreferenceGroup>(key)?.let { group ->
                    group.forEachRecursively { add(it) }
                }
            }
        }

        private fun updatePreferenceDataSummary(preference: Preference, data: PreferenceData) {
        val newSummary = data.summary
        if (newSummary != null) {
            val contentDescription = data.summaryContentDescription
            if (contentDescription != null) {
                // This tells screen readers (like TalkBack) to read the summary
                // content description instead of the summary.
                val spannable = SpannableString(newSummary)
                spannable.setSpan(
                    TtsSpan.TextBuilder(contentDescription.toString()).build(),
                    0,
                    newSummary.length,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE,
                )
                preference.summary = spannable
            } else {
                preference.summary = newSummary
            }
        }
    }

    companion object {
        @VisibleForTesting var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    }
}
