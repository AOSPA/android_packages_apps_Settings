/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.FaceManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.overlay.FeatureFactory;

import com.android.internal.widget.LockPatternUtils;

public class ParanoidFaceStatusPreferenceController extends BiometricStatusPreferenceController {

    public static final String KEY_FACE_SETTINGS = "paranoid_face_settings";

    protected final LockPatternUtils mLockPatternUtils;

    public ParanoidFaceStatusPreferenceController(Context context) {
        this(context, KEY_FACE_SETTINGS);
    }

    public ParanoidFaceStatusPreferenceController(Context context, String key) {
        super(context, key);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider().getLockPatternUtils(context);
    }

    @Override
    protected boolean isDeviceSupported() {
        ParanoidFaceSenseConnector psf = ParanoidFaceSenseConnector.getInstance(mContext);
        return psf.isParanoidFaceSenseEnabled();
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        ParanoidFaceSenseConnector psf = ParanoidFaceSenseConnector.getInstance(mContext);
        return psf.hasEnrolledFaceSenseUsers();
    }

    @Override
    protected String getSummaryTextEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary_none);
    }

    @Override
    protected String getSettingsClassName() {
        return Settings.ParanoidFaceSettingsActivity.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return FaceEnrollIntroduction.class.getName();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateEnabledState(preference);
    }

    private void updateEnabledState(Preference preference) {
        ParanoidFaceSenseConnector psf = ParanoidFaceSenseConnector.getInstance(mContext);
        if (psf.isParanoidFaceSenseEnabled()) {
            if (psf.isFaceDisabledByAdmin()){
                preference.setEnabled(false);
                preference.setSummary(R.string.disabled_by_administrator_summary);
            }else if (!mLockPatternUtils.isSecure(getUserId())){
                preference.setEnabled(false);
                preference.setSummary(R.string.disabled_because_no_backup_security);
            }else{
                preference.setEnabled(true);
            }
        }
    }
}
