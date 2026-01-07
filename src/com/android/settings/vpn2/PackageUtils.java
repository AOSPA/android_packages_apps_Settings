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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

class PackageUtils {
    private static final String TAG = PackageUtils.class.getSimpleName();

    // Returns {@code true} if the application exists for the given user.
    static boolean isExistingApp(Context context, int userId, String packageName) {
        try {
            context.getPackageManager().getPackageInfoAsUser(packageName, /* flags= */ 0, userId);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    // Returns whether the given app can be launched which means the app is likely recognizable to
    // the user
    static boolean hasLauncherIntent(
            PackageManager packageManager, int userId, String packageName) {
        Intent intent =
                new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setPackage(packageName);
        List<ResolveInfo> resolveInfos =
                packageManager.queryIntentActivitiesAsUser(intent, /* flags= */ 0, userId);
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    // Auto app has been transitioning to a system app without launcher (Starting in Q).
    // Look for any apps supporting Car Mode.
    static boolean hasCarIntent(PackageManager packageManager, int userId, String packageName) {
        Intent intent =
                new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_CAR_MODE)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .addCategory(Intent.CATEGORY_CAR_DOCK)
                        .setPackage(packageName);
        List<ResolveInfo> resolveInfos =
                packageManager.queryIntentActivitiesAsUser(intent, /* flags= */ 0, userId);
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    private PackageUtils() {}
}
