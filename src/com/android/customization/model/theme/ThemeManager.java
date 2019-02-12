/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.theme;

import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.SETTINGS_PACKAGE;
import static com.android.customization.model.ResourceConstants.SYSUI_PACKAGE;

import android.content.Context;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThemeManager implements CustomizationManager<ThemeBundle> {

    private static final String OVERLAY_CATEGORY_COLOR = "android.theme.customization.accent_color";
    private static final String OVERLAY_CATEGORY_FONT = "android.theme.customization.font";
    private static final String OVERLAY_CATEGORY_SHAPE =
            "android.theme.customization.adaptive_icon_shape";
    private static final String OVERLAY_CATEGORY_ICON_ANDROID =
            "android.theme.customization.icon_pack.android";
    private static final String OVERLAY_CATEGORY_ICON_SETTINGS =
            "android.theme.customization.icon_pack.settings";
    private static final String OVERLAY_CATEGORY_ICON_SYSUI =
            "android.theme.customization.icon_pack.systemui";

    private static final Set<String> THEME_CATEGORIES = new HashSet<>();
    static {
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_COLOR);
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_FONT);
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_SHAPE);
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_ICON_ANDROID);
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_ICON_SETTINGS);
        THEME_CATEGORIES.add(OVERLAY_CATEGORY_ICON_SYSUI);
    };

    //TODO: replace with System.Secure constant
    private static final String THEME_SETTING = "theme_customization_overlay_packages";


    private final ThemeBundleProvider mProvider;
    private final OverlayManager mOverlayManager;
    private final Context mContext;
    private boolean useThemeWallpaper;
    private Map<String, String> mCurrentOverlays;

    public ThemeManager(ThemeBundleProvider provider, Context context) {
        mProvider = provider;
        mContext = context;
        mOverlayManager = context.getSystemService(OverlayManager.class);
    }

    @Override
    public boolean isAvailable() {
        return mProvider.isAvailable();
    }

    @Override
    public void apply(ThemeBundle theme) {
        if (theme.isDefault()) {
            disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_COLOR);
            disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_FONT);
            disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_SHAPE);
            disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_ICON_ANDROID);
            disableCurrentOverlay(SYSUI_PACKAGE, OVERLAY_CATEGORY_ICON_SYSUI);
            disableCurrentOverlay(SETTINGS_PACKAGE, OVERLAY_CATEGORY_ICON_SETTINGS);
        } else {
            for (String packageName : theme.getAllPackages()) {
                if (packageName != null) {
                    mOverlayManager.setEnabledExclusiveInCategory(packageName,
                            UserHandle.myUserId());
                }
            }
        }
        Settings.Secure.putString(mContext.getContentResolver(), THEME_SETTING,
                theme.getSerializedPackages());
        mCurrentOverlays = null;
        // TODO: set wallpaper
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<ThemeBundle> callback) {
        mProvider.fetch(callback, false);
    }

    private void disableCurrentOverlay(String packageName, String category) {
        OverlayInfo current = getEnabledOverlayInfo(packageName, category);
        if (current != null) {
            mOverlayManager.setEnabled(current.packageName, false, UserHandle.myUserId());
        }
    }

    @Nullable
    private OverlayInfo getEnabledOverlayInfo(String packageName, String category) {
        List<OverlayInfo> overlayInfos = mOverlayManager
                .getOverlayInfosForTarget(packageName, UserHandle.myUserId());
        for (OverlayInfo overlayInfo : overlayInfos) {
            if (category.equals(overlayInfo.category) && overlayInfo.isEnabled()) {
                return overlayInfo;
            }
        }
        return null;
    }

    public Map<String, String> getCurrentOverlays() {
        if (mCurrentOverlays == null) {
            mCurrentOverlays = new HashMap<>();
            addAllEnabledOverlaysForPackage(ANDROID_PACKAGE);
            addAllEnabledOverlaysForPackage(SYSUI_PACKAGE);
            addAllEnabledOverlaysForPackage(SETTINGS_PACKAGE);
        }
        return mCurrentOverlays;
    }

    private void addAllEnabledOverlaysForPackage(String targetPackage) {
        for (OverlayInfo overlayInfo :
                mOverlayManager.getOverlayInfosForTarget(targetPackage, UserHandle.myUserId())) {
            if (overlayInfo.isEnabled() && THEME_CATEGORIES.contains(overlayInfo.category)) {
                mCurrentOverlays.put(overlayInfo.category, overlayInfo.packageName);
            }
        }
    }

    public String getStoredOverlays() {
        return Settings.Secure.getString(mContext.getContentResolver(), THEME_SETTING);
    }
}
