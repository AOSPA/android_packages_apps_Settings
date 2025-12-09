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

package com.android.settings.gestures

import android.content.Context
import android.content.pm.PackageManager
import android.service.quickaccesswallet.QuickAccessWalletClient
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.widget.SelectorWithWidgetPreference

// LINT.IfChange
sealed class DoubleTapPowerBasePreference(private val dataStore: DoubleTapPowerStorage) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    SelectorWithWidgetPreference.OnClickListener,
    PreferenceAvailabilityProvider {

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun createWidget(context: Context) = SelectorWithWidgetPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as SelectorWithWidgetPreference).setOnClickListener(this)
    }

    override fun onRadioButtonClicked(emiter: SelectorWithWidgetPreference) {
        emiter.isChecked = true
    }
}

/** The "Launch camera" preference. */
class DoubleTapPowerForCameraPreference(dataStore: DoubleTapPowerStorage) :
    DoubleTapPowerBasePreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.double_tap_power_camera_action_title

    override val purpose: Int
        get() = R.string.gesture_double_power_tap_camera_purpose

    override fun isEnabled(context: Context) =
        DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(context)

    override fun isAvailable(context: Context) =
        DoubleTapPowerSettingsUtils.isMultiTargetDoubleTapPowerButtonGestureAvailable(context)

    companion object {
        const val KEY = "gesture_double_power_tap_camera"
    }
}

/** The "Digital wallet" preference. */
class DoubleTapPowerForWalletPreference(dataStore: DoubleTapPowerStorage) :
    DoubleTapPowerBasePreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.double_tap_power_wallet_action_title

    override val keywords
        get() = R.string.keywords_double_tap_power_wallet_action

    override val purpose: Int
        get() = R.string.gesture_double_power_tap_wallet_purpose

    override fun isEnabled(context: Context) =
        DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(context) &&
            QuickAccessWalletClient.create(context).isWalletServiceAvailable

    override fun isAvailable(context: Context): Boolean {
        if (!DoubleTapPowerSettingsUtils.isMultiTargetDoubleTapPowerButtonGestureAvailable(context))
            return false
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    companion object {
        const val KEY = "gesture_double_power_tap_wallet"
    }
}
// LINT.ThenChange(DoubleTapPowerForCameraPreferenceController.java,
//                 DoubleTapPowerForWalletPreferenceController.java)
