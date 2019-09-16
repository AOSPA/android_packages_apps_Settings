package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import java.util.ArrayList;

public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "wifi_tether_security";

    private final String[] mSecurityEntries;
    private final String[] mSecurityValues;
    private int mSecurityValue;
    private boolean mWpa3SoftApSupported;
    private boolean mDualSoftApSupported;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        ArrayList<String> securityEntries =  new ArrayList<String>();
        ArrayList<String> securityValues =  new ArrayList<String>();

        mDualSoftApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);
        mWpa3SoftApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_wap3_sap_mode_enabled);

        // Add SAE security type
        if (mWpa3SoftApSupported) {
            securityValues.add(String.valueOf(WifiConfiguration.KeyMgmt.SAE));
            securityEntries.add(context.getString(R.string.wifi_security_sae));
        }
        // Add WPA2-PSK security type
        securityValues.add(String.valueOf(WifiConfiguration.KeyMgmt.WPA2_PSK));
        securityEntries.add(context.getString(R.string.wifi_security_wpa2));
        // Add OWE security type
        if (mWpa3SoftApSupported && mDualSoftApSupported) {
            securityValues.add(String.valueOf(WifiConfiguration.KeyMgmt.OWE));
            securityEntries.add(context.getString(R.string.wifi_security_owe));
        }
        // Add open security type
        securityValues.add(String.valueOf(WifiConfiguration.KeyMgmt.NONE));
        securityEntries.add(context.getString(R.string.wifi_security_none));

        mSecurityEntries = securityEntries.toArray(new String[securityEntries.size()]);
        mSecurityValues = securityValues.toArray(new String[securityValues.size()]);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        if (config == null) {
            mSecurityValue = WifiConfiguration.KeyMgmt.WPA2_PSK;
        } else if (config.getAuthType() == WifiConfiguration.KeyMgmt.NONE) {
            mSecurityValue = WifiConfiguration.KeyMgmt.NONE;
        } else if (mWpa3SoftApSupported && mDualSoftApSupported
                       && config.getAuthType() == WifiConfiguration.KeyMgmt.OWE) {
            mSecurityValue = WifiConfiguration.KeyMgmt.OWE;
        } else if (mWpa3SoftApSupported && config.getAuthType() == WifiConfiguration.KeyMgmt.SAE) {
            mSecurityValue = WifiConfiguration.KeyMgmt.SAE;
        } else {
            mSecurityValue = WifiConfiguration.KeyMgmt.WPA2_PSK;
        }

        final ListPreference preference = (ListPreference) mPreference;
        preference.setEntries(mSecurityEntries);
        preference.setEntryValues(mSecurityValues);
        preference.setSummary(getSummaryForSecurityType(mSecurityValue));
        preference.setValue(String.valueOf(mSecurityValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSecurityValue = Integer.parseInt((String) newValue);
        preference.setSummary(getSummaryForSecurityType(mSecurityValue));
        mListener.onTetherConfigUpdated();
        return true;
    }

    public int getSecurityType() {
        return mSecurityValue;
    }

    private String getSummaryForSecurityType(int securityType) {
        final ListPreference preference = (ListPreference) mPreference;
        int securityEntryIndex = preference.findIndexOfValue(String.valueOf(securityType));

        return mSecurityEntries[securityEntryIndex];
    }

    public boolean isWpa3Supported() {
        return mWpa3SoftApSupported;
    }
}
