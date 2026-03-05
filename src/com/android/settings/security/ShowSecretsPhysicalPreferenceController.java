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

package com.android.settings.security;

import android.app.compat.CompatChanges;
import android.content.Context;
import android.text.ShowSecretsSetting;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.text.flags.Flags;

public class ShowSecretsPhysicalPreferenceController extends TogglePreferenceController {

    private static final String KEY_SHOW_SECRETS_PHYSICAL = "show_secrets_physical";

    public ShowSecretsPhysicalPreferenceController(Context context) {
        super(context, KEY_SHOW_SECRETS_PHYSICAL);
    }

    @Override
    public boolean isChecked() {
        return ShowSecretsSetting.shouldShowPhysicalInput(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        ShowSecretsSetting.setShouldShowPhysicalInput(mContext, isChecked);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!areSplitSettingsEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return mContext.getResources().getBoolean(R.bool.config_show_show_secrets_physical)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @VisibleForTesting
    boolean areSplitSettingsEnabled() {
        return Flags.splitShowPasswordsToTouchAndPhysical()
                && CompatChanges.isChangeEnabled(
                        ShowSecretsSetting.SPLIT_SHOW_PASSWORDS_TO_TOUCH_AND_PHYSICAL);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }
}
