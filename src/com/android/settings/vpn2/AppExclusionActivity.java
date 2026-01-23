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

package com.android.settings.vpn2;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/** Settings screen configuring app exclusion list for a VPN. */
public final class AppExclusionActivity extends CollapsingToolbarBaseActivity {
    private static final String TAG = "AppExclusionActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserManager userManager = getSystemService(UserManager.class);

        // Use Utils.getSecureTargetUser() to get the target user securely.
        // The target user can be set by the system in the case of cross-profile intent
        // forwarding, or set by the Settings app running in the same user through extras.
        UserHandle userHandle =
                Utils.getSecureTargetUser(
                        getActivityToken(),
                        userManager,
                        savedInstanceState,
                        getIntent().getExtras());
        int userId = userHandle.getIdentifier();

        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        EnforcingAdmin admin =
                dpm.getEnforcingAdminsForPolicy(
                                DevicePolicyIdentifiers.getIdentifierForUserRestriction(
                                        UserManager.DISALLOW_CONFIG_VPN),
                                userId)
                        .getMostImportantEnforcingAdmin();
        if (admin != null) {
            Log.i(TAG, "User " + userId + " is not allowed to configure VPN.");
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                    this, admin, UserManager.DISALLOW_CONFIG_VPN);
            finish();
            return;
        }

        String vpnPackage = getLaunchedFromPackage();
        if (vpnPackage == null) {
            Log.e(TAG, "Cannot find the VPN package launching this activity.");
            finish();
            return;
        }

        if (!PackageUtils.isExistingApp(this, userId, vpnPackage)) {
            Log.e(TAG, "VPN package " + vpnPackage + " does not exist for user " + userId + ".");
            finish();
            return;
        }

        AppExclusionUtils.removeUninstalledApp(this, userId, vpnPackage);

        if (savedInstanceState == null) {
            final Fragment fragment = new AppExclusionFragment();
            final Bundle args = new Bundle();
            args.putString(AppExclusionFragment.EXTRA_VPN_PACKAGE, vpnPackage);
            args.putInt(AppExclusionFragment.EXTRA_USER_ID, userId);
            fragment.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }
}
