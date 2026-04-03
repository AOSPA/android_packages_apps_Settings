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
import android.os.OutcomeReceiver
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
import android.telephony.satellite.EnableRequestAttributes
import android.telephony.satellite.EnableResponse
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteManager.SatelliteException
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.TelephonyTogglePreferenceController
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteUtils
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.widget.MainSwitchPreference

/** Controller for the main switch on the satellite settings screen. */
class SatelliteSettingMainSwitchController(context: Context, key: String) :
    TelephonyTogglePreferenceController(context, key) {

    private lateinit var mSwitchPreference: MainSwitchPreference
    private var mSatelliteManager: SatelliteManager? = null
    private var mIsSatelliteEnabled = false
    private var mNtnConnectType = CARRIER_ROAMING_NTN_CONNECT_MANUAL

    fun init(subId: Int, satelliteManager: SatelliteManager?) {
        mSubId = subId
        val repository =
            FeatureFactory.featureFactory.telephonyFeatureProvider.satelliteSettingsRepository
        mNtnConnectType = repository.getSatelliteNtnConnectType(mSubId)
        mSatelliteManager = satelliteManager
        if (SatelliteUtils.isLteBasedNtnSupported(mSubId)) {
            requestIsEnabled()
        }
    }

    override fun getAvailabilityStatus(subId: Int): Int {
        if (!Flags.enableSatelliteToggle()) {
            return CONDITIONALLY_UNAVAILABLE
        }

        // Only display the toggle if LTE NTN is supported.
        return if (SatelliteUtils.isLteBasedNtnSupported(subId)) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mSwitchPreference = screen.findPreference(preferenceKey)!!
        // Sync visual state in case mIsSatelliteEnabled was updated before this method ran.
        mSwitchPreference.isChecked = mIsSatelliteEnabled
    }

    override fun isChecked(): Boolean {
        return mIsSatelliteEnabled
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (mSatelliteManager == null) {
            Log.e(TAG, "SatelliteManager is null, cannot toggle.")
            return false
        }

        // Optimistic Update: Provide instant feedback to the user
        mIsSatelliteEnabled = isChecked

        // Build attributes using the required USER reason code
        val attributes =
            EnableRequestAttributes.Builder(isChecked)
                .setSatelliteEnablementRequestReason(
                    SatelliteManager.SATELLITE_ENABLEMENT_REQUEST_REASON_USER
                )
                .setConnectType(mNtnConnectType)
                .build()

        // Trigger the modem state change
        mSatelliteManager?.requestEnabled(mSubId, attributes, mContext.mainExecutor) { result ->
            if (result != SatelliteManager.SATELLITE_RESULT_SUCCESS) {
                Log.e(TAG, "Failed to set enabled=$isChecked, error=$result")

                // Revert state on failure
                mIsSatelliteEnabled = !isChecked
                if (::mSwitchPreference.isInitialized) {
                    mSwitchPreference.isChecked = mIsSatelliteEnabled
                    // Inform the user of the failure
                    Toast.makeText(mContext, R.string.satellite_toggle_error, Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Log.d(TAG, "Successfully set satellite enabled state to $isChecked")
            }
        }
        return true
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        if (SatelliteUtils.isLteBasedNtnSupported(mSubId)) {
            requestIsEnabled()
        }
    }

    private fun requestIsEnabled() {
        if (mSatelliteManager == null) {
            Log.e(TAG, "SatelliteManager is null.")
            if (::mSwitchPreference.isInitialized) {
                mSwitchPreference.isEnabled = false
            }
            return
        }

        Log.d(TAG, "requestIsEnabled for subId $mSubId, connectType: $mNtnConnectType")
        mSatelliteManager?.requestIsEnabled(
            mSubId,
            mNtnConnectType,
            mContext.mainExecutor,
            object : OutcomeReceiver<EnableResponse, SatelliteException> {
                override fun onResult(result: EnableResponse) {
                    mIsSatelliteEnabled = result.isEnabled()
                    if (::mSwitchPreference.isInitialized) {
                        mSwitchPreference.isChecked = mIsSatelliteEnabled
                        mSwitchPreference.isEnabled = true
                    }
                    Log.d(TAG, "Current satellite state for subId $mSubId: $mIsSatelliteEnabled")
                }

                override fun onError(error: SatelliteException) {
                    Log.e(TAG, "Failed to request enabled state for subId $mSubId", error)
                    // We keep the switch enabled to allow the user to try toggling it again (manual
                    // retry)
                }
            },
        )
    }

    companion object {
        private const val TAG = "SatelliteSettingMainSwitchController"
    }
}
