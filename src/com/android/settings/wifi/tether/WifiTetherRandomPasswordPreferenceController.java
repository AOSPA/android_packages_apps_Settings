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
        setNewPassword(randomPassword);
        ((SwitchPreference) mPreference).setSummary(
                mContext.getString(R.string.wifi_hotspot_random_password_enabled, randomPassword));
    }

    private void disableRandomPassword() {
        if (mOriginalPassword != null) {
            setNewPassword(mOriginalPassword);
            mOriginalPassword = null;
        }
        ((SwitchPreference) mPreference).setSummary(R.string.wifi_hotspot_random_password_summary);
    }

    private void setNewPassword(String password) {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newConfig = new SoftApConfiguration.Builder(config)
                .setPassphrase(password, config.getSecurityType())
                .build();
        mWifiManager.setSoftApConfiguration(newConfig);
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(9);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(mRandom.nextInt(chars.length())));
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
