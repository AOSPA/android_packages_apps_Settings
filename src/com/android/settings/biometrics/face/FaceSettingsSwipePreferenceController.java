/*
 * Copyright (C) 2020-2022 Paranoid Android
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

import static android.provider.Settings.Secure.FACE_UNLOCK_ALWAYS_REQUIRE_SWIPE;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.Utils;

public class FaceSettingsSwipePreferenceController
        extends FaceSettingsPreferenceController {

    static final String KEY = "security_settings_face_require_swipe";

    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = OFF;  // swipe to face unlock is disabled by default

    private FaceManager mFaceManager;
    private UserManager mUserManager;

    public FaceSettingsSwipePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    public FaceSettingsSwipePreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public boolean isChecked() {
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            return false;
        }
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                FACE_UNLOCK_ALWAYS_REQUIRE_SWIPE, DEFAULT, getUserId()) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(), FACE_UNLOCK_ALWAYS_REQUIRE_SWIPE,
                isChecked ? ON : OFF, getUserId());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mUserManager.isManagedProfile(getUserId())) {
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
