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
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
    private int mSecurityType;
    private boolean isVendorDualApSupported;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();

        isVendorDualApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);

        updatePreferenceEntries(config);
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mBandIndex = SoftApConfiguration.BAND_2GHZ;
            Log.d(TAG, "Updating band index to BAND_2GHZ because no config");
        } else if (is5GhzBandSupported()) {
            mBandIndex = validateSelection(config);
            Log.d(TAG, "Updating band index to " + mBandIndex);
        } else {
            mWifiManager.setSoftApConfiguration(
                    new SoftApConfiguration.Builder(config).setBand(SoftApConfiguration.BAND_2GHZ)
                        .build());
            mBandIndex = SoftApConfiguration.BAND_2GHZ;
            Log.d(TAG, "5Ghz not supported, updating band index to 2GHz");
        }
        ListPreference preference =
                (ListPreference) mPreference;
        preference.setEntries(mBandSummaries);
        preference.setEntryValues(mBandEntries);

        if (!is5GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setEnabled(true);
            preference.setValue(Integer.toString(config.getBand()));
            preference.setSummary(getConfigSummary());
        }
    }

    String getConfigSummary() {
        switch (mBandIndex) {
            case SoftApConfiguration.BAND_2GHZ:
                return mBandSummaries[0];
            case SoftApConfiguration.BAND_5GHZ:
                return mBandSummaries[1];
            case SoftApConfiguration.BAND_DUAL:
                return mBandSummaries[2];
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
        mBandIndex = validateSelection(Integer.parseInt((String) newValue));
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        preference.setSummary(getConfigSummary());
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    private int validateSelection(SoftApConfiguration config) {
        if (config.getBand() == SoftApConfiguration.BAND_DUAL
                && config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_OWE) {
            config = new SoftApConfiguration.Builder(config).setBand(
                SoftApConfiguration.BAND_2GHZ).build();
            mWifiManager.setSoftApConfiguration(config);
            Log.d(TAG, "Dual band not supported for OWE security, updating band index to " + mBandIndex);
        }

        return validateSelection(config.getBand());
    }

    private int validateSelection(int band) {
        // unsupported states:
        // 1: BAND_5GHZ only - include 2GHZ since some of countries doesn't support 5G hotspot
        // 2: no 5 GHZ support means we can't have BAND_5GHZ - default to 2GHZ
        if (SoftApConfiguration.BAND_5GHZ == band) {
            if (!is5GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
            return SoftApConfiguration.BAND_5GHZ | SoftApConfiguration.BAND_2GHZ;
        }

        return band;
    }

    public void updatePreferenceEntries(SoftApConfiguration config) {
        mSecurityType = (config == null ? SoftApConfiguration.SECURITY_TYPE_OPEN : config.getSecurityType());
        Log.d(TAG, "updating band preferences.");
        updatePreferenceEntries();
     }

    @VisibleForTesting
    void updatePreferenceEntries() {
        Resources res = mContext.getResources();
        int entriesRes = R.array.wifi_ap_band;
        int summariesRes = R.array.wifi_ap_band_summary;
        if (isVendorDualApSupported && mSecurityType != SoftApConfiguration.SECURITY_TYPE_OWE) {
            // change the list option if AP+AP is supproted and selected security type is not OWE
            entriesRes = R.array.wifi_ap_band_vendor_config_full;
            summariesRes = R.array.wifi_ap_band_vendor_summary_full;
        }
        mBandEntries = res.getStringArray(entriesRes);
        mBandSummaries = res.getStringArray(summariesRes);
    }

    private boolean is5GhzBandSupported() {
        final String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.is5GHzBandSupported() || countryCode == null) {
            return false;
        }
        return true;
    }

    public int getBandIndex() {
        return mBandIndex;
    }

    public boolean isVendorDualApSupported() {
        return isVendorDualApSupported;
    }

    public boolean isBandEntriesHasDualband() {
        if (mBandEntries == null)
            return false;

        for (int i = 0 ; i < mBandEntries.length; i++) {
            if (Integer.parseInt(mBandEntries[i]) == SoftApConfiguration.BAND_DUAL)
                return true;
        }

        return false;
    }
}
