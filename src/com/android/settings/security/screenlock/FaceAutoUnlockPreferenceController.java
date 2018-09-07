/*
 * Copyright 2019 Paranoid Android
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

package com.android.settings.security.screenlock;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;

public class FaceAutoUnlockPreferenceController extends TogglePreferenceController {

    private static final String FACE_AUTO_UNLOCK = "face_auto_unlock";

    private final LockPatternUtils mLockPatternUtils;

    public FaceAutoUnlockPreferenceController(Context context) {
        super(context, FACE_AUTO_UNLOCK);
        mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.isSecure(UserHandle.myUserId())) {
            return BasePreferenceController.AVAILABLE;
        } else {
            return BasePreferenceController.DISABLED_FOR_USER;
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.System.FACE_AUTO_UNLOCK, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.FACE_AUTO_UNLOCK, isChecked ? 1 : 0);
        return true;
    }
}