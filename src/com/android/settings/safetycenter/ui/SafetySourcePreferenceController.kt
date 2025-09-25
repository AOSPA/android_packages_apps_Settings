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
import android.os.Flags as OsFlags
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.permission.flags.Flags as PermissionFlags
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
    private val userManager: UserManager = mContext.getSystemService(UserManager::class.java)!!
    private var viewModel: LiveSafetyCenterViewModel? = null
    private lateinit var safetySourceId: String
    private var profileType: SafetySourcePreference.Profile =
        SafetySourcePreference.Profile.PERSONAL

    /**
     * Sets the ViewModel instance for this controller and registers an observer to update the
     * preference state when [SafetyCenterUiData] changes.
     *
     * @param viewModel The [LiveSafetyCenterViewModel] instance.
     * @param owner The [LifecycleOwner] to scope the observation.
     */
    fun setViewModelAndLifecycle(viewModel: LiveSafetyCenterViewModel, owner: LifecycleOwner) {
        this.viewModel = viewModel
        viewModel.safetyCenterUiLiveData.observe(owner) { data ->
            if (data == null) {
                Log.d(TAG, "SafetyCenterUiData LiveData received null for $preferenceKey")
                return@observe
            }
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
            it.isVisible = false
        }
    }

    /** Updates the preference's UI elements based on the provided [SafetyCenterUiData]. */
    private fun updatePreferenceUi(preference: SafetySourcePreference, data: SafetyCenterUiData) {
        Log.d(TAG, "updatePreferenceUi with data for $preferenceKey")

        val targetUserHandle = getUserHandleForProfile(profileType)
        if (targetUserHandle == null) {
            Log.w(
                TAG,
                "No target UserHandle found for profile type $profileType for key $preferenceKey",
            )
            preference.isVisible = false
            return
        }

        val entry = data.getEntry(targetUserHandle.identifier, safetySourceId!!)
        if (entry == null) {
            Log.d(
                TAG,
                "No entry found for $safetySourceId and user ${targetUserHandle.identifier} for key $preferenceKey",
            )
            preference.isVisible = false
            return
        }

        preference.title = entry.title
        preference.summary = entry.summary
        preference.isVisible = true
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        viewModel?.let { vm ->
            val currentData = vm.getCurrentSafetyCenterDataAsUiData()
            updatePreferenceUi(preference as SafetySourcePreference, currentData)
        } ?: Log.w(TAG, "ViewModel not set in updateState for $preferenceKey, skipping UI update")
    }

    /**
     * Finds the [UserHandle] that matches the specified [SafetySourcePreference.Profile] type.
     *
     * @param profileType The type of profile (PERSONAL, WORK, PRIVATE) to search for.
     * @return The [UserHandle] of the matching profile, or null if not found.
     */
    private fun getUserHandleForProfile(profileType: SafetySourcePreference.Profile): UserHandle? {
        return userManager.userProfiles.firstOrNull { userHandle ->
            isUserMatchingProfileType(userHandle, profileType)
        }
    }

    /**
     * Checks if a given [UserHandle] matches the characteristics of the specified
     * [SafetySourcePreference.Profile].
     *
     * @param userHandle The [UserHandle] to check.
     * @param profileType The desired profile type.
     * @return True if the [UserHandle] matches the profile type, false otherwise.
     */
    private fun isUserMatchingProfileType(
        userHandle: UserHandle,
        profileType: SafetySourcePreference.Profile,
    ): Boolean {
        val userInfo = userManager.getUserInfo(userHandle.identifier)
        if (userInfo == null) {
            Log.e(TAG, "Failed to get UserInfo for user $userHandle")
            return false
        }

        val isPrivate = isPrivateProfileSupported() && userInfo.isPrivateProfile
        val isManaged = userInfo.isManagedProfile

        return when (profileType) {
            SafetySourcePreference.Profile.WORK -> isManaged
            SafetySourcePreference.Profile.PRIVATE -> isPrivate
            SafetySourcePreference.Profile.PERSONAL -> !userInfo.isProfile
        }
    }

    /** Checks if the private profile feature is supported and enabled on the device. */
    private fun isPrivateProfileSupported(): Boolean {
        return PermissionFlags.privateProfileSupported() && OsFlags.allowPrivateProfile()
    }

    companion object {
        private const val TAG = "SafetySourcePrefCtrl"
    }
}
