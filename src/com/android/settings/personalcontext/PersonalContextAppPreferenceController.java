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

package com.android.settings.personalcontext;

import android.content.Context;
import android.service.personalcontext.PersonalContextManager;
import android.util.Log;

/**
 * Controller for displaying and handling changes to the per-app PersonalContext settings.
 */
public class PersonalContextAppPreferenceController {
    private static final String TAG = "PersonalContextPrefController";

    private final Context mContext;
    private final String mPackageName;

    public PersonalContextAppPreferenceController(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
    }

    /**
     * Sets whether the package associated with this controller has personal context enabled.
     */
    public void setPersonalContextEnabled(boolean value) {
        if (!isPersonalContextAvailable()) {
            Log.e(TAG, "PersonalContextManagerService not available on device.");
            return;
        }

        final PersonalContextManager personalContextManager =
                mContext.getSystemService(PersonalContextManager.class);

        personalContextManager.setPersonalContextModeEnabled(mPackageName, value);
    }

    /**
     * Returns whether personal context is enabled for the package associated with this controller.
     */
    public boolean isPersonalContextForAppEnabled() {
        if (!isPersonalContextAvailable()) {
            return false;
        }

        final PersonalContextManager personalContextManager =
                mContext.getSystemService(PersonalContextManager.class);

        return personalContextManager.isPersonalContextModeEnabled(mPackageName);
    }

    /**
     * Returns whether personal context service has been enabled on this device.
     */
    public boolean isPersonalContextServiceEnabled() {
        if (!isPersonalContextAvailable()) {
            return false;
        }

        final PersonalContextManager personalContextManager =
                mContext.getSystemService(PersonalContextManager.class);

        return personalContextManager.isEnabled();
    }

    /**
     * Returns whether personal context is available on this device.
     */
    public boolean isPersonalContextAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enablePersonalContextManagerService
        ) && mContext.getSystemService(PersonalContextManager.class) != null;
    }
}
