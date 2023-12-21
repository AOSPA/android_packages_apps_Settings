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
import android.annotation.NonNull;
import android.net.wifi.WifiManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApInfo;
import static android.net.wifi.ScanResult.WIFI_BAND_6_GHZ;
import static android.net.wifi.WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS;
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController
       implements WifiManager.SoftApCallback {

    private static final String TAG = "WifiTetherApBandPref";
    private static final String PREF_KEY = "wifi_tether_network_ap_band";

    private static final int BAND_5GHZ = SoftApConfiguration.BAND_5GHZ | SoftApConfiguration.BAND_2GHZ;
    private static final int BAND_6GHZ = SoftApConfiguration.BAND_6GHZ | SoftApConfiguration.BAND_2GHZ;

    private String[] mBandEntries;
    private String[] mBandSummaries;
    private int mBandIndex;
    private final Context mContext;
    private SoftApCapability mSoftApCapability;
    private Set<Integer> mCurrentBands;
    private Map<Integer, Boolean> mAllowedBands;
    private boolean mVerboseLoggingEnabled;

    // Dual Band (2G + 5G)
    public static final int BAND_BOTH_2G_5G = 1 << 4;

    public WifiTetherApBandPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mContext = context;
        mWifiManager.registerSoftApCallback(context.getMainExecutor(), this);
        mCurrentBands = new HashSet<>();
        mAllowedBands = new HashMap<>();
        mVerboseLoggingEnabled = mWifiManager.isVerboseLoggingEnabled();
        updatePreferenceEntries();
    }

    @Override
    public void updateDisplay() {
        if (mCurrentBands.size() == 2) {
            mBandIndex = BAND_BOTH_2G_5G;
            Log.d(TAG, "Updating band index to Both based on current band size");
        } else if (mCurrentBands.size() == 1) {
            Iterator<Integer> iterator = mCurrentBands.iterator();
            mBandIndex= iterator.next();
            Log.d(TAG, "Updating band index based on mCurrentBands to :" + mBandIndex);
        } else {
            final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
            if (config == null) {
                mBandIndex = SoftApConfiguration.BAND_2GHZ;
                Log.d(TAG, "Updating band index to BAND_2GHZ because no config");
            } else if (is5GhzBandSupported() || is6GhzBandSupported()) {
                if (config.getChannels().size() == 2) {
                    if (config.getSecurityType() ==
                        SoftApConfiguration.SECURITY_TYPE_WPA3_OWE_TRANSITION
                            || config.getSecurityType() ==
                        SoftApConfiguration.SECURITY_TYPE_WPA3_OWE) {
                        mWifiManager.setSoftApConfiguration(
                                new SoftApConfiguration.Builder(config)
                                    .setBand(SoftApConfiguration.BAND_2GHZ)
                                    .build());
                        mBandIndex = SoftApConfiguration.BAND_2GHZ;
                        Log.d(TAG, "Dual band not supported with OWE, updating band index to 2GHz");
                    } else {
                        mBandIndex = BAND_BOTH_2G_5G;
                    }
                } else {
                    mBandIndex = validateSelection(config.getBand());
                }
                Log.d(TAG, "Updating band index to " + mBandIndex);
            } else {
                mWifiManager.setSoftApConfiguration(
                        new SoftApConfiguration.Builder(config).setBand(SoftApConfiguration.BAND_2GHZ)
                            .build());
                mBandIndex = SoftApConfiguration.BAND_2GHZ;
                Log.d(TAG, "5Ghz/6Ghz not supported, updating band index to 2GHz");
            }
        }

        ListPreference preference =
                (ListPreference) mPreference;
        preference.setEntries(mBandSummaries);
        preference.setEntryValues(mBandEntries);
        preference.setValue(Integer.toString(mBandIndex));

        if (!is5GhzBandSupported() && !is6GhzBandSupported()) {
            preference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            preference.setSummary(getConfigSummary());
        }
    }

    String getConfigSummary() {
        switch (mBandIndex) {
            case SoftApConfiguration.BAND_2GHZ:
                return mBandSummaries[0];
            case BAND_5GHZ:
            case BAND_6GHZ:
            case BAND_BOTH_2G_5G:
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
        mBandIndex = validateSelection(Integer.parseInt((String) newValue));
        Log.d(TAG, "Band preference changed, updating band index to " + mBandIndex);
        mCurrentBands.clear();
        preference.setSummary(getConfigSummary());
        mListener.onTetherConfigUpdated(this);
        return true;
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
        } else if ((band & SoftApConfiguration.BAND_6GHZ) != 0) {
            if (!is6GhzBandSupported()) {
                return SoftApConfiguration.BAND_2GHZ;
            }
            return BAND_6GHZ;
        }

        return band;
    }

    @VisibleForTesting
    void updatePreferenceEntries() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
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
        // Add Dual AP bands
        if (is5GhzBandSupported()
                && (mWifiManager.isBridgedApConcurrencySupported() || isVendorLegacyDualBandSupported())
                && (config != null)
                && (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_WPA3_OWE_TRANSITION
                    && config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_WPA3_OWE)) {
            bandEntries.add(String.valueOf(BAND_BOTH_2G_5G));
            bandSummaries.add(mContext.getString(R.string.wifi_ap_choose_vendor_dual_band));
        }

        mBandEntries = bandEntries.toArray(new String[bandEntries.size()]);
        mBandSummaries = bandSummaries.toArray(new String[bandSummaries.size()]);
    }


    private boolean is5GhzBandSupported() {
        if (mSoftApCapability != null &&
            mSoftApCapability.areFeaturesSupported(SoftApCapability
                                                   .SOFTAP_FEATURE_BAND_5G_SUPPORTED) &&
            mSoftApCapability.getSupportedChannelList(SoftApConfiguration.BAND_5GHZ).length > 0) {
            return true;
        } else {
             try {
                 if (mAllowedBands.get(WIFI_BAND_5_GHZ_WITH_DFS) != null
                     && mAllowedBands.get(WIFI_BAND_5_GHZ_WITH_DFS)) {
                         return true;
                 } else {
                         mAllowedBands.put(WIFI_BAND_5_GHZ_WITH_DFS,
                         mWifiManager.getAllowedChannels(WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP).size() > 0);
                         return true;
                 }
             } catch (Exception e) {
                 Log.e(TAG, "5Ghz Band Not Supported");
                 if (mVerboseLoggingEnabled) {
                     e.printStackTrace();
                 }
             }
        }
        return false;
    }

    private boolean is6GhzBandSupported() {
        if (mSoftApCapability != null &&
            mSoftApCapability.areFeaturesSupported(SoftApCapability
                                                   .SOFTAP_FEATURE_BAND_6G_SUPPORTED) &&
            mSoftApCapability.getSupportedChannelList(SoftApConfiguration.BAND_6GHZ).length > 0) {
            return true;
        }
        else {
             try {
                 if (mAllowedBands.get(WIFI_BAND_6_GHZ) != null
                     && mAllowedBands.get(WIFI_BAND_6_GHZ)) {
                         return true;
                 } else {
                         mAllowedBands.put(WIFI_BAND_6_GHZ,
                         mWifiManager.getAllowedChannels(WIFI_BAND_6_GHZ, OP_MODE_SAP).size() > 0);
                         return true;
                 }
             } catch (Exception e) {
                 Log.e(TAG, "6Ghz Band Not Supported");
                 if (mVerboseLoggingEnabled) {
                     e.printStackTrace();
                 }
             }
        }
        return false;
    }

    public int getBandIndex() {
        return mBandIndex;
    }

    private boolean isVendorLegacyDualBandSupported() {
        return mContext.getResources().getBoolean(
                     com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        if(mVerboseLoggingEnabled) {
           Log.d(TAG, "onCapabilityChanged:"+ softApCapability);
        }
        mSoftApCapability = softApCapability;
        updatePreferenceEntries();
        updateDisplay();
    }

    @Override
    public void onInfoChanged(@NonNull List<SoftApInfo> softApInfoList) {
            if(mVerboseLoggingEnabled) {
               Log.d(TAG, "onInfoChanged");
            }
            mCurrentBands.clear();
            for (SoftApInfo softApInfo : softApInfoList) {
                int frequency = softApInfo.getFrequency();
                classifyAndAddFreq(frequency);
            }
            updatePreferenceEntries();
            updateDisplay();
    }

    private void classifyAndAddFreq(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            // 2.4 GHz range
            mCurrentBands.add(SoftApConfiguration.BAND_2GHZ);
        } else if (frequency >= 4900 && frequency < 5900) {
            // 5 GHz range
            mCurrentBands.add(BAND_5GHZ);
        } else if (frequency > 5900 && frequency < 7115) {
            // 6 GHz range or other
            mCurrentBands.add(BAND_6GHZ);
        }
    }
}
