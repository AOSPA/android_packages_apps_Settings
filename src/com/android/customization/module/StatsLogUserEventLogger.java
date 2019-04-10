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
package com.android.customization.module;

import android.stats.style.nano.StyleEnums;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.customization.model.clock.Clockface;
import com.android.customization.model.grid.GridOption;
import com.android.customization.model.theme.ThemeBundle;
import com.android.wallpaper.module.NoOpUserEventLogger;

import java.util.Map;
import java.util.Objects;

import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_COLOR;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_SHAPE;

/**
 * StatsLog-backed implementation of {@link ThemesUserEventLogger}.
 */
public class StatsLogUserEventLogger extends NoOpUserEventLogger implements ThemesUserEventLogger {

    private static final String TAG = "StatsLogUserEventLogger";

    @Override
    public void logResumed() {
        Log.d(TAG, String.format("logResumed: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.ONRESUME,
                0, 0, 0, 0, 0, 0, 0));
//        StatsLogCompat.write(StyleEnums.ONRESUME,
//                0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void logStopped() {
        Log.d(TAG, String.format("logStopped: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.ONSTOP,
                0, 0, 0, 0, 0, 0, 0));
//        StatsLogCompat.write(StyleEnums.ONSTOP,
//                0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public void logActionClicked(String collectionId, int actionLabelResId) {
        Log.d(TAG, String.format("logActionClicked: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.WALLPAPER_EXPLORE,
                0, 0, 0, 0, 0,
                collectionId.hashCode(),
                0));
//        StatsLogCompat.write(StyleEnums.WALLPAPER_EXPLORE,
//                0, 0, 0, 0, 0,
//                collectionId.hashCode(),
//                0));
    }

    @Override
    public void logIndividualWallpaperSelected(String collectionId) {
        Log.d(TAG, String.format("logIndividualWallpaperSelected: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.WALLPAPER_SELECT,
                0, 0, 0, 0, 0, 0,
                collectionId.hashCode()));
//        StatsLogCompat.write(StyleEnums.WALLPAPER_SELECT,
//                0, 0, 0, 0, 0, 0,
//                collectionId.hashCode());
    }

    @Override
    public void logCategorySelected(String collectionId) {
        Log.d(TAG, String.format("logCategorySelected: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.WALLPAPER_SELECT,
                0, 0, 0, 0, 0,
                collectionId.hashCode(),
                0));
//        StatsLogCompat.write(StyleEnums.WALLPAPER_SELECT,
//                0, 0, 0, 0, 0,
//                collectionId.hashCode(),
//                0);
    }

    @Override
    public void logWallpaperSet(String collectionId, @Nullable String wallpaperId) {
        Log.d(TAG, String.format("logWallpaperSet: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.WALLPAPER_APPLIED,
                0, 0, 0, 0, 0,
                collectionId.hashCode(),
                Objects.hashCode(wallpaperId)));
//        StatsLogCompat.write(StyleEnums.WALLPAPER_SELECT,
//                0, 0, 0, 0, 0,
//                collectionId.hashCode(),
//                0);
    }

    @Nullable
    private String getThemePackage(ThemeBundle theme, String category) {
        Map<String, String> packages = theme.getPackagesByCategory();
        return packages.get(category);
    }

    @Override
    public void logThemeSelected(ThemeBundle theme, boolean isCustomTheme) {
        Log.d(TAG, String.format("logThemeSelected: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_SELECT,
                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_COLOR)),
                Objects.hashCode(getThemePackage(theme,OVERLAY_CATEGORY_FONT)),
                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_SHAPE)),
                0, 0, 0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_SELECT,
//                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_COLOR)),
//                Objects.hashCode(getThemePackage(theme,OVERLAY_CATEGORY_FONT)),
//                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_SHAPE)),
//                0, 0, 0, 0);
    }

    @Override
    public void logThemeApplied(ThemeBundle theme, boolean isCustomTheme) {
        Log.d(TAG, String.format("logThemeApplied: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_APPLIED,
                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_COLOR)),
                Objects.hashCode(getThemePackage(theme,OVERLAY_CATEGORY_FONT)),
                Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_SHAPE)),
                0, 0, 0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_APPLIED,
//        Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_COLOR)),
//        Objects.hashCode(getThemePackage(theme,OVERLAY_CATEGORY_FONT)),
//        Objects.hashCode(getThemePackage(theme, OVERLAY_CATEGORY_SHAPE)),
//                0, 0, 0, 0);
    }

    @Override
    public void logClockSelected(Clockface clock) {
        Log.d(TAG, String.format("logClockSelected: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_SELECT,
                0, 0, 0,
                Objects.hashCode(clock.getId()),
                0, 0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_SELECT,
//        0, 0, 0,
//        Objects.hashCode(clock.getId()),
//        0, 0, 0));
    }

    @Override
    public void logClockApplied(Clockface clock) {
        Log.d(TAG, String.format("logClockApplied: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_APPLIED,
                0, 0, 0,
                Objects.hashCode(clock.getId()),
                0, 0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_APPLIED,
//        0, 0, 0,
//        Objects.hashCode(clock.getId()),
//        0, 0, 0));
    }

    @Override
    public void logGridSelected(GridOption grid) {
        Log.d(TAG, String.format("logGridSelected: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_SELECT,
                0, 0, 0, 0,
                grid.cols,
                0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_SELECT,
//        0, 0, 0, 0,
//        Objects.hashCode(clock.getId()),
//        0, 0));
    }

    @Override
    public void logGridApplied(GridOption grid) {
        Log.d(TAG, String.format("logGridApplied: %d, %d, %d, %d, %d, %d, %d, %d",
                StyleEnums.PICKER_APPLIED,
                0, 0, 0, 0,
                grid.cols,
                0, 0));
//        StatsLogCompat.write(StyleEnums.PICKER_APPLIED,
//        0, 0, 0, 0,
//        Objects.hashCode(clock.getId()),
//        0, 0));
    }
}
