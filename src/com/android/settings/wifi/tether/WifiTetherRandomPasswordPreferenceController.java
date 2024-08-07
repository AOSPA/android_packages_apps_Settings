package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import com.android.settings.R;
import java.util.Random;

public class WifiTetherRandomPasswordPreferenceController extends WifiTetherBasePreferenceController {

    private static final String PREF_KEY = "wifi_tether_random_password";
    private String mOriginalPassword;
    private Random mRandom;

    public WifiTetherRandomPasswordPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mRandom = new Random();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        final SwitchPreference preference = (SwitchPreference) mPreference;
        if (preference == null) {
            return;
        }

        preference.setChecked(false);
        preference.setSummary(R.string.wifi_hotspot_random_password_summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isChecked = (Boolean) newValue;
        if (isChecked) {
            enableRandomPassword();
        } else {
            disableRandomPassword();
        }
        return true;
    }

    private void enableRandomPassword() {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        mOriginalPassword = config.getPassphrase();
        String randomPassword = generateRandomPassword();
        SoftApConfiguration newConfig = new SoftApConfiguration.Builder(config)
                .setPassphrase(randomPassword, config.getSecurityType())
                .build();
        mWifiManager.setSoftApConfiguration(newConfig);
        ((SwitchPreference) mPreference).setSummary(
                mContext.getString(R.string.wifi_hotspot_random_password_enabled, randomPassword));
    }

    private void disableRandomPassword() {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newConfig = new SoftApConfiguration.Builder(config)
                .setPassphrase(mOriginalPassword, config.getSecurityType())
                .build();
        mWifiManager.setSoftApConfiguration(newConfig);
        ((SwitchPreference) mPreference).setSummary(R.string.wifi_hotspot_random_password_summary);
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 9; i++) {
            sb.append(mRandom.nextInt(10));
        }
        return sb.toString();
    }

    public void onHotspotStopped() {
        if (((SwitchPreference) mPreference).isChecked()) {
            disableRandomPassword();
            ((SwitchPreference) mPreference).setChecked(false);
        }
    }
}
