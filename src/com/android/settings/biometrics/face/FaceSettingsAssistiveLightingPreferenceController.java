/*
 * Copyright (C) 2020 Paranoid Android
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;

import com.android.settings.Utils;

public class FaceSettingsAssistiveLightingPreferenceController
        extends FaceSettingsPreferenceController {

    public static final String KEY = "security_settings_face_assistive_lighting";

    @VisibleForTesting
    protected FaceManager mFaceManager;
    private UserManager mUserManager;

    public FaceSettingsAssistiveLightingPreferenceController(Context context) {
        this(context, KEY);
    }

    public FaceSettingsAssistiveLightingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_ASSISTIVE_LIGHTING, 0, getUserId()) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_ASSISTIVE_LIGHTING, isChecked ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isAvailable(mContext)) {
            preference.setEnabled(false);
        } else if (adminDisabled()) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mUserManager.isManagedProfile(UserHandle.myUserId())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (mFaceManager != null && mFaceManager.isHardwareDetected()) {
            return mFaceManager.hasEnrolledTemplates(getUserId())
                    ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }
}
