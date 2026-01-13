/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.content.Intent
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterStatus
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.StatusUiData
import com.android.settingslib.safetycenter.SafetyCenterUiData
import com.android.settingslib.widget.StatusBannerPreference

/**
 * Controller for the [StatusBannerPreference] that displays the overall status of the Safety
 * Center.
 *
 * This class observes data from the [LiveSafetyCenterViewModel] and updates the UI of the banner to
 * reflect the current security state, including dynamic changes like a scanning animation.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
class StatusBannerPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var preference: StatusBannerPreference? = null
    var viewModel: LiveSafetyCenterViewModel? = null
    var isQuickSettings: Boolean = false

    override fun onViewCreated(owner: LifecycleOwner) {
        if (viewModel == null) {
            Log.w(TAG, "[$preferenceKey] ViewModel not set, cannot observe LiveData")
            return
        }
        viewModel!!.statusUiLiveData.observe(owner) { statusUiData ->
            Log.d(TAG, "[$preferenceKey] statusUiLiveData observer notified")
            updateUi(statusUiData)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (uiData != null) {
            updateUi(StatusUiData(uiData))
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (uiData != null) {
            updateUi(StatusUiData(uiData))
        }
    }

    private fun updateUi(statusUiData: StatusUiData?) {
        val status = statusUiData ?: return
        val pref = preference ?: return

        pref.showSafetyStatus(status)
        pref.updateBannerButton(status)
    }

    private fun StatusBannerPreference.showSafetyStatus(status: StatusUiData) {
        title = status.title
        summary = status.summary
        applyIconTint = false
        iconLevel = status.bannerStatus

        val iconResId: Int =
            if (status.isRefreshInProgress) {
                playIconAnimationInLoop = true
                showIconBackground = false
                getShieldOnlyAnimatedIconResId(status.severityLevel)
            } else {
                playIconAnimationInLoop = false
                showIconBackground = true
                getShieldWithGlyphIconResId(status.severityLevel)
            }
        icon =
            AppCompatResources.getDrawable(
                ContextThemeWrapper(context, R.style.ThemeOverlay_SafetyCenterColors),
                iconResId,
            )
    }

    @DrawableRes
    private fun getShieldWithGlyphIconResId(severityLevel: Int): Int {
        return when (severityLevel) {
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK ->
                R.drawable.safety_center_expressive_shield_glyph_icon_status_level_low
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION ->
                R.drawable.safety_center_expressive_shield_glyph_icon_status_level_medium
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING ->
                R.drawable.safety_center_expressive_shield_glyph_icon_status_level_high
            else -> {
                Log.w(
                    TAG,
                    "Unexpected OverallSeverityLevel: $severityLevel, defaulting to low severity shield with glyph icon",
                )
                R.drawable.safety_center_expressive_shield_glyph_icon_status_level_low
            }
        }
    }

    @DrawableRes
    private fun getShieldOnlyAnimatedIconResId(severityLevel: Int): Int {
        return when (severityLevel) {
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK ->
                R.drawable.safety_center_expressive_shield_status_level_low_anim
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION ->
                R.drawable.safety_center_expressive_shield_status_level_medium_anim
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING ->
                R.drawable.safety_center_expressive_shield_status_level_high_anim
            else -> {
                Log.w(
                    TAG,
                    "Unexpected OverallSeverityLevel: $severityLevel, defaulting to low shield",
                )
                R.drawable.safety_center_expressive_shield_status_level_low_anim
            }
        }
    }

    private fun StatusBannerPreference.updateBannerButton(status: StatusUiData) {
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        val hasActiveIssues = uiData?.getActiveIssues()?.isNotEmpty() ?: false

        if (uiData != null && hasActiveIssues) {
            setButtonOnClickListener(null) // This hides the button.
        } else if (
            uiData != null &&
                isQuickSettings &&
                !status.isRefreshInProgress &&
                (hasUnknownSeverityEntry(uiData) ||
                    hasEntryExceedingOverallSeverity(uiData, status.severityLevel))
        ) {
            setButtonText(R.string.safety_center_review_settings)
            setButtonOnClickListener {
                val intent = Intent(Intent.ACTION_SAFETY_CENTER)
                NavigationSource.QUICK_SETTINGS_TILE.addToIntent(intent)
                context.startActivity(intent)
            }
            buttonLevel = StatusBannerPreference.BannerStatus.LOW
            isButtonEnabled = true
        } else if (
            uiData == null ||
                status.severityLevel == SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK ||
                status.severityLevel == SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_UNKNOWN
        ) {
            setButtonText(R.string.safety_center_rescan_button)
            setButtonOnClickListener {
                if (triggerRescan()) {
                    isButtonEnabled = false
                    viewModel?.interactionLogger?.record(Action.SCAN_INITIATED)
                }
            }
            buttonLevel = StatusBannerPreference.BannerStatus.LOW
            isButtonEnabled = !status.isRefreshInProgress
        } else {
            setButtonOnClickListener(null) // This hides the button.
        }
    }

    private fun getAllVisibleEntries(uiData: SafetyCenterUiData): List<SafetyCenterEntry> {
        return SafetyCenterSubpageRegistry.subpageConfigs.keys.flatMap { subpageKey ->
            val sourceIds = SafetyCenterSubpageRegistry.getXmlSafetySourceIds(mContext, subpageKey)
            uiData.getDynamicEntriesForSources(sourceIds)
        }
    }

    private fun hasUnknownSeverityEntry(uiData: SafetyCenterUiData): Boolean {
        return getAllVisibleEntries(uiData).any {
            it.severityLevel == SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN
        }
    }

    private fun hasEntryExceedingOverallSeverity(
        uiData: SafetyCenterUiData,
        overallSeverity: Int,
    ): Boolean {
        val overallSeverityComparable = overallSeverity.toComparableSeverity()
        return getAllVisibleEntries(uiData).any { entry ->
            entry.severityLevel.toComparableSeverity() > overallSeverityComparable
        }
    }

    private fun Int.toComparableSeverity(): Int =
        when (this) {
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING,
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING -> 3
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION,
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION -> 2
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK,
            SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK -> 1
            else -> 0
        }

    private fun triggerRescan(): Boolean {
        if (viewModel?.statusUiLiveData?.value?.isRefreshInProgress == false) {
            (viewModel?.rescan())
            return true
        }
        return false
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    companion object {
        private const val TAG = "StatusBannerPrefCtrl"
    }
}
