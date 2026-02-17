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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.settings.flags.Flags
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.network.telephony.TelephonyTogglePreferenceController
import com.android.settingslib.widget.MainSwitchPreference

/**
 * Controller for the main switch on the satellite settings screen.
 */
class SatelliteSettingMainSwitchController(context: Context, key: String) :
    TelephonyTogglePreferenceController(context, key) {

    private var mCarrierConfigs: PersistableBundle = PersistableBundle.EMPTY
    private lateinit var mSwitchPreference: MainSwitchPreference

    fun init(subId: Int, carrierConfigs: PersistableBundle) {
        mSubId = subId
        mCarrierConfigs = carrierConfigs
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        if (!Flags.enableSatelliteToggle()) {
            return CONDITIONALLY_UNAVAILABLE
        }
        val ntnConnectType = mCarrierConfigs.getInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
        )

        return if (ntnConnectType != CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mSwitchPreference = screen.findPreference(preferenceKey)!!
    }

    override fun isChecked(): Boolean {
        // TODO: Call SatelliteManager to get the real value.
        return true
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        // TODO: Call SatelliteManager to set the value.
        Log.d(TAG, "onSwitchChanged: isChecked=$isChecked")
        return true
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        // TODO: Call SatelliteManager to get the value and set the preference.
        preference.isEnabled = true
    }

    companion object {
        private const val TAG = "SatelliteSettingMainSwitchController"
    }
}
