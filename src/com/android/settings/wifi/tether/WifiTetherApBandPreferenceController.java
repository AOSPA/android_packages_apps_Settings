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
import java.util.ArrayList;
import java.util.Arrays;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

    // Predefined Band 5Ghz / 6Ghz combinations.
    // 1- 5Ghz/6Ghz prefer (default): prefers 5Ghz/6Ghz, but supports 2Ghz also.
    // 2- 5Ghz/6Ghz only: strict 5Ghz/6Ghz band.
    private static final int BAND_5GHZ = SoftApConfiguration.BAND_5GHZ | SoftApConfiguration.BAND_2GHZ;
    private static final int BAND_6GHZ = SoftApConfiguration.BAND_6GHZ | SoftApConfiguration.BAND_2GHZ;

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
    private int mSecurityType;
    private boolean isVendorDualApSupported;
    private final Context mContext;
    private boolean m5GHzSupported;
    private boolean m6GHzSupported;
    private String mCountryCode;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        mContext = context;

        isVendorDualApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);

        syncBandSupportAndCountryCode();
        updatePreferenceEntries(config);
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        syncBandSupportAndCountryCode();
        if (config == null) {
            mBandIndex = SoftApConfiguration.BAND_2GHZ;
            Log.d(TAG, "Updating band index to BAND_2GHZ because no config");
        } else if (is5GhzBandSupported() || is6GhzBandSupported()) {
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

        if (!is5GhzBandSupported() && !is6GhzBandSupported()) {
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
            case BAND_5GHZ:
            case BAND_6GHZ:
            case SoftApConfiguration.BAND_DUAL:
                final ListPreference preference = (ListPreference) mPreference;
                return mBandSummaries[preference.findIndexOfValue(String.valueOf(mBandIndex))];
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
        syncBandSupportAndCountryCode();
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
        // 3: no 6 GHZ support means we can't have AP_BAND_6GHZ - default to 2GHZ
        if (band == BAND_5GHZ) {
            if (!is5GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
            // fallthrough to return BAND_5GHZ
        } else if (band == BAND_6GHZ) {
            if (!is6GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
            // fallthrough to return BAND_6GHZ
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
        // change the list option if AP+AP is supproted and selected security type is not OWE
        if (isVendorDualApSupported && mSecurityType != SoftApConfiguration.SECURITY_TYPE_OWE &&
            (is5GhzBandSupported())) {
            bandEntries.add(String.valueOf(SoftApConfiguration.BAND_DUAL));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_vendor_dual_band));
        }
        mBandEntries = bandEntries.toArray(new String[bandEntries.size()]);
        mBandSummaries = bandSummaries.toArray(new String[bandSummaries.size()]);
    }

    // This is used to reduce IPC calls to framework.
    private void syncBandSupportAndCountryCode() {
        m5GHzSupported = mWifiManager.is5GHzBandSupported();
        m6GHzSupported = mWifiManager.is6GHzBandSupported();
        mCountryCode   = mWifiManager.getCountryCode();
    }

    private boolean is5GhzBandSupported() {
        if (!m5GHzSupported || mCountryCode == null) {
            return false;
        }
        return true;
    }

    private boolean is6GhzBandSupported() {
        if (!m6GHzSupported || mCountryCode == null) {
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
