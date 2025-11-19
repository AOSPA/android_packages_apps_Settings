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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
import android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT
import android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.util.Log

/** Utility class for satellite/telephony-related functionalities. */
object SatelliteUtils {
    /**
     * Returns true if LTE-based NTN is supported by the device.
     *
     * Checks if LTE-based NTN is supported for any of the active subscription IDs.
     */
    fun isLteBasedNtnSupportedByDevice(context: Context): Boolean {
        val subscriptionManager: SubscriptionManager? =
            context.getSystemService(SubscriptionManager::class.java)
        if (subscriptionManager == null) {
            Log.w(TAG, "SubscriptionManager is null")
            return false
        }
        val activeSubscriptionInfoList =
            subscriptionManager.getActiveSubscriptionInfoList() ?: emptyList()
        for (subscriptionInfo in activeSubscriptionInfoList) {
            Log.i(
                TAG,
                "Checking LTE-based NTN support for ${subscriptionInfo.getDisplayName()}, SubId: ${subscriptionInfo.subscriptionId}",
            )
            if (isLteBasedNtnSupportedByCarrier(context, subscriptionInfo.subscriptionId)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if LTE-based NTN is supported for the carrier.
     *
     * If the attach restriction reasons are empty and Satellite Attach is supported in the carrier
     * config, it means that LTE-based NTN is supported.
     */
    fun isLteBasedNtnSupportedByCarrier(context: Context, activeSubId: Int): Boolean {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null")
            return false
        }

        if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.w(TAG, "ActiveSubId is invalid")
            return false
        }

        val configBundle = fetchCarrierConfigData(context, activeSubId)
        val isSatelliteAttachSupported =
            configBundle.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false)
        // TODO(b/434793872): May need to handle extra logic for if connect type is
        // CARRIER_ROAMING_NTN_CONNECT_HYBRID. In certain cases, we may want to show NBIOT landing
        // page instead of LTE landing page.
        // If connect type is not manual, then automatic NTN connect is supported.
        val isCarrierRoamingNtnConnectTypeAutomatic =
            configBundle.getInt(
                KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                /* default= */ CARRIER_ROAMING_NTN_CONNECT_MANUAL,
            ) != CARRIER_ROAMING_NTN_CONNECT_MANUAL
        val hasNoAttachRestrictionReasons =
            satelliteManager.getAttachRestrictionReasonsForCarrier(activeSubId).isEmpty()
        Log.i(
            TAG,
            "isLteBasedNtnSupported: ${hasNoAttachRestrictionReasons && isSatelliteAttachSupported && isCarrierRoamingNtnConnectTypeAutomatic} " +
                "[hasNoAttachRestrictionReasons=$hasNoAttachRestrictionReasons, " +
                "isSatelliteAttachSupported=$isSatelliteAttachSupported, " +
                "isCarrierRoamingNtnConnectTypeAutomatic=$isCarrierRoamingNtnConnectTypeAutomatic]",
        )
        return hasNoAttachRestrictionReasons &&
            isSatelliteAttachSupported &&
            isCarrierRoamingNtnConnectTypeAutomatic
    }

    private fun fetchCarrierConfigData(context: Context, subId: Int): PersistableBundle {
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)
        var bundle = CarrierConfigManager.getDefaultConfig()

        if (carrierConfigManager == null) {
            Log.e(TAG, "CarrierConfigManager is null, returning default config.")
            return bundle
        }

        try {
            val fetchedBundle =
                carrierConfigManager.getConfigForSubId(
                    subId,
                    KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                    KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                )
            if (!fetchedBundle.isEmpty) {
                bundle = fetchedBundle
            } else {
                Log.e(TAG, "Fetched bundle is null or empty, using default config.")
            }
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "Exception fetching carrier config: $exception")
        }

        return bundle
    }

    private const val TAG = "SatelliteUtils"
}
