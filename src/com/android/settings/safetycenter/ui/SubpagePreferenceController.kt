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

package com.android.settings.safetycenter.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel

/**
 * A [BasePreferenceController] that manages a preference for launching a subpage in Safety Center.
 *
 * @property context The context of the controller.
 * @property preferenceKey The key of the preference managed by this controller.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
class SubpagePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var preference: Preference? = null
    private var relatedSafetySources: List<String> = emptyList()
    private var viewModel: LiveSafetyCenterViewModel? = null

    /**
     * Sets the ViewModel instance for this controller and registers the observer to update the
     * preference state when data changes.
     *
     * @param viewModel The LiveSafetyCenterViewModel instance.
     * @param owner The LifecycleOwner to scope the observation.
     */
    fun setViewModelAndLifecycle(viewModel: LiveSafetyCenterViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel

        viewModel.safetyCenterUiLiveData.observe(owner) { data ->
            if (data == null) {
                Log.d(TAG, "SafetyCenterData LiveData received null for $preferenceKey")
                return@observe
            }
            Log.d(TAG, "safetyCenterUiLiveData observer notified for $preferenceKey")
            preference?.let { updatePreferenceUi(it, data) }
        }
    }

    /**
     * Sets the list of related safety source IDs for this subpage.
     *
     * @param relatedSafetySources The list of safety source IDs.
     */
    fun setRelatedSafetySources(relatedSafetySources: List<String>) {
        this.relatedSafetySources = relatedSafetySources
    }

    override fun getAvailabilityStatus(): Int {
        // TODO: b/424132940 - Add logic to check for preference availability.
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun getSummary(): CharSequence {
        // TODO: b/424132940 - Implement logic to calculate the summary based on safety sources
        // data.
        return ""
    }

    /**
     * Updates the preference's UI elements based on the provided data. This method is called by the
     * LiveData observer.
     */
    private fun updatePreferenceUi(preference: Preference, data: SafetyCenterData) {
        Log.d(TAG, "updatePreferenceUi with data for $preferenceKey")
        preference.icon = getSubpageIcon(data)
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        viewModel?.let { vm ->
            val currentData = vm.getCurrentSafetyCenterDataAsUiData()
            updatePreferenceUi(preference, currentData)
        } ?: Log.w(TAG, "ViewModel not set in updateState for $preferenceKey, skipping UI update")
    }

    private fun getRelatedSafetySourcesData(data: SafetyCenterData): List<SafetyCenterEntry> {
        return data.entriesOrGroups
            .flatMap { entryOrGroup ->
                // Combines the standalone entry (if it exists) and the list of group entries
                listOfNotNull(entryOrGroup.entry) +
                    (entryOrGroup.entryGroup?.entries ?: emptyList())
            }
            .filter { entry -> relatedSafetySources.contains(entry.safetySourceId) }
            .also { filteredList ->
                Log.d(
                    TAG,
                    "getRelatedSafetySourcesData for $preferenceKey: ${filteredList.size} entries found matching $relatedSafetySources",
                )
            }
    }

    private fun getSubpageIcon(data: SafetyCenterData): Drawable? {
        Log.d(TAG, "getSubpageIcon called for $preferenceKey")

        val relatedSafetySourcesData = getRelatedSafetySourcesData(data)

        if (relatedSafetySourcesData.isEmpty()) {
            Log.d(TAG, "No relevant entries found for $preferenceKey")
            return null
        }

        val maxSeverity =
            relatedSafetySourcesData.maxOfOrNull { it.severityLevel }
                ?: SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN

        Log.d(TAG, "Max Severity for $preferenceKey: $maxSeverity")

        val iconResId: Int? =
            when (maxSeverity) {
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING -> R.drawable.ic_safety_warn
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION ->
                    R.drawable.ic_safety_recommendation
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK -> R.drawable.ic_safety_info
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED -> {
                    val entryForIconType =
                        relatedSafetySourcesData.find { it.severityLevel == maxSeverity }
                    selectSeverityUnspecifiedIconResId(
                        entryForIconType?.severityUnspecifiedIconType
                    )
                }
                else -> {
                    Log.e(
                        TAG,
                        "getSubpageIcon: Unknown maxSeverity level '$maxSeverity' for key '$preferenceKey'",
                    )
                    null
                }
            }
        return iconResId?.let { mContext.getDrawable(it) }
    }

    private fun selectSeverityUnspecifiedIconResId(severityUnspecifiedIconType: Int?): Int? {
        return when (severityUnspecifiedIconType) {
            SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON -> R.drawable.ic_safety_empty
            SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_PRIVACY -> R.drawable.ic_privacy
            else -> {
                Log.e(
                    TAG,
                    String.format(
                        "Unknown SafetyCenterEntry.SeverityNoneIconType: %s",
                        severityUnspecifiedIconType,
                    ),
                )
                null
            }
        }
    }

    companion object {
        private const val TAG = "SubpagePrefController"
    }
}
