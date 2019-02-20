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

import android.app.Activity;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.graphics.Point;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.module.WallpaperPersister;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.module.WallpaperSetter;
import com.android.wallpaper.util.WallpaperCropUtils;

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
    private final WallpaperSetter mWallpaperSetter;
    private final Activity mActivity;

    private Map<String, String> mCurrentOverlays;

    public ThemeManager(ThemeBundleProvider provider, Activity activity,
            WallpaperSetter wallpaperSetter) {
        mProvider = provider;
        mActivity = activity;
        mOverlayManager = activity.getSystemService(OverlayManager.class);
        mWallpaperSetter = wallpaperSetter;
    }

    @Override
    public boolean isAvailable() {
        return mProvider.isAvailable();
    }

    @Override
    public void apply(ThemeBundle theme, Callback callback) {
        // Set wallpaper
        if (theme.shouldUseThemeWallpaper()) {
            applyWallpaper(theme, new SetWallpaperCallback() {
                @Override
                public void onSuccess() {
                    applyOverlays(theme, callback);
                }

                @Override
                public void onError(@Nullable Throwable throwable) {
                    callback.onError(throwable);
                }
            });
        } else {
            applyOverlays(theme, callback);
        }
    }

    private void applyWallpaper(ThemeBundle theme, SetWallpaperCallback callback) {
        Point defaultCropSurfaceSize = WallpaperCropUtils.getDefaultCropSurfaceSize(
                mActivity.getResources(),
                mActivity.getWindowManager().getDefaultDisplay());
        Asset wallpaperAsset = theme.getWallpaperInfo().getAsset(mActivity);
        wallpaperAsset.decodeRawDimensions(mActivity,
                dimensions -> {
                    float scale = 1f;
                    // Calculate scale to fit the screen height
                    if (dimensions != null && dimensions.y > 0) {
                        scale = (float) defaultCropSurfaceSize.y / dimensions.y;
                    }
                    mWallpaperSetter.setCurrentWallpaper(mActivity,
                            theme.getWallpaperInfo(),
                            wallpaperAsset,
                            WallpaperPersister.DEST_BOTH,
                            scale, null, callback);
                });
    }

    private void applyOverlays(ThemeBundle theme, Callback callback) {
        boolean allApplied = true;
        if (theme.isDefault()) {
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_COLOR);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_FONT);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_SHAPE);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, OVERLAY_CATEGORY_ICON_ANDROID);
            allApplied &= disableCurrentOverlay(SYSUI_PACKAGE, OVERLAY_CATEGORY_ICON_SYSUI);
            allApplied &= disableCurrentOverlay(SETTINGS_PACKAGE, OVERLAY_CATEGORY_ICON_SETTINGS);
        } else {
            for (String packageName : theme.getAllPackages()) {
                if (packageName != null) {
                    allApplied &= mOverlayManager.setEnabledExclusiveInCategory(packageName,
                            UserHandle.myUserId());
                }
            }
        }
        allApplied &= Settings.Secure.putString(mActivity.getContentResolver(),
                THEME_SETTING, theme.getSerializedPackages());
        mCurrentOverlays = null;
        if (allApplied) {
            callback.onSuccess();
        } else {
            callback.onError(null);
        }
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<ThemeBundle> callback) {
        mProvider.fetch(callback, false);
    }

    private boolean disableCurrentOverlay(String packageName, String category) {
        OverlayInfo current = getEnabledOverlayInfo(packageName, category);
        if (current != null) {
           return mOverlayManager.setEnabled(current.packageName, false, UserHandle.myUserId());
        }
        return true;
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
        return Settings.Secure.getString(mActivity.getContentResolver(), THEME_SETTING);
    }
}
