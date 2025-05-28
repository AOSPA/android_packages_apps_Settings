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
package com.android.settings.network.telephony.satellite

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.network.telephony.TelephonyBasePreferenceController

/** A controller to control How is work paragraph.  */
class SatelliteSettingIndicatorController(context: Context?, preferenceKey: String?) :
    TelephonyBasePreferenceController(context, preferenceKey) {
    private var mCarrierConfigs: PersistableBundle? = null

    fun init(subId: Int, carrierConfigs: PersistableBundle) {
        mSubId = subId
        mCarrierConfigs = carrierConfigs
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        updateHowItWorksContent(
            screen,
            SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext, mSubId)
        )
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        return AVAILABLE
    }

    @VisibleForTesting
    fun updateHowItWorksContent(screen: PreferenceScreen, isSatelliteEligible: Boolean) {
        /* Composes "How it works" section, which guides how users can use satellite messaging, when
           satellite messaging is included in user's mobile plan, or it'll will be grey out. */
        if (!isSatelliteEligible) {
            val category =
                screen.findPreference<PreferenceCategory?>(PREF_KEY_CATEGORY_HOW_IT_WORKS)
            if (category == null) return
            category.isEnabled = false
            category.shouldDisableView = true
        }
        if (!this.isCarrierRoamingNtnConnectedTypeManual) {
            return
        }
        val connectionGuide: Preference = screen.findPreference(KEY_SATELLITE_CONNECTION_GUIDE)!!
        connectionGuide.setTitle(R.string.title_satellite_connection_guide_for_manual_type)
        connectionGuide.setSummary(R.string.summary_satellite_connection_guide_for_manual_type)

        val supportedService: Preference = screen.findPreference(KEY_SUPPORTED_SERVICE)!!
        supportedService.setTitle(R.string.title_supported_service_for_manual_type)
        supportedService.setSummary(R.string.summary_supported_service_for_manual_type)

    }

    private val isCarrierRoamingNtnConnectedTypeManual: Boolean
        get() = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL == mCarrierConfigs!!.getInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
        )

    companion object {
        @VisibleForTesting
        const val PREF_KEY_CATEGORY_HOW_IT_WORKS: String = "key_category_how_it_works"

        @VisibleForTesting
        const val KEY_SATELLITE_CONNECTION_GUIDE: String = "key_satellite_connection_guide"

        @VisibleForTesting
        const val KEY_SUPPORTED_SERVICE: String = "key_supported_service"
    }
}
