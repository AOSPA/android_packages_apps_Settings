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
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.overlay.FeatureFactory

/** Utility class for satellite/telephony-related functionalities. */
object SatelliteUtils {
    /**
     * Returns true if LTE-based NTN is supported by the carrier associated with the given subId.
     */
    @JvmStatic
    fun isLteBasedNtnSupported(context: Context, subId: Int): Boolean {
        Log.i(TAG, "Checking LTE-based NTN support for SubId: $subId")
        return isLteBasedNtnSupportedByCarrier(context, subId)
    }

    /** Returns true if LTE-based NTN is supported by any active subscription on the device. */
    @JvmStatic
    fun isLteBasedNtnSupportedByAnySub(context: Context): Boolean {
        val subscriptionManager: SubscriptionManager? =
            context.getSystemService(SubscriptionManager::class.java)
        if (subscriptionManager == null) {
            Log.w(TAG, "SubscriptionManager is null")
            return false
        }
        val activeSubscriptionInfoList =
            subscriptionManager.getActiveSubscriptionInfoList() ?: emptyList()
        for (subscriptionInfo in activeSubscriptionInfoList) {
            if (isLteBasedNtnSupported(context, subscriptionInfo.subscriptionId)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if Carrier Roaming NTN is supported for the carrier.
     *
     * This checks if satellite attach is supported by carrier config. It does NOT check the
     * connection type (LTE vs NB-IoT).
     *
     * "Support" is defined as inherent capability REGARDLESS of current availability or status.
     *
     * @param activeSubId The active subscription ID to check carrier support for.
     */
    @JvmStatic
    fun isCarrierRoamingNtnSupported(activeSubId: Int): Boolean {
        if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.w(TAG, "ActiveSubId is invalid")
            return false
        }

        val telephonyFeatureProvider = FeatureFactory.featureFactory.telephonyFeatureProvider
        if (telephonyFeatureProvider == null) {
            Log.w(TAG, "TelephonyFeatureProvider is null")
            return false
        }
        val repository = telephonyFeatureProvider.satelliteSettingsRepository
        val isSatelliteAttachSupported = repository.isSatelliteAttachSupported(activeSubId)

        logd {
            "isCarrierRoamingNtnSupported: $isSatelliteAttachSupported, activeSubId: $activeSubId"
        }
        return isSatelliteAttachSupported
    }

    /** Returns true if LTE-based NTN is supported for the given subId. */
    @JvmStatic
    fun isLteBasedNtnSupported(subId: Int): Boolean {
        val telephonyFeatureProvider = FeatureFactory.featureFactory.telephonyFeatureProvider
        if (telephonyFeatureProvider == null) {
            Log.w(TAG, "TelephonyFeatureProvider is null")
            return false
        }
        val repository = telephonyFeatureProvider.satelliteSettingsRepository
        if (!repository.isSatelliteAttachSupported(subId)) {
            return false
        }

        // TODO(b/434793872): May need to handle extra logic for if connect type is
        // CARRIER_ROAMING_NTN_CONNECT_HYBRID. In certain cases, we may want to show NBIOT landing
        // page instead of LTE landing page.
        // If connect type is not manual, then automatic NTN connect is supported.
        val isCarrierRoamingNtnConnectTypeAutomatic =
            repository.getSatelliteNtnConnectType(subId) != CARRIER_ROAMING_NTN_CONNECT_MANUAL

        logd { "isLteBasedNtnSupported: $isCarrierRoamingNtnConnectTypeAutomatic" }
        return isCarrierRoamingNtnConnectTypeAutomatic
    }

    /**
     * Returns true if LTE-based NTN is supported for the carrier.
     *
     * If carrier roaming NTN is supported by carrier, attach restriction reasons are empty and
     * carrier roaming NTN connect type is automatic, it means that LTE-based NTN is supported.
     */
    @JvmStatic
    fun isLteBasedNtnSupportedByCarrier(context: Context, activeSubId: Int): Boolean {
        if (activeSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.w(TAG, "ActiveSubId is invalid")
            return false
        }
        return isLteBasedNtnSupported(activeSubId)
    }

    @VisibleForTesting
    var satelliteAppsRepositoryProvider: (Context) -> SatelliteAppsRepository =
        { context -> SatelliteAppsRepository(context) }

    /**
     * Returns the appropriate intent for satellite entry point.
     *
     * If the current data support mode is unconstrained, this returns the intent for the detailed
     * satellite settings page. Otherwise, it returns the intent for the satellite landing page.
     */
    @JvmStatic
    fun resolveSatelliteSettingsIntent(context: Context): Intent {
        val subId = SubscriptionManager.getActiveDataSubscriptionId()
        val dataSupportMode =
            SatelliteStateRepository.getInstance(context).getSatelliteDataSupportMode(subId)

        if (dataSupportMode == SatelliteManager.SATELLITE_DATA_SUPPORT_UNCONSTRAINED) {
            val isCarrierRoamingNtnSupported = isCarrierRoamingNtnSupported(subId)
            val settingsIntent =
                satelliteAppsRepositoryProvider(context)
                    .getSettingsIntent(isCarrierRoamingNtnSupported)
            if (settingsIntent != null) {
                return settingsIntent
            }
        }

        return Intent(context, SatelliteLandingPageActivity::class.java)
    }

    /**
     * Returns the Intent to view the Satellite SOS supported countries Google Help Center Article
     */
    fun getSatelliteCoverageIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(SATELLITE_SOS_COVERAGE_URL))
    }

    /** Returns the Intent to change the default SMS application. */
    fun getDefaultSmsAppIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    }

    /** Returns the Intent to view the location source settings. */
    fun getLocationSourceSettingsIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    /** Returns the Intent for the carrier satellite settings. */
    fun getCarrierSettingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_SATELLITE_SETTING)
        intent.putExtra(":settings:show_fragment_as_subsetting", true)
        intent.putExtra("sub_id", SubscriptionManager.getActiveDataSubscriptionId())
        return intent
    }

    private const val TAG = "SatelliteUtils"
    private const val SATELLITE_SOS_COVERAGE_URL =
        "https://support.google.com/pixelphone?p=satellitesos"

    private inline fun logd(message: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message())
        }
    }
}
