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

import android.content.Context;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
import android.content.res.Resources;
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.overlay.FeatureFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for logic pertaining to the security type of Wi-Fi tethering.
 */
public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController
        implements WifiManager.SoftApCallback {

    private static final String PREF_KEY = "wifi_tether_security";
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
    private static final String WIFI_RES_PACKAGE = "com.android.wifi.resources";

    private Context mWifiResContext;
    private Resources mWifiRes;
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM

    private Map<Integer, String> mSecurityMap = new LinkedHashMap<Integer, String>();
    private int mSecurityValue;
    @VisibleForTesting
    boolean mIsWpa3Supported = true;
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
    boolean mIsOweSapSupported = true;
    boolean mIsDualSapSupported = false;
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
    private String[] securityNames;
    private String[] securityValues;
// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
    @VisibleForTesting
    boolean mShouldHidePreference;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        // If the Wi-Fi Hotspot Speed Feature available, then hide this controller.
        mShouldHidePreference = FeatureFactory.getFeatureFactory()
                .getWifiFeatureProvider().getWifiHotspotRepository().isSpeedFeatureAvailable();
        Log.d(TAG, "shouldHidePreference():" + mShouldHidePreference);
        if (mShouldHidePreference) {
            return;
        }
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
        securityNames = mContext.getResources().getStringArray(
// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
                R.array.wifi_tether_security);
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
        securityValues = mContext.getResources().getStringArray(
// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
                R.array.wifi_tether_security_values);
        for (int i = 0; i < securityNames.length; i++) {
            mSecurityMap.put(Integer.parseInt(securityValues[i]), securityNames[i]);
        }
        mWifiManager.registerSoftApCallback(context.getMainExecutor(), this);
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        mIsDualSapSupported = mWifiManager.isBridgedApConcurrencySupported();
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM

        try {
            mWifiResContext = mContext.createPackageContext(WIFI_RES_PACKAGE,
                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
// QTI_BEGIN: 2023-01-15: WLAN: Wifi: do not throw exception when wifi res context not found
            Log.d(PREF_KEY, "exception in createPackageContext: " + e);
            mWifiRes = null;
            mWifiResContext = null;
// QTI_END: 2023-01-15: WLAN: Wifi: do not throw exception when wifi res context not found
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
        }
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
// QTI_BEGIN: 2023-01-15: WLAN: Wifi: do not throw exception when wifi res context not found
        if (mWifiResContext != null)
            mWifiRes = mWifiResContext.getResources();
// QTI_END: 2023-01-15: WLAN: Wifi: do not throw exception when wifi res context not found
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
    }

    private int getWifiResId(String category, String name) {
        if (mWifiRes == null) {
            Log.e(PREF_KEY, "no WIFI resources, fail to get " + category + "." + name);
            return -1;
        }
        return mWifiRes.getIdentifier(name, category, WIFI_RES_PACKAGE);
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
    }

    @Override
    public boolean isAvailable() {
        if (mShouldHidePreference) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        if (mShouldHidePreference) {
            return;
        }
        // The mPreference will be ready when the fragment calls displayPreference(). Since the
        // capability of WPA3 hotspot callback will update the preference list here, add null point
        // checking to avoid the mPreference is not ready when the fragment is loading for settings
        // keyword searching only.
        if (mPreference == null) {
            return;
        }
        final ListPreference preference = (ListPreference) mPreference;
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        int defaultSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;

        for (int i = 0; i < securityNames.length; i++) {
            mSecurityMap.put(Integer.parseInt(securityValues[i]), securityNames[i]);
        }

        preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
        preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));

// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
        if ((config.getBand() & SoftApConfiguration.BAND_6GHZ) != 0
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
                && mSecurityMap.keySet().removeIf(
                key -> key < SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
            defaultSecurityType = SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
        }
// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        // If the device does not support WPA3 /OWE then remove the WPA3 /OWE options.
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        if (!mIsWpa3Supported && mSecurityMap.keySet()
                .removeIf(key -> key > SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
                    .toArray(CharSequence[]::new));
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        } else if (!(mIsDualSapSupported && mIsOweSapSupported) && mSecurityMap.keySet()
                .removeIf(key -> key > SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)) {
            preference.setEntries(mSecurityMap.values().stream().toArray(CharSequence[]::new));
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
            preference.setEntryValues(mSecurityMap.keySet().stream().map(i -> Integer.toString(i))
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
                    .toArray(CharSequence[]::new));
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        }

// QTI_BEGIN: 2022-10-06: WLAN: HotSpot: Use OWE only mode with 6GHz band option
        int securityType = mWifiManager.getSoftApConfiguration().getSecurityType();

        /* Since UI has single option for OWE and OWE Transition mode, lets map OWE to
         * OWE transition option for display purpose */
        if (securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_OWE)
             securityType = SoftApConfiguration.SECURITY_TYPE_WPA3_OWE_TRANSITION;

// QTI_END: 2022-10-06: WLAN: HotSpot: Use OWE only mode with 6GHz band option
        mSecurityValue = mSecurityMap.get(securityType) != null
// QTI_BEGIN: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band
                ? securityType : defaultSecurityType;
// QTI_END: 2021-08-18: WLAN: Remove none, wpa2-personal and wpa2/wpa3-personal security for 6GHz Band

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
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.

        final boolean isOweSupported =
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
// QTI_BEGIN: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_OWE)
// QTI_END: 2022-10-06: WLAN: Tethering: Check OWE vendor overlay support to allow OWE AKM
                || (mWifiRes != null && mWifiRes.getBoolean(getWifiResId("bool", "config_vendor_wifi_softap_owe_supported")));
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
        if (!isOweSupported) {
            Log.i(PREF_KEY, "OWE not supported.");
        }

        if (mIsWpa3Supported != isWpa3Supported
                || mIsOweSapSupported != isOweSupported) {
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
            mIsWpa3Supported = isWpa3Supported;
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
            mIsOweSapSupported = isOweSupported;
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
            updateDisplay();
        }
        mWifiManager.unregisterSoftApCallback(this);
    }

    public int getSecurityType() {
        return mSecurityValue;
    }
// QTI_BEGIN: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.

    public boolean isOweDualSapSupported() {
        return mIsDualSapSupported && mIsOweSapSupported;
    }
// QTI_END: 2021-07-13: WLAN: Softap: Add support for OWE Security for Softap Configuration.
}
