/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.security;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import static android.provider.Settings.System.PROXIMITY_ON_WAKE;

public class ProximityOnWakePreferenceController extends TogglePreferenceController {

    private final boolean mSupported;
    private final int mDefaultValue;

    public ProximityOnWakePreferenceController(Context context, String key) {
        super(context, key);
        mSupported = context.getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        mDefaultValue = context.getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWakeEnabledByDefault) ? 1 : 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupported ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(), PROXIMITY_ON_WAKE,
                mDefaultValue) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), PROXIMITY_ON_WAKE,
                isChecked ? 1 : 0);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }
}
