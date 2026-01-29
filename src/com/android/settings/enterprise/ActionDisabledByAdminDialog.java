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

package com.android.settings.enterprise;

import static android.provider.Settings.ACTION_SHOW_SUSPENDED_PACKAGE_ADMIN_SUPPORT_DETAILS;
import static android.security.advancedprotection.AdvancedProtectionManager.ADVANCED_PROTECTION_SYSTEM_ENTITY;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.SystemAuthority;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.advancedprotection.AdvancedProtectionManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.theme.R;
import com.android.settingslib.widget.theme.flags.Flags;

public class ActionDisabledByAdminDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private ActionDisabledByAdminDialogHelper mDialogHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Flags.isExpressiveDesignEnabled() && SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_AlertDialog_SettingsLib_Expressive);
        }
        super.onCreate(savedInstanceState);
        final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                getAdminDetailsFromIntent(getIntent());
        final String restriction = getRestrictionFromIntent(getIntent());
        mDialogHelper = new ActionDisabledByAdminDialogHelper(this, restriction);

        final AlertDialog.Builder dialogBuilder =
                (android.app.supervision.flags.Flags.deprecateDpmSupervisionApis()
                        && enforcedAdmin.component == null)
                        ? mDialogHelper.prepareDialogBuilder(restriction,
                            getEnforcingAdmin(getIntent()))
                        : mDialogHelper.prepareDialogBuilder(restriction, enforcedAdmin);
        dialogBuilder.setOnDismissListener(this).show();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        final String restriction = getRestrictionFromIntent(intent);

        if (android.app.supervision.flags.Flags.deprecateDpmSupervisionApis()
                && admin.component == null) {
            mDialogHelper.updateDialog(restriction, getEnforcingAdmin(intent));
        } else {
            mDialogHelper.updateDialog(restriction, admin);
        }
    }

    @VisibleForTesting
    EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        final EnforcedAdmin enforcedAdmin = new EnforcedAdmin(null, UserHandle.of(
                UserHandle.myUserId()));
        if (intent == null) {
            return enforcedAdmin;
        }
        enforcedAdmin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName.class);
        int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());

        final String restriction = getRestrictionFromIntent(intent);
        if (enforcedAdmin.component == null && restriction != null) {
            if (shouldLaunchAdvancedProtectionDialog(intent)) {
                // TODO(b/381025131): Move advanced protection logic to DevicePolicyManager or
                //  elsewhere.
                launchAdvancedProtectionDialog(userId, restriction);
            } else {
                EnforcingAdmin enforcingAdmin = getEnforcingAdmin(intent);
                if (enforcingAdmin != null) {
                    enforcedAdmin.component = enforcingAdmin.getComponentName();
                }
            }
        }

        if (intent.hasExtra(Intent.EXTRA_USER)) {
            enforcedAdmin.user = intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle.class);
        } else {
            if (userId == UserHandle.USER_NULL) {
                enforcedAdmin.user = null;
            } else {
                enforcedAdmin.user = UserHandle.of(userId);
            }
        }
        return enforcedAdmin;
    }

    private void launchAdvancedProtectionDialog(int userId, String restriction) {
        Intent apmSupportIntent = AdvancedProtectionManager
                .createSupportIntentForPolicyIdentifierOrRestriction(restriction,
                        AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_UNKNOWN);
        startActivityAsUser(apmSupportIntent, UserHandle.of(userId));
        finish();
    }

    private boolean shouldLaunchAdvancedProtectionDialog(Intent intent) {
        EnforcingAdmin enforcingAdmin = getEnforcingAdmin(intent);
        return isAdvancedProtectionAdmin(enforcingAdmin);
    }

    @VisibleForTesting
    @Nullable
    EnforcingAdmin getEnforcingAdmin(Intent intent) {
        if (intent == null) {
            return null;
        }
        // Suspended package dialog is handled specially with a separate action due to complex
        // policy enforcement model.
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()
                && ACTION_SHOW_SUSPENDED_PACKAGE_ADMIN_SUPPORT_DETAILS.equals(intent.getAction())) {
            return getEnforcingAdminForSuspendedPackage(intent);
        }
        if (android.app.admin.flags.Flags.enforcingAdminExtraEnabled()
                && intent.hasExtra(DevicePolicyManager.EXTRA_ENFORCING_ADMIN)) {
            return intent.getParcelableExtra(DevicePolicyManager.EXTRA_ENFORCING_ADMIN,
                    EnforcingAdmin.class);
        }
        String restriction = getRestrictionFromIntent(intent);
        if (restriction == null) {
            return null;
        }
        final DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);

        if (dpm == null) {
            return null;
        }

        final int userId = getUserIdFromIntent(intent);

        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            PolicyEnforcementInfo policyEnforcementInfo = dpm.getEnforcingAdminsForPolicy(
                    DevicePolicyIdentifiers.getIdentifierForUserRestriction(restriction), userId);
            return policyEnforcementInfo.getMostImportantEnforcingAdmin();
        }

        return dpm.getEnforcingAdmin(userId, restriction);
    }

    private EnforcingAdmin getEnforcingAdminForSuspendedPackage(@NonNull Intent intent) {
        final DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);

        if (dpm == null) {
            return null;
        }

        final int userId = getUserIdFromIntent(intent);
        // If a package is suspended by admin, it can either be suspended by the admin on its
        // current user by PACKAGES_SUSPENDED_POLICY or suspended on parent (and connected profiles)
        // by PERSONAL_APPS_SUSPENDED_POLICY.
        PolicyEnforcementInfo packageSuspendedPolicy = dpm.getEnforcingAdminsForPolicy(
                DevicePolicyIdentifiers.PACKAGES_SUSPENDED_POLICY, userId);
        if (packageSuspendedPolicy.isEnforced()) {
            return packageSuspendedPolicy.getMostImportantEnforcingAdmin();
        }
        // PERSONAL_APPS_SUSPENDED_POLICY affects the parent of managed user and all
        // other profiles that are connected to the parent e.g. private profile and clone
        // profile.
        final UserHandle parent = getSystemService(UserManager.class).getProfileParent(
                UserHandle.of(userId));
        int targetUserId = parent == null ? userId : parent.getIdentifier();
        PolicyEnforcementInfo personalAppsSuspendedPolicy = dpm.getEnforcingAdminsForPolicy(
                DevicePolicyIdentifiers.PERSONAL_APPS_SUSPENDED_POLICY, targetUserId);
        return personalAppsSuspendedPolicy.getMostImportantEnforcingAdmin();
    }

    @VisibleForTesting
    String getRestrictionFromIntent(Intent intent) {
        if (intent == null) return null;
        return intent.getStringExtra(DevicePolicyManager.EXTRA_RESTRICTION);
    }

    private int getUserIdFromIntent(Intent intent) {
        return intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
    }

    private static boolean isAdvancedProtectionAdmin(@Nullable EnforcingAdmin admin) {
        if (admin == null) {
            return false;
        }
        return admin.getAuthority() instanceof SystemAuthority authority
                && ADVANCED_PROTECTION_SYSTEM_ENTITY.equals(authority.getSystemEntity());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
