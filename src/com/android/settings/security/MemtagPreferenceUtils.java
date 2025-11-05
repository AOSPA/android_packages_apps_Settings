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

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.EnforcingAdmin;
import android.content.Context;
import androidx.annotation.Nullable;
import android.app.admin.PolicyEnforcementInfo;

/**
 * Utilities for memory tagging settings.
 */
public class MemtagPreferenceUtils {

    private final Context mContext;

    public MemtagPreferenceUtils(Context context) {
        mContext = context;
    }

    @Nullable
    public EnforcingAdmin getMemtagEnforcingAdmin() {
        final DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return null;
        }
        final PolicyEnforcementInfo policyEnforcementInfo =
                    dpm.getEnforcingAdminsForPolicy(
                            DevicePolicyIdentifiers.MEMORY_TAGGING_POLICY, mContext.getUserId());
        return policyEnforcementInfo.getMostImportantEnforcingAdmin();
    }

}
