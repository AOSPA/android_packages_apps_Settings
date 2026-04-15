// QTI_BEGIN: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.settings.location;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

// QTI_END: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
// QTI_BEGIN: 2019-06-03: Android_UI: Settings: set customized Agps UI unavailable by default
import com.android.settings.R;
// QTI_END: 2019-06-03: Android_UI: Settings: set customized Agps UI unavailable by default
// QTI_BEGIN: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
import com.android.settings.core.BasePreferenceController;


// QTI_END: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
public class AgpsPreferenceController extends LocationBasePreferenceController {
// QTI_BEGIN: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
    private static final String KEY_ASSISTED_GPS = "assisted_gps";

    private CheckBoxPreference mAgpsPreference;

// QTI_END: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
    public AgpsPreferenceController(Context context, String key) {
        super(context, key);
// QTI_BEGIN: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ASSISTED_GPS;
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
// QTI_END: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
// QTI_BEGIN: 2019-06-03: Android_UI: Settings: set customized Agps UI unavailable by default
        return mContext.getResources().getBoolean(R.bool.config_agps_enabled)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
// QTI_END: 2019-06-03: Android_UI: Settings: set customized Agps UI unavailable by default
// QTI_BEGIN: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mAgpsPreference =
                (CheckBoxPreference) screen.findPreference(KEY_ASSISTED_GPS);
    }

    @Override
    public void updateState(Preference preference) {
        if (mAgpsPreference != null) {
            mAgpsPreference.setChecked(Settings.Global.getInt(
                    mContext.getContentResolver(), Settings.Global.ASSISTED_GPS_ENABLED, 0) == 1);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_ASSISTED_GPS.equals(preference.getKey())) {
            final ContentResolver cr = mContext.getContentResolver();
            final boolean switchState = mAgpsPreference.isChecked();
            Settings.Global.putInt(cr, Settings.Global.ASSISTED_GPS_ENABLED,
                    switchState ? 1 : 0);
            return true;
        }
        return false;
    }
// QTI_END: 2019-01-03: Android_UI: Settings: Refactor Agps preference controller code

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {}
}
