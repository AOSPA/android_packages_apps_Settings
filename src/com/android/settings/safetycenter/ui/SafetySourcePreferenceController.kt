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
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settingslib.safetycenter.SafetyCenterUiData
import com.android.settingslib.safetycenter.SafetySourcePreference

/**
 * A PreferenceController to manage individual [SafetySourcePreference] entries within Safety Center
 * subpages.
 *
 * This controller fetches the safety source ID and profile type from the preference attributes,
 * observes [LiveSafetyCenterViewModel] for data updates, and updates the preference's UI. It
 * handles different user profiles (Personal, Work, Private) to display the correct entry.
 */
// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs used by the ViewModel.
@SuppressLint("MissingPermission")
class SafetySourcePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var preference: SafetySourcePreference? = null
    var viewModel: LiveSafetyCenterViewModel? = null
    var activityTaskId: Int? = null
    private lateinit var safetySourceId: String
    private var profileType: SafetySourcePreference.Profile =
        SafetySourcePreference.Profile.PERSONAL

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

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        preference?.let {
            safetySourceId = requireNotNull(it.safetySource)
            profileType = it.profile
            val uiData = viewModel?.safetyCenterUiLiveData?.value
            if (uiData != null) {
                updatePreferenceUi(preference as SafetySourcePreference, uiData)
            }
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        val uiData = viewModel?.safetyCenterUiLiveData?.value
        if (preference != null && uiData != null) {
            updatePreferenceUi(preference as SafetySourcePreference, uiData)
        }
    }

    /** Updates the preference's UI elements based on the provided [SafetyCenterUiData]. */
    private fun updatePreferenceUi(preference: SafetySourcePreference, data: SafetyCenterUiData) {
        val targetUserHandle = UserProfilesUtils.getUserHandleForProfile(mContext, profileType)
        if (targetUserHandle == null) {
            Log.w(TAG, "[$preferenceKey] No target UserHandle found for profile type $profileType")
            preference.isVisible = false
            return
        }

        val entry = data.getEntry(targetUserHandle.identifier, safetySourceId)
        if (entry == null) {
            Log.d(
                TAG,
                "[$preferenceKey] No entry found for $safetySourceId and user ${targetUserHandle.identifier}",
            )
            preference.isVisible = false
            return
        }

        Log.d(
            TAG,
            "[$preferenceKey] Updating UI for entry with safetySourceId $safetySourceId and user ${targetUserHandle.identifier}",
        )
        preference.title = entry.title
        preference.summary = entry.summary
        preference.isVisible = true
        preference.setOnPreferenceClickListener {
            PendingIntentSender.sendIntent(
                    mContext,
                    entry.pendingIntent,
                    safetySourceId,
                    activityTaskId,
                )
                .also { wasIntentSent ->
                    if (wasIntentSent) {
                        viewModel?.interactionLogger?.recordForEntry(Action.ENTRY_CLICKED, entry)
                    }
                }
        }
    }

    companion object {
        private const val TAG = "SafetySourcePrefCtrl"
    }
}
