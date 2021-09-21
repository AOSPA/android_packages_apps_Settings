package com.android.settings.wifi.tether;

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

import android.content.Context;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import java.util.ArrayList;

public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "wifi_tether_security";
    private static final String TAG = "WifiTetherSecurityPreferenceController";
    private static final int BIT_DEVICE_DBS_CAPABLE = 13; /* QCA_WLAN_VENDOR_FEATURE_CONCURRENT_BAND_SESSIONS */

    private String[] mSecurityEntries;
    private String[] mSecurityValues;
    private int mSecurityValue;
    private boolean mSecurityCapaFetched;
    private boolean mDualSoftApSupported;
    private boolean mSaeSapSupprted;
    private boolean mOweSapSupprted;
    private boolean mConcurrentBandSupported;
    final Context mContext;

    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() {
        @Override
        public void onCapabilityChanged(SoftApCapability capability) {
            if (mSecurityCapaFetched)
                return;

            ArrayList<String> securityEntries =  new ArrayList<String>();
            ArrayList<String> securityValues =  new ArrayList<String>();

            mSecurityCapaFetched = true;

            if (capability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_SAE))
                mSaeSapSupprted = true;

            if (capability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_OWE))
                mOweSapSupprted = true;

            if (mSaeSapSupprted) {
                // Add SAE security type
                securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION));
                securityEntries.add(mContext.getString(R.string.wifi_security_sae));
            }

            if (mOweSapSupprted && mDualSoftApSupported && isConcurrentBandSupported()) {
                // Add OWE security type
                securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_OWE));
                securityEntries.add(mContext.getString(R.string.wifi_security_owe));
            }
            // Add WPA2-PSK security type
            securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK));
            securityEntries.add(mContext.getString(R.string.wifi_security_wpa2));
            // Add open security type
            securityValues.add(String.valueOf(SoftApConfiguration.SECURITY_TYPE_OPEN));
            securityEntries.add(mContext.getString(R.string.wifi_security_none));

            mSecurityEntries = securityEntries.toArray(new String[securityEntries.size()]);
            mSecurityValues = securityValues.toArray(new String[securityValues.size()]);

            updateDisplay();
            Log.i(TAG, "Updated supported SoftAp AKMs");
        }

    };

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mContext = context;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mDualSoftApSupported = context.getResources().getBoolean(
            com.android.internal.R.bool.config_wifi_dual_sap_mode_enabled);

        Log.i(TAG, "Register SoftAp callback");
        wifiManager.registerSoftApCallback(new HandlerExecutor(new Handler()), mSoftApCallback);
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        } else if (config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_OPEN;
        } else if (mOweSapSupprted && mDualSoftApSupported
                   && config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_OWE) {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_OWE;
        } else if (mSaeSapSupprted
                   && config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION) {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;
        } else {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
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
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    public int getSecurityType() {
        return mSecurityValue;
    }

    private String getSummaryForSecurityType(int securityType) {
        final ListPreference preference = (ListPreference) mPreference;
        int securityEntryIndex = preference.findIndexOfValue(String.valueOf(securityType));

        return securityEntryIndex < 0 ? "" : mSecurityEntries[securityEntryIndex];
    }

    public boolean isOweSapSupported() {
        return mOweSapSupprted;
    }

    private boolean isConcurrentBandSupported() {
        if (mConcurrentBandSupported) return true;

        /* isConcurrentBandSupported gives mask of supported features.
         * BIT_DEVICE_DBS_CAPABLE indicates Concurrent band support */
        int concurrentBandFlags = mWifiManager.isConcurrentBandSupported();
        mConcurrentBandSupported = (concurrentBandFlags > 0
                && ((concurrentBandFlags >> BIT_DEVICE_DBS_CAPABLE) & 1) > 0);

        return mConcurrentBandSupported;
    }
}
