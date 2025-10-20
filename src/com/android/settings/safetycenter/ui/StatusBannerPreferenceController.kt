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
import android.safetycenter.SafetyCenterStatus
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.StatusUiData
import com.android.settingslib.widget.StatusBannerPreference

/**
 * Controller for the [StatusBannerPreference] that displays the overall status of the Safety
 * Center.
 *
 * This class observes data from the [LiveSafetyCenterViewModel] and updates the UI of the banner to
 * reflect the current security state, including dynamic changes like a scanning animation.
 */
class StatusBannerPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var preference: StatusBannerPreference? = null
    private var viewModel: LiveSafetyCenterViewModel? = null

    fun setViewModelAndLifecycle(viewModel: LiveSafetyCenterViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel
        this.viewModel?.statusUiLiveData?.observe(owner) { statusUiData -> updateUi(statusUiData) }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        updateUi(viewModel?.statusUiLiveData?.value)
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
                getShieldOnlyIconResId(status.severityLevel)
            } else {
                getShieldWithGlyphIconResId(status.severityLevel)
            }
        icon = ContextCompat.getDrawable(context, iconResId)
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
    private fun getShieldOnlyIconResId(severityLevel: Int): Int {
        return when (severityLevel) {
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK ->
                R.drawable.safety_center_expressive_shield_status_level_low
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION ->
                R.drawable.safety_center_expressive_shield_status_level_medium
            SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING ->
                R.drawable.safety_center_expressive_shield_status_level_high
            else -> {
                Log.w(
                    TAG,
                    "Unexpected OverallSeverityLevel: $severityLevel, defaulting to low shield",
                )
                R.drawable.safety_center_expressive_shield_status_level_low
            }
        }
    }

    private fun StatusBannerPreference.updateBannerButton(status: StatusUiData) {
        if (status.severityLevel == SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK) {
            setButtonText(R.string.safety_center_rescan_button)
            setButtonOnClickListener {
                if (triggerRescan()) {
                    isButtonEnabled = false
                }
            }
            buttonLevel = StatusBannerPreference.BannerStatus.LOW
            isButtonEnabled = !status.isRefreshInProgress
        } else {
            setButtonOnClickListener(null) // This hides the button.
        }
    }

    private fun triggerRescan(): Boolean {
        if (viewModel?.statusUiLiveData?.value?.isRefreshInProgress == false) {
            @SuppressLint("MissingPermission")
            // LiveSafetyCenterViewModel.rescan() in Settings app (has MANAGE_SAFETY_CENTER)
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
