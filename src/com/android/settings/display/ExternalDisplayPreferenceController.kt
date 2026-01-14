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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.connecteddevice.DevicePreferenceCallback
import com.android.settings.connecteddevice.display.ExternalDisplayListUpdater
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle

/** Controller to update the [androidx.preference.Preference] for external display. */
class ExternalDisplayPreferenceController :
    BasePreferenceController,
    DefaultLifecycleObserver,
    DevicePreferenceCallback {

    private val externalDisplayListUpdater: ExternalDisplayListUpdater
    private var preferenceGroup: PreferenceGroup? = null
    private var fragment: PreferenceFragmentCompat? = null

    constructor(
        context: Context,
        lifecycle: Lifecycle?
    ) : super(context, PREF_KEY_EXTERNAL_DISPLAY_CATEGORY) {
        externalDisplayListUpdater = ExternalDisplayListUpdater(this, SettingsEnums.DISPLAY)
        lifecycle?.addObserver(this)
    }

    @VisibleForTesting
    constructor(
        context: Context,
        updater: ExternalDisplayListUpdater,
        lifecycle: Lifecycle?
    ) : super(context, PREF_KEY_EXTERNAL_DISPLAY_CATEGORY) {
        externalDisplayListUpdater = updater
        lifecycle?.addObserver(this)
    }

    fun setFragment(fragment: PreferenceFragmentCompat?) {
        this.fragment = fragment
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun onStart(owner: LifecycleOwner) {
        externalDisplayListUpdater.registerCallback()
        externalDisplayListUpdater.refreshPreference()
    }

    override fun onStop(owner: LifecycleOwner) {
        externalDisplayListUpdater.unregisterCallback()
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preferenceGroup = screen
        externalDisplayListUpdater.initPreference(screen.context)
    }

    override fun onDeviceAdded(preference: Preference) {
        val group = preferenceGroup ?: return

        var category = group.findPreference<PreferenceCategory>(PREF_KEY_EXTERNAL_DISPLAY_CATEGORY)
        if (category == null) {
            category = PreferenceCategory(mContext)
            category.key = PREF_KEY_EXTERNAL_DISPLAY_CATEGORY
            category.setTitle(R.string.external_display_settings_title)
            category.order = EXTERNAL_DISPLAY_PREFERENCE_ORDER
            group.addPreference(category)
        }

        // Ensure the preference is added to the category.
        if (category.findPreference<Preference>(preference.key) == null) {
            category.addPreference(preference)
        }

        // NOTE: Scroll to the preference category to ensure the preference is visible.
        fragment?.scrollToPreference(category)
    }

    override fun onDeviceRemoved(preference: Preference) {
        val group = preferenceGroup ?: return

        val category = group.findPreference<PreferenceCategory>(PREF_KEY_EXTERNAL_DISPLAY_CATEGORY)
        if (category != null) {
            category.removePreference(preference)
            if (category.preferenceCount == 0) {
                group.removePreference(category)
            }
        }
    }

    companion object {
        // The order of the preference in the preference group. Make sure it is small enough to be
        // ranked at the top of the preference list.
        private const val EXTERNAL_DISPLAY_PREFERENCE_ORDER = -500

        @VisibleForTesting
        const val PREF_KEY_EXTERNAL_DISPLAY_CATEGORY = "external_display_category"
    }
}
