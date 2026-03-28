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

package com.android.settings.datetime;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyIdentifier;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.RestrictedPreferenceHelperProvider;

class DateTimePolicyHelper {
    private static final String TAG = "DateTimePolicyHelper";

    private DateTimePolicyHelper() {}

    /**
     * Checks the auto time policy and sets the disabled by admin state on the preference if
     * enforced by policy value.
     *
     * <p>If the policy is not enforcing, the preference will be disabled by {@link
     * UserManager#DISALLOW_CONFIG_DATE_TIME} user restriction (if set).
     *
     * @param preference The preference to set the disabled by admin state on.
     */
    static void checkAutoTimePolicyAndSetDisabled(Context context, Preference preference) {
        final @Nullable DevicePolicyManager dpm =
                context.getSystemService(DevicePolicyManager.class);
        final int userId = UserHandle.myUserId();
        if (dpm == null) {
            Log.w(TAG, "Unable to find DevicePolicyManager for the given context");
            return;
        }

        final RestrictedPreferenceHelper preferenceHelper =
                getRestrictedPreferenceHelper(preference);
        if (preferenceHelper == null) {
            return;
        }

        EnforcingAdmin enforcingAdmin = getEnforcingAdminForAutoTime(dpm, userId);
        if (enforcingAdmin != null) {
            preferenceHelper.setDisabledByEnforcingAdmin(enforcingAdmin);
        } else {
            // If no policy or policy is not enforcing, check for user restrictions. Ensure not
            // disabled by a previous admin state.
            preferenceHelper.setDisabledByEnforcingAdmin(null);
            preferenceHelper.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_DATE_TIME, userId);
        }
    }

    private static @Nullable EnforcingAdmin getEnforcingAdminForAutoTime(
            DevicePolicyManager dpm, int userId) {
        final Integer autoTimePolicyValue =
                dpm.getResolvedDeviceWidePolicy(PolicyIdentifier.AUTO_TIME);
        if (autoTimePolicyValue == null) {
            return null;
        }
        switch (autoTimePolicyValue) {
            case PolicyIdentifier.AUTO_TIME_ENABLED:
            case PolicyIdentifier.AUTO_TIME_DISABLED:
                return dpm.getEnforcingAdminsForPolicy(
                                DevicePolicyIdentifiers.AUTO_TIME_POLICY, userId)
                        .getMostImportantEnforcingAdmin();
            case PolicyIdentifier.AUTO_TIME_USER_CHOICE:
            case PolicyIdentifier.AUTO_TIME_ENABLED_UNENFORCED:
            case PolicyIdentifier.AUTO_TIME_DISABLED_UNENFORCED:
            default:
                return null;
        }
    }

    private static @Nullable RestrictedPreferenceHelper getRestrictedPreferenceHelper(
            Preference preference) {
        if (preference instanceof RestrictedPreferenceHelperProvider helperProvider) {
            return helperProvider.getRestrictedPreferenceHelper();
        }

        Log.w(
                TAG,
                "Preference is not of type RestrictedPreferenceHelperProvider : "
                        + preference.getClass().getName());
        return null;
    }
}
