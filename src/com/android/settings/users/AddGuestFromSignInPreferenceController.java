/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.users;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference controller to control the toggle for "Allow guest to use device"
 *
 * <p>This preference depends on the {@code android.os.UserManager#DISALLOW_ADD_USER}, and it will
 * be disabled and display the unchecked state when the adding users is set to OFF.
 */
public class AddGuestFromSignInPreferenceController extends TogglePreferenceController {

    private final UserCapabilities mUserCaps;
    private UserManager mUserManager;

    @VisibleForTesting
    public AddGuestFromSignInPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mUserCaps = UserCapabilities.create(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!android.os.Flags.disallowAddGuest()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!mContext.getResources()
                .getBoolean(
                        com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen)) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!mUserCaps.isAdmin()) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return !mUserManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_ADD_GUEST, UserHandle.SYSTEM);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mUserManager.setUserRestriction(
                UserManager.DISALLOW_ADD_GUEST, !isChecked, UserHandle.SYSTEM);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

}
