/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.annotation.NonNull;
import android.content.Context;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for logic pertaining to the security type of Wi-Fi tethering.
 */
public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController
        implements WifiManager.SoftApCallback {

    private static final String PREF_KEY = "wifi_tether_security";

    private Map<Integer, String> mSecurityMap = new LinkedHashMap<Integer, String>();
    private int mSecurityValue;
    @VisibleForTesting
    boolean mIsWpa3Supported = true;
    boolean mIsOweSapSupported = true;
    boolean mIsDualSapSupported = false;
    private String[] securityNames;
    private String[] securityValues;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        securityNames = mContext.getResources().getStringArray(
                R.array.wifi_tether_security);
        securityValues = mContext.getResources().getStringArray(
                R.array.wifi_tether_security_values);
        for (int i = 0; i < securityNames.length; i++) {
            mSecurityMap.put(Integer.parseInt(securityValues[i]), securityNames[i]);
        }
        mWifiManager.registerSoftApCallback(context.getMainExecutor(), this);
        mIsDualSapSupported = mWifiManager.isBridgedApConcurrencySupported();
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        // The mPreference will be ready when the fragment calls displayPreference(). Since the
        // capability of WPA3 hotspot callback will update the preference list here, add null point
        // checking to avoid the mPreference is not ready when the fragment is loading for settings
        // keyword searching only.
        if (mPreference == null) {
            return;
        }
        final ListPreference preference = (ListPreference) mPreference;
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        int defaultSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;

        for (int i = 0; i < securityNames.length; i++) {
            mSecurityMap.put(Integer.parseInt(securityValues[i]), securityNames[i]);
        }

        preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
        preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));

        if (config.getBand() == (SoftApConfiguration.BAND_6GHZ | SoftApConfiguration.BAND_2GHZ)
                && mSecurityMap.keySet().removeIf(
                key -> key < SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
            defaultSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
        }
        // If the device does not support WPA3 /OWE then remove the WPA3 /OWE options.
        if (!mIsWpa3Supported && mSecurityMap.keySet()
                .removeIf(key -> key > SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
        } else if (!(mIsDualSapSupported && mIsOweSapSupported) && mSecurityMap.keySet()
                .removeIf(key -> key > SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
        }

        final int securityType = mWifiManager.getSoftApConfiguration().getSecurityType();
        mSecurityValue = mSecurityMap.get(securityType) != null
                ? securityType : defaultSecurityType;

        preference.setSummary(mSecurityMap.get(mSecurityValue));
        preference.setValue(String.valueOf(mSecurityValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSecurityValue = Integer.parseInt((String) newValue);
        preference.setSummary(mSecurityMap.get(mSecurityValue));
        if (mListener != null) {
            mListener.onTetherConfigUpdated(this);
        }
        return true;
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        final boolean isWpa3Supported =
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_SAE);
        if (!isWpa3Supported) {
            Log.i(PREF_KEY, "WPA3 SAE is not supported on this device");
        }

        final boolean isOweSupported =
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_OWE);
        if (!isOweSupported) {
            Log.i(PREF_KEY, "OWE not supported.");
        }

        if (mIsWpa3Supported != isWpa3Supported
                || mIsOweSapSupported != isOweSupported) {
            mIsWpa3Supported = isWpa3Supported;
            mIsOweSapSupported = isOweSupported;
            updateDisplay();
        }
        mWifiManager.unregisterSoftApCallback(this);
    }

    public int getSecurityType() {
        return mSecurityValue;
    }

    public boolean isOweDualSapSupported() {
        return mIsDualSapSupported && mIsOweSapSupported;
    }
}
