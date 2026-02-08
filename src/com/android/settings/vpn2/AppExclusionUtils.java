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

import static java.util.stream.Collectors.toCollection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnManager;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Collection of application exclusion related utility functions. */
final class AppExclusionUtils {
    private static final String TAG = "AppExclusionUtils";

    /** Returns a package name list from {@link ApplicationInfo} {@link List}. */
    public static List<String> fetchNameListFromInfoList(List<ApplicationInfo> infoList) {
        return infoList.stream()
                .map(info -> info.packageName)
                .collect(toCollection(ArrayList<String>::new));
    }

    /** Returns the application label from package name. */
    public static String getApplicationLabel(Context context, int userId, String packageName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = getPackageInfo(context, userId, packageName);
        if (pkgInfo != null) {
            return pkgInfo.applicationInfo.loadLabel(pm).toString();
        }
        return "";
    }

    public static @Nullable PackageInfo getPackageInfo(
            Context context, int userId, String packageName) {
        PackageManager pm = context.getPackageManager();
        // The catch block is for the case that the app doesn't exist, so we can fall
        // back to the default activity icon.
        try {
            return pm.getPackageInfoAsUser(packageName, 0 /* flags */, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Unable to get package info for " + packageName);
            return null;
        }
    }

    static void removeUninstalledApp(Context context, int userId, String vpnPackage) {
        List<String> exclusionList = getAppExclusionList(context, userId, vpnPackage);
        if (exclusionList == null) {
            // No application exclusion list. No changes.
            return;
        }
        List<String> newList = new ArrayList<>();
        for (String name : exclusionList) {
            if (PackageUtils.isExistingApp(context, userId, name)) {
                newList.add(name);
            }
        }
        if (newList.containsAll(exclusionList)) {
            // No changes
            return;
        }
        setAppExclusionList(context, userId, vpnPackage, newList);
    }

    /** Gets VPN application exclusion list from {@link VpnManager} */
    static List<String> getAppExclusionList(Context context, int userId, String vpnPackage) {
        List<String> list =
                context.getSystemService(VpnManager.class)
                        .getAppExclusionList(UserHandle.of(userId), vpnPackage);
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    /** Sets VPN application exclusion list to {@link VpnManager} */
    static boolean setAppExclusionList(
            Context context, int userId, String vpnPackage, List<String> list) {
        return context.getSystemService(VpnManager.class)
                .setAppExclusionList(UserHandle.of(userId), vpnPackage, list);
    }

    private AppExclusionUtils() {}
}
