/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.development.qstile;

import static com.android.settingslib.development.AbstractEnableAdbPreferenceController.ADB_SETTING_OFF;
import static com.android.settingslib.development.AbstractEnableAdbPreferenceController.ADB_SETTING_ON;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.development.AdbWirelessDebuggingPreferenceController;

/**
 * Tile to control the "Wireless debugging" developer setting
 */
public class AdbWirelessDebuggingDevelopmentTile extends DevelopmentTiles {
    private Context mContext;
    private KeyguardManager mKeyguardManager;
    private Toast mToast;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mSettingsObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refresh();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
        mToast = Toast.makeText(mContext,
                com.android.settingslib.R.string.adb_wireless_no_network_msg,
                Toast.LENGTH_LONG);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED), false,
                mSettingsObserver);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    protected boolean isEnabled() {
        return isAdbWifiEnabled();
    }

    @Override
    public void setIsEnabled(boolean isEnabled) {
        // Don't allow Wireless Debugging to be enabled from the lock screen.
        if (isEnabled && mKeyguardManager.isKeyguardLocked()) {
            return;
        }

        // Show error toast if not connected to Wi-Fi
        if (isEnabled && !AdbWirelessDebuggingPreferenceController.isWifiConnected(mContext)) {
            // Close quick shade
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            mToast.show();
        }

        writeAdbWifiSetting(isEnabled);
    }

    private boolean isAdbWifiEnabled() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_WIFI_ENABLED,
                ADB_SETTING_OFF) != ADB_SETTING_OFF;
    }

    protected void writeAdbWifiSetting(boolean enabled) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_WIFI_ENABLED,
                enabled ? ADB_SETTING_ON : ADB_SETTING_OFF);
    }
}
