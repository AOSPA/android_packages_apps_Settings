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

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import java.util.ArrayList;
import java.util.Arrays;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
    private int mSecurityType;
    private boolean isDualMode;
    private boolean isVendorDualApSupported;
    private final Context mContext;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mContext = context;
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        isDualMode = mWifiManager.isDualModeSupported();

        isVendorDualApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);

        updatePreferenceEntries(config);
    }

    @Override
    public void updateDisplay() {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        int tempBandIndex = mBandIndex;
        if (config == null) {
            mBandIndex = 0;
            Log.d(TAG, "Updating band index to 0 because no config");
        } else if (is5GhzBandSupported() || is6GhzBandSupported()) {
            mBandIndex = validateSelection(config);
            Log.d(TAG, "Updating band index to " + mBandIndex);
        } else {
            config.apBand = 0;
            mWifiManager.setWifiApConfiguration(config);
            mBandIndex = config.apBand;
            Log.d(TAG, "5Ghz not supported, updating band index to " + mBandIndex);
        }
        ListPreference preference =
                (ListPreference) mPreference;
        preference.setEntries(mBandSummaries);
        preference.setEntryValues(mBandEntries);

        if (preference.findIndexOfValue(String.valueOf(mBandIndex)) >= mBandEntries.length) {
            mBandIndex = tempBandIndex < mBandEntries.length ? tempBandIndex : 0;
        }

        if (!is5GhzBandSupported() && !is6GhzBandSupported()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setEnabled(true);
            preference.setValue(Integer.toString(config.apBand));
            preference.setSummary(getConfigSummary());
        }
    }

    String getConfigSummary() {
        if (mBandIndex == WifiConfiguration.AP_BAND_ANY) {
           return mContext.getString(R.string.wifi_ap_prefer_5G);
        }
        final ListPreference preference = (ListPreference) mPreference;
        return mBandSummaries[preference.findIndexOfValue(String.valueOf(mBandIndex))];
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mBandIndex = validateSelection(Integer.parseInt((String) newValue));
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        preference.setSummary(getConfigSummary());
        mListener.onTetherConfigUpdated();
        return true;
    }

    private int validateSelection(WifiConfiguration config) {
        if (config.apBand ==  WifiConfiguration.AP_BAND_DUAL
                && config.getAuthType() == WifiConfiguration.KeyMgmt.OWE) {
            config.apBand = 0;
            mWifiManager.setWifiApConfiguration(config);
            Log.d(TAG, "Dual band not supported for OWE security, updating band index to " + mBandIndex);
        }

        return validateSelection(config.apBand);
    }

    private int validateSelection(int band) {
        // Reset the band to 2.4 GHz if we get a weird config back to avoid a crash.
        final boolean isDualMode = mWifiManager.isDualModeSupported();

        // unsupported states:
        // 1: no dual mode means we can't have AP_BAND_ANY - default to 5GHZ
        // 2: no 5 GHZ support means we can't have AP_BAND_5GHZ - default to 2GHZ
        // 3: With Dual mode support we can't have AP_BAND_5GHZ or AP_BAND_6GHZ - default to ANY
        // 4: no 6 GHZ support means we can't have AP_BAND_6GHZ - default to 2GHZ
        if (!isDualMode && WifiConfiguration.AP_BAND_ANY == band) {
            return WifiConfiguration.AP_BAND_5GHZ;
        } else if (!is5GhzBandSupported() && WifiConfiguration.AP_BAND_5GHZ == band) {
            return WifiConfiguration.AP_BAND_2GHZ;
        } else if (isDualMode && (WifiConfiguration.AP_BAND_5GHZ == band ||
                   WifiConfiguration.AP_BAND_6GHZ == band)) {
            return WifiConfiguration.AP_BAND_ANY;
        } else if (!is6GhzBandSupported() && WifiConfiguration.AP_BAND_6GHZ == band) {
            return WifiConfiguration.AP_BAND_2GHZ;
        }

        return band;
    }

    public void updatePreferenceEntries(WifiConfiguration config) {
        mSecurityType = (config == null ? WifiConfiguration.KeyMgmt.NONE : config.getAuthType());
        Log.d(TAG, "updating band preferences.");
        updatePreferenceEntries();
     }

    @VisibleForTesting
    void updatePreferenceEntries() {
        Resources res = mContext.getResources();
        ArrayList<String> bandEntries =  new ArrayList<String>();
        ArrayList<String> bandSummaries =  new ArrayList<String>();
        // Add 2GHz band
        bandEntries.add(String.valueOf(WifiConfiguration.AP_BAND_2GHZ));
        bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_2G));
        // Add 5GHz band
        if (is5GhzBandSupported()) {
            bandEntries.add(String.valueOf(WifiConfiguration.AP_BAND_5GHZ));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_5G));
        }
        // Add 6GHz band
        if (is6GhzBandSupported()) {
            bandEntries.add(String.valueOf(WifiConfiguration.AP_BAND_6GHZ));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_6G));
        }
        // change the list options if this is a dual mode device
        if (isDualMode) {
            int entriesRes = R.array.wifi_ap_band_dual_mode;
            int summariesRes = R.array.wifi_ap_band_dual_mode_summary;
            bandEntries = new ArrayList<String>(Arrays.asList(res.getStringArray(entriesRes)));
            bandSummaries = new ArrayList<String>(Arrays.asList(res.getStringArray(summariesRes)));
            // change the list option if AP+AP is supproted and selected security type is not OWE
        } else if (isVendorDualApSupported && mSecurityType != WifiConfiguration.KeyMgmt.OWE && is5GhzBandSupported()) {
            bandEntries.add(String.valueOf(WifiConfiguration.AP_BAND_DUAL));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_vendor_both));
        }
        mBandEntries = bandEntries.toArray(new String[bandEntries.size()]);
        mBandSummaries = bandSummaries.toArray(new String[bandSummaries.size()]);
    }

    private boolean is5GhzBandSupported() {
        final String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            return false;
        }
        return true;
    }
    private boolean is6GhzBandSupported() {
        final String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.is6GHzBandSupported() || countryCode == null) {
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
            if (Integer.parseInt(mBandEntries[i]) == WifiConfiguration.AP_BAND_DUAL)
                return true;
        }

        return false;
    }
}
