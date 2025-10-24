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

import android.content.Context
import android.safetycenter.SafetyCenterStatus
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.StatusUiData
import com.android.settingslib.widget.StatusBannerPreference
import com.android.settingslib.widget.preference.statusbanner.R as StatusBannerR

/**
 * Controller for the [StatusBannerPreference] that displays the overall status of the Safety
 * Center.
 *
 * This class observes data from the [LiveSafetyCenterViewModel] and updates the UI of the banner to
 * reflect the current security state, including dynamic changes like a scanning animation.
 */
class StatusBannerPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var mPreference: StatusBannerPreference? = null
    private var mViewModel: LiveSafetyCenterViewModel? = null

    fun setViewModelAndLifecycle(viewModel: LiveSafetyCenterViewModel, owner: LifecycleOwner) {
        mViewModel = viewModel
        mViewModel?.statusUiLiveData?.observe(owner) { statusUiData -> updateUi(statusUiData) }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        updateUi(mViewModel?.statusUiLiveData?.value)
    }

    private fun updateUi(statusUiData: StatusUiData?) {
        statusUiData?.let { data ->
            mPreference?.apply {
                title = data.title
                summary = data.summary
                iconLevel = data.bannerStatus
                if (data.isRefreshInProgress) {
                    icon =
                        ContextCompat.getDrawable(
                            context,
                            StatusBannerR.drawable.settingslib_expressive_icon_status_level_low,
                        )
                } else {
                    when (data.severityLevel) {
                        SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK -> {
                            icon =
                                ContextCompat.getDrawable(
                                    context,
                                    StatusBannerR.drawable
                                        .settingslib_expressive_icon_status_level_low,
                                )
                        }
                        SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION -> {
                            icon =
                                ContextCompat.getDrawable(
                                    context,
                                    StatusBannerR.drawable
                                        .settingslib_expressive_icon_status_level_medium,
                                )
                        }
                        SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING -> {
                            icon =
                                ContextCompat.getDrawable(
                                    context,
                                    StatusBannerR.drawable
                                        .settingslib_expressive_icon_status_level_high,
                                )
                        }
                        else -> {
                            Log.w(
                                TAG,
                                "Unexpected OverallSeverityLevel: ${statusUiData.severityLevel}",
                            )
                            icon =
                                ContextCompat.getDrawable(
                                    context,
                                    StatusBannerR.drawable
                                        .settingslib_expressive_icon_status_level_low,
                                )
                        }
                    }
                }
            }
        }
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    companion object {
        private const val TAG = "StatusBannerPrefCtrl"
    }
}
