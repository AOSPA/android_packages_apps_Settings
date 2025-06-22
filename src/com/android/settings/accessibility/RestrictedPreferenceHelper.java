/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class helps setup RestrictedPreference for accessibility.
 */
public class RestrictedPreferenceHelper {

    private final Context mContext;
    private final DevicePolicyManager mDpm;
    private final AppOpsManager mAppOps;

    public RestrictedPreferenceHelper(Context context) {
        mContext = context;
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mAppOps = context.getSystemService(AppOpsManager.class);
    }

    /**
     * Creates the list of {@link RestrictedPreference} with the installedServices arguments.
     *
     * @param installedServices The list of {@link AccessibilityServiceInfo}s of the
     *                          installed accessibility services
     * @return The list of {@link RestrictedPreference}
     */
    public List<RestrictedPreference> createAccessibilityServicePreferenceList(
            List<AccessibilityServiceInfo> installedServices) {

        final Set<ComponentName> enabledServices =
                AccessibilityUtils.getEnabledServicesFromSettings(mContext);
        final List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final int installedServicesSize = installedServices.size();

        final List<RestrictedPreference> preferenceList = new ArrayList<>(
                installedServicesSize);

        for (int i = 0; i < installedServicesSize; ++i) {
            final AccessibilityServiceInfo info = installedServices.get(i);
            final ResolveInfo resolveInfo = info.getResolveInfo();
            final String packageName = resolveInfo.serviceInfo.packageName;
            final ComponentName componentName = new ComponentName(packageName,
                    resolveInfo.serviceInfo.name);
            final boolean serviceEnabled = enabledServices.contains(componentName);

            RestrictedPreference preference = new AccessibilityServicePreference(
                    mContext, packageName, resolveInfo.serviceInfo.applicationInfo.uid,
                    info, serviceEnabled);
            setRestrictedPreferenceEnabled(preference, permittedServices, serviceEnabled);
            preferenceList.add(preference);
        }
        return preferenceList;
    }

    /**
     * Creates the list of {@link AccessibilityActivityPreference} with the installedShortcuts
     * arguments.
     *
     * @param installedShortcuts The list of {@link AccessibilityShortcutInfo}s of the
     *                           installed accessibility shortcuts
     * @return The list of {@link AccessibilityActivityPreference}
     */
    public List<AccessibilityActivityPreference> createAccessibilityActivityPreferenceList(
            List<AccessibilityShortcutInfo> installedShortcuts) {

        final int installedShortcutsSize = installedShortcuts.size();
        final List<AccessibilityActivityPreference> preferenceList = new ArrayList<>(
                installedShortcutsSize);

        for (int i = 0; i < installedShortcutsSize; ++i) {
            final AccessibilityShortcutInfo info = installedShortcuts.get(i);
            final ActivityInfo activityInfo = info.getActivityInfo();
            final ComponentName componentName = info.getComponentName();

            AccessibilityActivityPreference preference = new AccessibilityActivityPreference(
                    mContext, componentName.getPackageName(), activityInfo.applicationInfo.uid,
                    info);
            // Accessibility Activities do not have elevated privileges so restricting
            // them based on ECM or device admin does not give any value.
            preference.setEnabled(true);
            preferenceList.add(preference);
        }
        return preferenceList;
    }

    private void setRestrictedPreferenceEnabled(RestrictedPreference preference,
            final List<String> permittedServices, boolean serviceEnabled) {
        // permittedServices null means all accessibility services are allowed.
        boolean serviceAllowed = permittedServices == null || permittedServices.contains(
                preference.getPackageName());

        if (android.permission.flags.Flags.enhancedConfirmationModeApisEnabled()
                && android.security.Flags.extendEcmToAllSettings()) {
            preference.checkEcmRestrictionAndSetDisabled(
                    AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE,
                    preference.getPackageName(), serviceEnabled);
            if (preference.isDisabledByEcm()) {
                serviceAllowed = false;
            }

            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true);
            } else {
                // Disable accessibility service that are not permitted.
                final RestrictedLockUtils.EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                mContext, preference.getPackageName(), UserHandle.myUserId());

                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else if (!preference.isDisabledByEcm()) {
                    preference.setEnabled(false);
                }
            }
        } else {
            boolean appOpsAllowed;
            if (serviceAllowed) {
                try {
                    final int mode = mAppOps.noteOpNoThrow(
                            AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS,
                            preference.getUid(), preference.getPackageName());
                    final boolean ecmEnabled = mContext.getResources().getBoolean(
                            com.android.internal.R.bool.config_enhancedConfirmationModeEnabled);
                    appOpsAllowed = !ecmEnabled || mode == AppOpsManager.MODE_ALLOWED
                            || mode == AppOpsManager.MODE_DEFAULT;
                    serviceAllowed = appOpsAllowed;
                } catch (Exception e) {
                    // Allow service in case if app ops is not available in testing.
                    appOpsAllowed = true;
                }
            } else {
                appOpsAllowed = false;
            }
            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true);
            } else {
                // Disable accessibility service that are not permitted.
                final RestrictedLockUtils.EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                mContext, preference.getPackageName(), UserHandle.myUserId());

                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else if (!appOpsAllowed) {
                    preference.setDisabledByAppOps(true);
                } else {
                    preference.setEnabled(false);
                }
            }
        }
    }
}
