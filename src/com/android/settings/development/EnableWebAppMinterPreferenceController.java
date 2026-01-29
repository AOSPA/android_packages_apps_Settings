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

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

public class EnableWebAppMinterPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "EnableWebAppMinterCtrl";

    @VisibleForTesting static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting static final int SETTING_VALUE_ON = 1;

    public EnableWebAppMinterPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        if (!com.android.webapp.flags.Flags.enableWebAppServiceV2()) {
            Log.d(TAG, "Flag enableWebAppServiceV2 is disabled");
            return false;
        }

        final Intent intent = new Intent("com.android.webapp.IWebAppService");
        final List<ResolveInfo> services =
                mContext.getPackageManager()
                        .queryIntentServices(
                                intent,
                                PackageManager.MATCH_SYSTEM_ONLY | PackageManager.GET_META_DATA);
        if (services.isEmpty()) {
            Log.d(TAG, "No system services found checking for " + intent);
            return false;
        }

        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (si == null || si.metaData == null) {
                Log.d(
                        TAG,
                        "Found service "
                                + (si != null ? si.name : "null")
                                + " but missing metadata");
                continue;
            }
            if (si.metaData.getBoolean("com.android.webapp.developer_option_visible")) {
                Log.d(TAG, "Found service " + si.name + " with visible=true");
                return true;
            } else {
                Log.d(TAG, "Found service " + si.name + " but visible=false");
            }
        }
        Log.d(TAG, "No services had visible=true");
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return Settings.Global.ENABLE_WEBAPP_MINTER;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_WEBAPP_MINTER,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_WEBAPP_MINTER,
                        SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_WEBAPP_MINTER,
                SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
