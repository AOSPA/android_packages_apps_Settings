/*
 * Copyright (C) 2022 Paranoid Android
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;

import java.util.List;

public class ScreenOffUdfpsPreferenceController extends TogglePreferenceController {

    private final FingerprintManager mFingerprintManager;
    private List<FingerprintSensorPropertiesInternal> mSensorProperties;

    private Preference mPreference;

    public ScreenOffUdfpsPreferenceController(Context context, String key) {
        super(context, key);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);

        if (mFingerprintManager != null) {
            mSensorProperties = mFingerprintManager.getSensorPropertiesInternal();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isUdfps()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mFingerprintManager != null && (!mFingerprintManager.isHardwareDetected()
                || !mFingerprintManager.hasEnrolledFingerprints())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportsScreenOffUdfps)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SCREEN_OFF_UDFPS_ENABLED, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SCREEN_OFF_UDFPS_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean isUdfps() {
        return mSensorProperties.stream().anyMatch(prop -> prop.isAnyUdfpsType());
    }
}
