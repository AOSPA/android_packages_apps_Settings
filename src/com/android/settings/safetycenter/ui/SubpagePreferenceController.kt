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
import android.app.settings.SettingsEnums
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.permission.flags.Flags
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterIssue
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.safetycenter.SafetyCenterSeverityConverter.toEntrySeverityLevel
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.INVALID_SESSION_ID
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.createSessionArgs
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.safetycenter.SafetyCenterUiData
import kotlin.math.max

/**
 * A [BasePreferenceController] that manages a preference for launching a subpage in Safety Center.
 *
 * @property context The context of the controller.
 * @property preferenceKey The key of the preference managed by this controller.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
open class SubpagePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var preference: Preference? = null
    var sessionId = INVALID_SESSION_ID
    var relatedSafetySources: List<String> = emptyList()
    var relatedIssueOnlySafetySources: List<String> = emptyList()
    var viewModel: LiveSafetyCenterViewModel? = null
    @StringRes var defaultSummaryResId: Int? = null

    override fun onViewCreated(owner: LifecycleOwner) {
        if (viewModel == null) {
            Log.w(TAG, "[$preferenceKey] ViewModel not set, cannot observe LiveData")
            return
        }
        viewModel!!.safetyCenterUiLiveData.observe(owner) { data ->
            if (data == null) {
                Log.d(TAG, "[$preferenceKey] LiveData received null")
                return@observe
            }
            Log.d(TAG, "[$preferenceKey] safetyCenterUiLiveData observer notified")
            preference?.let { updatePreferenceUi(it, data) }
        }
    }

    protected open fun setIcon(preference: Preference, icon: Drawable?) {
        preference.icon = icon
    }

    protected open fun setVisibility(preference: Preference, isVisible: Boolean) {
        preference.isVisible = isVisible
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (!TextUtils.equals(preference.key, preferenceKey)) {
            return super.handlePreferenceTreeClick(preference)
        }

        val args: Bundle = createSessionArgs(sessionId)
        args.putAll(NavigationSource.SAFETY_CENTER.createArgs())
        SubSettingLauncher(mContext)
            .setDestination(preference.fragment)
            .setArguments(args)
            .setSourceMetricsCategory(SettingsEnums.SAFETY_CENTER)
            .launch()

        return true
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        relatedSafetySources =
            SafetyCenterSubpageRegistry.getXmlSafetySourceIds(mContext, preferenceKey)
        relatedIssueOnlySafetySources =
            SafetyCenterSubpageRegistry.getIssueOnlySafetySourceIds(preferenceKey)
        defaultSummaryResId = SafetyCenterSubpageRegistry.getDefaultSummaryResId(preferenceKey)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (preference != null && uiData != null) {
            updatePreferenceUi(preference!!, uiData)
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (preference != null && uiData != null) {
            updatePreferenceUi(preference, uiData)
        }
    }

    /**
     * Updates the preference's UI elements based on the provided data. This method is called by the
     * LiveData observer.
     */
    private fun updatePreferenceUi(preference: Preference, data: SafetyCenterUiData) {
        val relatedSafetySourcesData = getRelatedSafetySourcesData(data)
        val relatedIssueOnlySafetySourcesData = getRelatedIssueOnlySafetySourcesData(data)

        val subpageMaxSeverity =
            getSubpageMaxSeverity(relatedSafetySourcesData, relatedIssueOnlySafetySourcesData)

        val highestSeverityIssueOnlySafetySourceIssue =
            relatedIssueOnlySafetySourcesData.maxByOrNull { it.severityLevel }

        setIcon(preference, getSubpageIcon(subpageMaxSeverity, relatedSafetySourcesData))
        preference.summary =
            getSubpageSummary(
                data,
                relatedSafetySourcesData,
                highestSeverityIssueOnlySafetySourceIssue,
                subpageMaxSeverity,
            )
        setVisibility(
            preference,
            relatedSafetySourcesData.isNotEmpty() ||
                SafetyCenterSubpageRegistry.hasInjectedTiles(preferenceKey),
        )
        Log.d(TAG, "[$preferenceKey] UI updated with max severity: $subpageMaxSeverity")
    }

    private fun getSubpageMaxSeverity(
        relatedSafetySourcesData: List<SafetyCenterEntry>,
        relatedIssueOnlySafetySourcesData: List<SafetyCenterIssue>,
    ): Int? {
        val entrySeverities = relatedSafetySourcesData.map { it.severityLevel }
        val issueSeverities = relatedIssueOnlySafetySourcesData.map { toEntrySeverityLevel(it.severityLevel) }

        val allSeverities = entrySeverities + issueSeverities

        return allSeverities.reduceOrNull { acc, severity -> mergeSeverities(acc, severity) }
    }

    private fun mergeSeverities(left: Int, right: Int): Int {
        // Rule 1: Warnings and Recommendations always win
        if (left > SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK || right > SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK) {
            return max(left, right)
        }

        // Rule 2: If there are no warnings/recommendations, UNKNOWN trumps OK
        if (left == SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN || right == SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN) {
            return SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN
        }

        // Rule 3: Fallback to standard math for remaining cases (e.g., OK vs UNSPECIFIED)
        return max(left, right)
    }

    private fun getRelatedSafetySourcesData(data: SafetyCenterUiData): List<SafetyCenterEntry> {
        val entries = data.getDynamicEntriesForSources(relatedSafetySources)
        Log.d(
            TAG,
            "[$preferenceKey] Found ${entries.size} entries for sources: $relatedSafetySources",
        )
        return entries
    }

    private fun getRelatedIssueOnlySafetySourcesData(
        data: SafetyCenterUiData
    ): List<SafetyCenterIssue> {
        val issues = data.getActiveIssuesForSources(relatedIssueOnlySafetySources)
        Log.d(
            TAG,
            "[$preferenceKey] Found ${issues.size} issues for issue-only sources: $relatedIssueOnlySafetySources",
        )
        return issues
    }

    private fun getSubpageIcon(
        subpageMaxSeverity: Int?,
        relatedSafetySourcesData: List<SafetyCenterEntry>,
    ): Drawable? {
        if (subpageMaxSeverity == null) {
            Log.d(TAG, "[$preferenceKey] getSubpageIcon: Null max severity")
            return null
        }

        val iconResId: Int? =
            when (subpageMaxSeverity) {
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING -> R.drawable.ic_safety_warn
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION ->
                    R.drawable.ic_safety_recommendation
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK -> R.drawable.ic_safety_info
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED -> {
                    // Unspecified only comes from entries
                    val entryForIcon =
                        relatedSafetySourcesData.find { it.severityLevel == subpageMaxSeverity }
                    selectSeverityUnspecifiedIconResId(entryForIcon?.severityUnspecifiedIconType)
                }
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN -> R.drawable.ic_safety_null_state
                else -> {
                    Log.e(
                        TAG,
                        "[$preferenceKey] getSubpageIcon: Unknown maxSeverity level '$subpageMaxSeverity'",
                    )
                    R.drawable.ic_safety_null_state
                }
            }
        return iconResId?.let {
            AppCompatResources.getDrawable(
                ContextThemeWrapper(mContext, R.style.ThemeOverlay_SafetyCenterColors),
                it,
            )
        }
    }

    private fun selectSeverityUnspecifiedIconResId(severityUnspecifiedIconType: Int?): Int? {
        return when (severityUnspecifiedIconType) {
            SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON -> R.drawable.ic_safety_empty
            SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_PRIVACY -> R.drawable.ic_privacy
            SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_RECOMMENDATION ->
                R.drawable.ic_safety_null_state
            else -> {
                Log.e(
                    TAG,
                    "[$preferenceKey] Unknown SeverityNoneIconType: $severityUnspecifiedIconType",
                )
                R.drawable.ic_safety_null_state
            }
        }
    }

    private fun getSubpageSummary(
        data: SafetyCenterUiData,
        relatedSafetySourcesData: List<SafetyCenterEntry>,
        highestSeverityIssueOnlySafetySourceIssue: SafetyCenterIssue?,
        subpageMaxSeverity: Int?,
    ): CharSequence {
        if (subpageMaxSeverity == null) {
            Log.d(
                TAG,
                "[$preferenceKey] getSubpageSummary: Null max severity, returning default summary",
            )
            return getDefaultSubpageSummary(relatedSafetySourcesData)
        }

        when (subpageMaxSeverity) {
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING,
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION,
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK -> {
                for (entry in relatedSafetySourcesData) {
                    val entrySummary = entry.summary
                    if (entry.severityLevel != subpageMaxSeverity || entrySummary == null) {
                        continue
                    }

                    if (subpageMaxSeverity > SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK) {
                        return entrySummary
                    }

                    if (hasActiveIssues(data, entry)) {
                        return entrySummary
                    }
                }

                if (
                    highestSeverityIssueOnlySafetySourceIssue != null &&
                        toEntrySeverityLevel(
                            highestSeverityIssueOnlySafetySourceIssue.severityLevel
                        ) == subpageMaxSeverity
                ) {
                    return highestSeverityIssueOnlySafetySourceIssue.title
                }

                return getDefaultSubpageSummary(relatedSafetySourcesData)
            }
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED -> {
                return getDefaultSubpageSummary(relatedSafetySourcesData)
            }
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN -> {
                val hasError =
                    if (Flags.openSafetyCenterApis()) {
                        relatedSafetySourcesData.any { it.hasError() }
                    } else {
                        false
                    }
                return if (hasError) {
                    mContext.getString(R.string.safety_center_refresh_error)
                } else {
                    mContext.getString(R.string.safety_center_subpage_unknown_summary)
                }
            }
            else -> {
                Log.e(
                    TAG,
                    "[$preferenceKey] Unexpected maxSeverity $subpageMaxSeverity, returning default summary",
                )
                return getDefaultSubpageSummary(relatedSafetySourcesData)
            }
        }
    }

    private fun hasActiveIssues(data: SafetyCenterUiData, entry: SafetyCenterEntry): Boolean {
        if (!Flags.openSafetyCenterApis() || entry.safetySourceId == null) {
            return false
        }
        return data.getActiveIssuesForSources(listOf(entry.safetySourceId!!)).isNotEmpty()
    }

    protected open fun getDefaultSubpageSummary(
        safetySourceEntries: List<SafetyCenterEntry>
    ): CharSequence {
        return defaultSummaryResId?.let { mContext.getString(it) } ?: ""
    }

    override fun updateNonIndexableKeys(keys: MutableList<String>) {
        if (!SafetyCenterSubpageRegistry.isSubpageAvailable(mContext, preferenceKey)) {
            Log.d(TAG, "[$preferenceKey] Subpage is not available, adding to non-indexable keys")
            keys.add(preferenceKey)
        }
    }

    companion object {
        private const val TAG = "SubpagePrefController"
    }
}
