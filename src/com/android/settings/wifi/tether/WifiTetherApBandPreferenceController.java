/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.SoftApConfiguration;
// QTI_BEGIN: 2023-07-04: WLAN: Tether-Settings: Check 6Ghz SAP support from Wifi HAL
import static android.net.wifi.ScanResult.WIFI_BAND_6_GHZ;
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;
// QTI_END: 2023-07-04: WLAN: Tether-Settings: Check 6Ghz SAP support from Wifi HAL
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
import java.util.ArrayList;
import java.util.Arrays;
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
import com.android.settings.overlay.FeatureFactory;
// QTI_END: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
    // Predefined Band 5Ghz / 6Ghz combinations.
    // 1- 5Ghz/6Ghz prefer (default): prefers 5Ghz/6Ghz, but supports 2Ghz also.
    // 2- 5Ghz/6Ghz only: strict 5Ghz/6Ghz band.
    private static final int BAND_5GHZ = SoftApConfiguration.BAND_5GHZ | SoftApConfiguration.BAND_2GHZ;
    private static final int BAND_6GHZ = SoftApConfiguration.BAND_6GHZ | SoftApConfiguration.BAND_2GHZ;

// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
    private final Context mContext;
    private boolean m5GHzSupported;
    private boolean m6GHzSupported;
    private String mCountryCode;
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
    boolean mShouldHidePreference;
// QTI_END: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled

// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
    // Dual Band (2G + 5G)
    public static final int BAND_BOTH_2G_5G = 1 << 4;

// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        mContext = context;

// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
        // If the Wi-Fi Hotspot Speed Feature available, then hide this controller.
        mShouldHidePreference = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository().isSpeedFeatureAvailable();
        if (mShouldHidePreference) {
            return;
        }

// QTI_END: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        syncBandSupportAndCountryCode();
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        updatePreferenceEntries();
    }

// QTI_BEGIN: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
    @Override
    public boolean isAvailable() {
        if (mShouldHidePreference) {
            return false;
        }
        return super.isAvailable();
    }

// QTI_END: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
    @Override
    public void updateDisplay() {
// QTI_BEGIN: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
        if (mShouldHidePreference) {
            return;
        }
// QTI_END: 2024-01-09: WLAN: Disable AP Band and Security preference when Wi-fi Hotspot speed feature is enabled
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        syncBandSupportAndCountryCode();
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        if (config == null) {
            mBandIndex = SoftApConfiguration.BAND_2GHZ;
            Log.d(TAG, "Updating band index to BAND_2GHZ because no config");
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        } else if (is5GhzBandSupported() || is6GhzBandSupported()) {
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2024-05-22: WLAN: wifi: remove dependency on "framework-wifi-vendor-hide-access-defaults"
            if (config.getChannels().size() == 2) {
// QTI_END: 2024-05-22: WLAN: wifi: remove dependency on "framework-wifi-vendor-hide-access-defaults"
// QTI_BEGIN: 2022-09-14: WLAN: SAP: fix Dual Band display issue in Enhanced Open security
                if (config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_WPA3_OWE_TRANSITION
                        || config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_WPA3_OWE) {
// QTI_END: 2022-09-14: WLAN: SAP: fix Dual Band display issue in Enhanced Open security
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
                    mWifiManager.setSoftApConfiguration(
                            new SoftApConfiguration.Builder(config)
                                .setBand(SoftApConfiguration.BAND_2GHZ)
                                .build());
                    mBandIndex = SoftApConfiguration.BAND_2GHZ;
                    Log.d(TAG, "Dual band not supported with OWE, updating band index to 2GHz");
                } else {
                    mBandIndex = BAND_BOTH_2G_5G;
                }
            } else
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
                mBandIndex = validateSelection(config.getBand());
// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
            Log.d(TAG, "Updating band index to " + mBandIndex);
        } else {
            mWifiManager.setSoftApConfiguration(
                    new SoftApConfiguration.Builder(config).setBand(SoftApConfiguration.BAND_2GHZ)
                        .build());
            mBandIndex = SoftApConfiguration.BAND_2GHZ;
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            Log.d(TAG, "5Ghz/6Ghz not supported, updating band index to 2GHz");
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        }
        ListPreference preference =
                (ListPreference) mPreference;
// QTI_BEGIN: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
        if (preference == null) {
            return;
        }
// QTI_END: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
        preference.setEntries(mBandSummaries);
        preference.setEntryValues(mBandEntries);

// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        if (!is5GhzBandSupported() && !is6GhzBandSupported()) {
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
            preference.setValue(Integer.toString(mBandIndex));
// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
            preference.setSummary(getConfigSummary());
        }
    }

    String getConfigSummary() {
        switch (mBandIndex) {
            case SoftApConfiguration.BAND_2GHZ:
                return mBandSummaries[0];
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            case BAND_5GHZ:
            case BAND_6GHZ:
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
            case BAND_BOTH_2G_5G:
// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
                final ListPreference preference = (ListPreference) mPreference;
                return mBandSummaries[preference.findIndexOfValue(String.valueOf(mBandIndex))];
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            default:
                return mContext.getString(R.string.wifi_ap_prefer_5G);
        }
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        syncBandSupportAndCountryCode();
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        mBandIndex = validateSelection(Integer.parseInt((String) newValue));
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        preference.setSummary(getConfigSummary());
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    private int validateSelection(int band) {
        // unsupported states:
        // 1: BAND_5GHZ only - include 2GHZ since some of countries doesn't support 5G hotspot
        // 2: no 5 GHZ support means we can't have BAND_5GHZ - default to 2GHZ
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        // 3: no 6 GHZ support means we can't have AP_BAND_6GHZ - default to 2GHZ
        if (band == BAND_5GHZ) {
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            if (!is5GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            // fallthrough to return BAND_5GHZ
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2022-06-02: WLAN: wifi(Settings): Validate AP band whether 6GHz is included
        } else if ((band & SoftApConfiguration.BAND_6GHZ) != 0) {
// QTI_END: 2022-06-02: WLAN: wifi(Settings): Validate AP band whether 6GHz is included
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            if (!is6GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2022-06-02: WLAN: wifi(Settings): Validate AP band whether 6GHz is included
            return BAND_6GHZ;
// QTI_END: 2022-06-02: WLAN: wifi(Settings): Validate AP band whether 6GHz is included
        }

        return band;
    }

    @VisibleForTesting
    void updatePreferenceEntries() {
// QTI_BEGIN: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
        if (mShouldHidePreference) {
            return;
        }
// QTI_END: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        Resources res = mContext.getResources();
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        ArrayList<String> bandEntries =  new ArrayList<String>();
        ArrayList<String> bandSummaries =  new ArrayList<String>();
        // Add 2GHz band
        bandEntries.add(String.valueOf(SoftApConfiguration.BAND_2GHZ));
        bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_2G));
        // Add 5GHz band
        if (is5GhzBandSupported()) {
            bandEntries.add(String.valueOf(BAND_5GHZ));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_prefer_5G));
        }
        // Add 6GHz band
        if (is6GhzBandSupported()) {
            bandEntries.add(String.valueOf(BAND_6GHZ));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_prefer_6G));
        }
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
        // Add Dual AP bands
// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
// QTI_BEGIN: 2021-08-05: WLAN: Dual-Softap: Use separate overlay for legacy targets.
        if (is5GhzBandSupported()
                && (mWifiManager.isBridgedApConcurrencySupported() || isVendorLegacyDualBandSupported())
// QTI_END: 2021-08-05: WLAN: Dual-Softap: Use separate overlay for legacy targets.
// QTI_BEGIN: 2022-09-14: WLAN: SAP: fix Dual Band display issue in Enhanced Open security
                && (config != null)
                && (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_WPA3_OWE_TRANSITION
                    && config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_WPA3_OWE)) {
// QTI_END: 2022-09-14: WLAN: SAP: fix Dual Band display issue in Enhanced Open security
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
            bandEntries.add(String.valueOf(BAND_BOTH_2G_5G));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_vendor_dual_band));
        }

// QTI_END: 2021-07-13: WLAN: Softap: Add support for Dual Band (2G+5G) configuration from UI
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        mBandEntries = bandEntries.toArray(new String[bandEntries.size()]);
        mBandSummaries = bandSummaries.toArray(new String[bandSummaries.size()]);
    }

    // This is used to reduce IPC calls to framework.
    private void syncBandSupportAndCountryCode() {
        m5GHzSupported = mWifiManager.is5GHzBandSupported();
        m6GHzSupported = mWifiManager.is6GHzBandSupported();
        mCountryCode   = mWifiManager.getCountryCode();
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2020-08-31: WLAN: wifi(settings): Conditionally sync band capability and country
    }

// QTI_END: 2020-08-31: WLAN: wifi(settings): Conditionally sync band capability and country
    private boolean is5GhzBandSupported() {
// QTI_BEGIN: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
        if (!m5GHzSupported || mCountryCode == null) {
            return false;
        }
        return true;
    }

    private boolean is6GhzBandSupported() {
// QTI_END: 2021-05-18: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
// QTI_BEGIN: 2023-07-04: WLAN: Tether-Settings: Check 6Ghz SAP support from Wifi HAL
        if (!m6GHzSupported || mCountryCode == null ) {
            return false;
        }
        try {
            if (mWifiManager.getAllowedChannels(WIFI_BAND_6_GHZ, OP_MODE_SAP).isEmpty()) {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "6Ghz Band Not Supported ");
// QTI_END: 2023-07-04: WLAN: Tether-Settings: Check 6Ghz SAP support from Wifi HAL
// QTI_BEGIN: 2020-07-28: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
            return false;
        }
        return true;
    }

// QTI_END: 2020-07-28: WLAN: Wi-Fi: Add 6GHz band support in Soft-AP UI advanced options
    public int getBandIndex() {
// QTI_BEGIN: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
        if (mShouldHidePreference) {
            return 0;
        }
// QTI_END: 2024-01-22: Android_UI: Avoid accessing AP Band preference controller when Wi-Fi Hotspot Speed Feature is enabled
        return mBandIndex;
    }
// QTI_BEGIN: 2021-08-05: WLAN: Dual-Softap: Use separate overlay for legacy targets.

    private boolean isVendorLegacyDualBandSupported() {
        return mContext.getResources().getBoolean(
                     com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);
    }
// QTI_END: 2021-08-05: WLAN: Dual-Softap: Use separate overlay for legacy targets.
}
