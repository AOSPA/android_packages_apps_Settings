/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.UserManager;
import android.security.Flags;
import android.security.advancedprotection.AdvancedProtectionManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link TogglePreferenceController} that controls whether the Wi-Fi Auto-connect feature should be
 * enabled.
 */
public class WifiAutoConnectPreferenceController2 extends TogglePreferenceController
        implements Preference.OnPreferenceClickListener {

    private static final String KEY_AUTO_CONNECT = "auto_connect";
    public static final int REQUEST_CODE_AUTOCONNECT_OVERRIDE = 555;
    private final AdvancedProtectionManager mAdvancedProtectionManager;
    private final UserManager mUserManager;
    private WifiEntry mWifiEntry;
    private WifiConfiguration mWifiConfig;
    private PreferenceRefreshCallback mRefreshCallback;
    private Preference mPreference;
    private Fragment mParentFragment;

    public WifiAutoConnectPreferenceController2(Context context) {
        super(context, KEY_AUTO_CONNECT);
        mAdvancedProtectionManager = context.getSystemService(AdvancedProtectionManager.class);
        mUserManager = context.getSystemService(UserManager.class);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
    }

    public void setWifiConfig(WifiConfiguration wifiConfig) {
        mWifiConfig = wifiConfig;
    }

    /**
     * Sets the parent Fragment, which is required for launching the dialog
     * for a result.
     *
     * @param fragment The parent Fragment (e.g., WifiNetworkDetailsFragment)
     */
    public void setParentFragment(Fragment fragment) {
        mParentFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiEntry.canSetAutoJoinEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        preference.setSelectable(true);
        preference.setSummary(R.string.wifi_auto_connect_summary);
        preference.setOnPreferenceClickListener(null);

        if (preference instanceof SwitchPreferenceCompat) {
            SwitchPreferenceCompat switchPref = (SwitchPreferenceCompat) preference;

            if (isAapmActiveAndInsecureNetwork()) {
                boolean isOverridden = mWifiConfig.isAutoJoinInAdvancedProtectionModeEnabled();

                preference.setEnabled(true);
                preference.setOnPreferenceClickListener(this);
                switchPref.setChecked(isOverridden);
                mPreference = preference;
            } else {
                switchPref.setChecked(isChecked());

                boolean enabled = true;
                if (com.android.settings.connectivity.Flags.wifiMultiuser()
                        && mWifiConfig != null) {
                    if (mWifiConfig.creatorUid != mContext.getUserId()) {
                        if (mUserManager != null && mUserManager.getUserCount() > 1) {
                            enabled = false;
                        }
                    }
                }
                preference.setEnabled(enabled);
            }
        }
    }

    @Override
    public boolean isChecked() {
        if (isAapmActiveAndInsecureNetwork()) {
            return mWifiConfig.isAutoJoinInAdvancedProtectionModeEnabled();
        }
        return mWifiEntry.isAutoJoinEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isAapmActiveAndInsecureNetwork()) {
            return false;
        }

        mWifiEntry.setAutoJoinEnabled(isChecked);
        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (!isAapmActiveAndInsecureNetwork()) {
            return false;
        }

        if (this.isChecked()) {
            mWifiConfig.setAutoJoinInAdvancedProtectionModeEnabled(false);
            mWifiEntry.setAutoJoinEnabled(false);

            if (mRefreshCallback != null) {
                mRefreshCallback.onPreferenceStateChange(preference);
            }
            return true;
        } else {
            showAapmSupportDialog();
            return true;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }

    private boolean isAapmActiveAndInsecureNetwork() {
        if (mWifiConfig == null) return false;

        boolean isAapmFeatureActive = Flags.aapmFeatureDisableInsecureWifiAutojoin()
                && mAdvancedProtectionManager != null
                && mAdvancedProtectionManager.isAdvancedProtectionEnabled();

        boolean isInsecureNetwork = !mWifiConfig.isAutoJoinInAdvancedProtectionModeEnabled();

        return isAapmFeatureActive && isInsecureNetwork;
    }

    private void showAapmSupportDialog() {
        final Intent supportIntent = getAapmSupportIntent();
        supportIntent.setFlags(0);

        mParentFragment.startActivityForResult(supportIntent, REQUEST_CODE_AUTOCONNECT_OVERRIDE);
    }

    /**
     * Helper method to wrap the static call for testability.
     */
    @VisibleForTesting
    Intent getAapmSupportIntent() {
        return AdvancedProtectionManager.createSupportIntent(
                AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSECURE_WIFI_AUTOJOIN,
                AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_UNKNOWN);
    }

    /**
     * Public method to be called by the parent Fragment's onActivityResult
     * when a result is received from the dialog.
     */
    public void handleDialogResult(int requestCode, int resultCode) {
        if (requestCode != REQUEST_CODE_AUTOCONNECT_OVERRIDE) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            // User clicked "Turn on anyway"
            mWifiConfig.setAutoJoinInAdvancedProtectionModeEnabled(true);
            mWifiEntry.setAutoJoinEnabled(true);

            if (mRefreshCallback != null && mPreference != null) {
                mRefreshCallback.onPreferenceStateChange(mPreference);
            }
        }
    }

    /**
     * Callback interface for requesting the parent fragment/activity to refresh the state of a
     * preference.
     */
    public interface PreferenceRefreshCallback {
        /** Called in the parent fragment */
        void onPreferenceStateChange(Preference preference);
    }

    public void setRefreshCallback(PreferenceRefreshCallback callback) {
        mRefreshCallback = callback;
    }
}
