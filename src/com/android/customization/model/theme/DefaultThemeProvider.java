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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.theme.ThemeBundle.Builder;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ThemeBundleProvider} that reads Themes' overlays from a stub APK.
 */
public class DefaultThemeProvider implements ThemeBundleProvider {

    private static final String TAG = "DefaultThemeProvider";

    private static final String THEMES_ARRAY = "themes";
    private static final String TITLE_PREFIX = "theme_title_";
    private static final String FONT_PREFIX = "theme_overlay_font_";
    private static final String COLOR_PREFIX = "theme_overlay_color_";

    private static final String DEFAULT_THEME_NAME= "default";

    private static final String ACCENT_COLOR_LIGHT_NAME = "accent_device_default_light";
    private static final String ACCENT_COLOR_DARK_NAME = "accent_device_default_dark";
    private static final String CONFIG_BODY_FONT_FAMILY = "config_bodyFontFamily";
    private static final String CONFIG_HEADLINE_FONT_FAMILY = "config_headlineFontFamily";

    private final Context mContext;
    private final String mStubPackageName;
    private Resources mStubApkResources;
    private List<ThemeBundle> mThemes;

    public DefaultThemeProvider(Context context) {
        mContext = context;
        mStubPackageName = mContext.getString(R.string.themes_stub_package);
        init();
    }

    private void init() {
        if (TextUtils.isEmpty(mStubPackageName)) {
            return;
        }
        mStubApkResources = null;
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo stubAppInfo = pm.getApplicationInfo(
                    mStubPackageName, PackageManager.GET_META_DATA);
            if (stubAppInfo != null) {
                mStubApkResources = pm.getResourcesForApplication(stubAppInfo);
            }
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Themes stub APK not found.");
        }
    }

    @Override
    public boolean isAvailable() {
        return mStubApkResources != null;
    }

    @Override
    public void fetch(OptionsFetchedListener<ThemeBundle> callback, boolean reload) {
        if (mThemes == null || reload) {
            mThemes = new ArrayList<>();
            loadAll();
        }

        if(callback != null) {
            callback.onOptionsLoaded(mThemes);
        }
    }

    private void loadAll() {
        addDefaultTheme();

        int themesListResId = mStubApkResources.getIdentifier(THEMES_ARRAY, "array",
                mStubPackageName);
        String[] themeNames = mStubApkResources.getStringArray(themesListResId);

        for (String themeName : themeNames) {
            // Default theme needs special treatment (see #addDefaultTheme())
            if (DEFAULT_THEME_NAME.equals(themeName)) {
                continue;
            }
            ThemeBundle.Builder builder = new Builder();
            try {
                builder.setTitle(mStubApkResources.getString(
                        mStubApkResources.getIdentifier(TITLE_PREFIX + themeName,
                                "string", mStubPackageName)));

                String fontOverlayPackage = getOverlayPackage(FONT_PREFIX, themeName);

                if (!TextUtils.isEmpty(fontOverlayPackage)) {
                    builder.setFontOverlayPackage(fontOverlayPackage)
                            .setBodyFontFamily(loadTypeface(CONFIG_BODY_FONT_FAMILY,
                                    fontOverlayPackage))
                            .setHeadlineFontFamily(loadTypeface(CONFIG_HEADLINE_FONT_FAMILY,
                                    fontOverlayPackage));
                }

                String colorOverlayPackage = getOverlayPackage(COLOR_PREFIX, themeName);

                if (!TextUtils.isEmpty(colorOverlayPackage)) {
                    builder.setColorPackage(colorOverlayPackage)
                            .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME,
                                    colorOverlayPackage))
                            .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME,
                                    colorOverlayPackage));
                }

                //TODO (santie) read the other overlays

                mThemes.add(builder.build());
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load part of theme %s, will skip it", themeName),
                        e);
            }
        }
    }


    /**
     * Default theme requires different treatment: if there are overlay packages specified in the
     * stub apk, we'll use those, otherwise we'll get the System default values. But we cannot skip
     * the default theme.
     */
    private void addDefaultTheme() {
        ThemeBundle.Builder builder = new Builder();
        Resources system = Resources.getSystem();

        int titleId = mStubApkResources.getIdentifier(TITLE_PREFIX + DEFAULT_THEME_NAME,
                "string", mStubPackageName);
        if (titleId > 0) {
            builder.setTitle(mStubApkResources.getString(titleId));
        } else {
            builder.setTitle(mContext.getString(R.string.default_theme_title));
        }

        String colorOverlayPackage = getOverlayPackage(COLOR_PREFIX, DEFAULT_THEME_NAME);

        try {
            builder.setColorPackage(colorOverlayPackage)
                    .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME, colorOverlayPackage))
                    .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME, colorOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find color overlay for default theme, will use system default", e);
            int colorAccentLight = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_LIGHT_NAME, "color", "android"), null);
            builder.setColorAccentLight(colorAccentLight);

            int colorAccentDark = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_DARK_NAME, "color", "android"), null);
            builder.setColorAccentDark(colorAccentDark);
            builder.setColorPackage(null);
        }

        String fontOverlayPackage = getOverlayPackage(FONT_PREFIX, DEFAULT_THEME_NAME);

        try {
            builder.setFontOverlayPackage(fontOverlayPackage)
                    .setBodyFontFamily(loadTypeface(CONFIG_BODY_FONT_FAMILY,
                            fontOverlayPackage))
                    .setHeadlineFontFamily(loadTypeface(CONFIG_HEADLINE_FONT_FAMILY,
                            fontOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Didn't find font overlay for default theme, will use system default", e);
            String headlineFontFamily = system.getString(system.getIdentifier(
                    CONFIG_HEADLINE_FONT_FAMILY,"string", "android"));
            String bodyFontFamily = system.getString(system.getIdentifier(CONFIG_BODY_FONT_FAMILY,
                    "string", "android"));
            builder.setHeadlineFontFamily(Typeface.create(headlineFontFamily, Typeface.NORMAL))
                    .setBodyFontFamily(Typeface.create(bodyFontFamily, Typeface.NORMAL));
            builder.setFontOverlayPackage(null);
        }

        mThemes.add(builder.build());
    }

    private String getOverlayPackage(String prefix, String themeName) {
        int overlayPackageResId = mStubApkResources.getIdentifier(prefix + themeName,
                "string", mStubPackageName);
        return mStubApkResources.getString(overlayPackageResId);
    }

    private Typeface loadTypeface(String configName, String fontOverlayPackage)
            throws NameNotFoundException, NotFoundException {

        // TODO(santie): check for font being present in system

        Resources overlayRes = mContext.getPackageManager()
                .getResourcesForApplication(fontOverlayPackage);

        String fontFamily = overlayRes.getString(overlayRes.getIdentifier(configName,
                "string", fontOverlayPackage));
        return Typeface.create(fontFamily, Typeface.NORMAL);
    }

    private int loadColor(String colorName, String colorPackage)
            throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager()
                .getResourcesForApplication(colorPackage);
        return overlayRes.getColor(overlayRes.getIdentifier(colorName, "color", colorPackage),
                null);
    }
}
