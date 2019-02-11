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
import android.graphics.Point;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.ResourceConstants;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.module.WallpaperPersister;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.module.WallpaperSetter;
import com.android.wallpaper.util.WallpaperCropUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThemeManager implements CustomizationManager<ThemeBundle> {

    private static final Set<String> THEME_CATEGORIES = new HashSet<>();
    static {
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_COLOR);
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_FONT);
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_SHAPE);
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_ICON_ANDROID);
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_ICON_SETTINGS);
        THEME_CATEGORIES.add(ResourceConstants.OVERLAY_CATEGORY_ICON_SYSUI);
    };


    private final ThemeBundleProvider mProvider;
    private final OverlayManagerCompat mOverlayManagerCompat;

    private final WallpaperSetter mWallpaperSetter;
    private final Activity mActivity;

    private Map<String, String> mCurrentOverlays;

    public ThemeManager(ThemeBundleProvider provider, Activity activity,
            WallpaperSetter wallpaperSetter, OverlayManagerCompat overlayManagerCompat) {
        mProvider = provider;
        mActivity = activity;
        mOverlayManagerCompat = overlayManagerCompat;
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
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_COLOR);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_FONT);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_SHAPE);
            allApplied &= disableCurrentOverlay(ANDROID_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_ICON_ANDROID);
            allApplied &= disableCurrentOverlay(SYSUI_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_ICON_SYSUI);
            allApplied &= disableCurrentOverlay(SETTINGS_PACKAGE, ResourceConstants.OVERLAY_CATEGORY_ICON_SETTINGS);
        } else {
            for (String packageName : theme.getAllPackages()) {
                if (packageName != null) {
                    allApplied &= mOverlayManagerCompat.setEnabledExclusiveInCategory(packageName,
                            UserHandle.myUserId());
                }
            }
        }
        allApplied &= Settings.Secure.putString(mActivity.getContentResolver(),
                ResourceConstants.THEME_SETTING, theme.getSerializedPackages());
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

    private boolean disableCurrentOverlay(String targetPackage, String category) {
        String currentPackageName = mOverlayManagerCompat.getEnabledPackageName(targetPackage,
                category);
        if (currentPackageName != null) {
           return mOverlayManagerCompat.disableOverlay(currentPackageName, UserHandle.myUserId());
        }
        return true;
    }

    public Map<String, String> getCurrentOverlays() {
        if (mCurrentOverlays == null) {
            mCurrentOverlays = mOverlayManagerCompat.getEnabledOverlaysForTargets(ANDROID_PACKAGE,
                    SYSUI_PACKAGE, SETTINGS_PACKAGE);
            mCurrentOverlays.entrySet().removeIf(
                    categoryAndPackage -> !THEME_CATEGORIES.contains(categoryAndPackage.getKey()));
        }
        return mCurrentOverlays;
    }

    public String getStoredOverlays() {
        return Settings.Secure.getString(mActivity.getContentResolver(), ResourceConstants.THEME_SETTING);
    }
}
