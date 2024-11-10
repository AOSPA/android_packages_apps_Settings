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

import static android.net.ConnectivityManager.TETHERING_WIFI;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLING;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.VisibleForTesting;

import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller for logic pertaining to switch Wi-Fi tethering.
 */
public class WifiTetherSwitchBarController implements
        LifecycleObserver, OnStart, OnStop, DataSaverBackend.Listener, OnCheckedChangeListener {

    private static final String TAG = "WifiTetherSBC";

    private final Context mContext;
    private final SettingsMainSwitchBar mSwitchBar;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;
    private final SoftApCallback mSoftApCallback = new SoftApCallback();

    @VisibleForTesting
    boolean mIsSwitchBusy;

    @VisibleForTesting
    DataSaverBackend mDataSaverBackend;
    @VisibleForTesting
    final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    Log.e(TAG, "Failed to start Wi-Fi Tethering.");
                    handleWifiApStateChanged(mWifiManager.getWifiApState());
                }
            };

    WifiTetherSwitchBarController(Context context, SettingsMainSwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mDataSaverBackend = new DataSaverBackend(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mSwitchBar.setChecked(mWifiManager.getWifiApState() == WIFI_AP_STATE_ENABLED);
        updateWifiSwitch();
    }

    @Override
    public void onStart() {
        mDataSaverBackend.addListener(this);
        mSwitchBar.addOnSwitchChangeListener(this);

        // use callback to replace broadcast
        mWifiManager.registerSoftApCallback(mContext.getApplicationContext().getMainExecutor(),
                mSoftApCallback);

        handleWifiApStateChanged(mWifiManager.getWifiApState());
    }

    @Override
    public void onStop() {
        mDataSaverBackend.remListener(this);
        mWifiManager.unregisterSoftApCallback(mSoftApCallback);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Filter out inappropriate callbacks when switch is busy.
        if (mIsSwitchBusy) return;

        if (isChecked) {
            startTether();
        } else {
            stopTether();
        }
    }

    void stopTether() {
        if (!isWifiApActivated()) return;

        mIsSwitchBusy = true;
        mConnectivityManager.stopTethering(TETHERING_WIFI);
    }

    void startTether() {
        if (isWifiApActivated()) return;

        mIsSwitchBusy = true;
        mConnectivityManager.startTethering(TETHERING_WIFI, true /* showProvisioningUi */,
                mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private boolean isWifiApActivated() {
        final int wifiApState = mWifiManager.getWifiApState();
        if (wifiApState == WIFI_AP_STATE_ENABLED || wifiApState == WIFI_AP_STATE_ENABLING) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    void handleWifiApStateChanged(int state) {
        if (state == WIFI_AP_STATE_ENABLING || state == WIFI_AP_STATE_DISABLING) return;

        final boolean shouldBeChecked = (state == WIFI_AP_STATE_ENABLED);
        if (mSwitchBar.isChecked() != shouldBeChecked) {
            mSwitchBar.setChecked(shouldBeChecked);
        }
        updateWifiSwitch();
    }

    private void updateWifiSwitch() {
        mSwitchBar.setEnabled(!mDataSaverBackend.isDataSaverEnabled());
        mIsSwitchBusy = false;
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        updateWifiSwitch();
    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
        // we don't care, since we just want to read the value
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
        // we don't care, since we just want to read the value
    }

    private class SoftApCallback implements WifiManager.SoftApCallback {
        @Override
        public void onStateChanged(int state, int failureReason) {
            Log.d(TAG, "onStateChanged(), state:" + state + ", failureReason:" + failureReason);
            handleWifiApStateChanged(state);
        }
    }
}
