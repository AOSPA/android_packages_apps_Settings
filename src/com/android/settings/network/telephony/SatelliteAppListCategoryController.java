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

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SatelliteRepository;
import com.android.settingslib.Utils;

import java.util.List;

/** A controller to show some of apps info which supported on Satellite service. */
public class SatelliteAppListCategoryController extends BasePreferenceController {
    private static final String TAG = "SatelliteAppListCategoryController";
    @VisibleForTesting
    static final int MAXIMUM_OF_PREFERENCE_AMOUNT = 3;

    private List<String> mPackageNameList;

    public SatelliteAppListCategoryController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Initialize the necessary applications' data*/
    public void init() {
        SatelliteRepository satelliteRepository = new SatelliteRepository(mContext);
        init(satelliteRepository);
    }

    @VisibleForTesting
    void init(@NonNull SatelliteRepository satelliteRepository) {
        mPackageNameList = satelliteRepository.getSatelliteDataOptimizedApps();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        for (int i = 0; i < mPackageNameList.size() && i < MAXIMUM_OF_PREFERENCE_AMOUNT; i++) {
            String packageName = mPackageNameList.get(i);
            ApplicationInfo appInfo = getApplicationInfo(mContext, packageName);
            if (appInfo != null) {
                Drawable icon = Utils.getBadgedIcon(mContext, appInfo);
                CharSequence name = appInfo.loadLabel(mContext.getPackageManager());
                Preference pref = new Preference(mContext);
                pref.setIcon(icon);
                pref.setTitle(name);
                preferenceCategory.addPreference(pref);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.satellite25q4Apis()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return mPackageNameList.isEmpty()
                ? CONDITIONALLY_UNAVAILABLE
                : AVAILABLE;
    }

    static ApplicationInfo getApplicationInfo(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfoAsUser(packageName, /* flags= */ 0, context.getUserId());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
