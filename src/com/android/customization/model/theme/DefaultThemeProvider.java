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
import static com.android.customization.model.ResourceConstants.CONFIG_ICON_MASK;
import static com.android.customization.model.ResourceConstants.ICON_PREVIEW_DRAWABLE_NAME;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_ANDROID;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_LAUNCHER;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_SETTINGS;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ICON_SYSUI;
import static com.android.customization.model.ResourceConstants.SETTINGS_PACKAGE;
import static com.android.customization.model.ResourceConstants.SYSUI_ICONS_FOR_PREVIEW;
import static com.android.customization.model.ResourceConstants.SYSUI_PACKAGE;

import android.content.Context;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.ResourceConstants;
import com.android.customization.model.ResourcesApkProvider;
import com.android.customization.model.theme.ThemeBundle.Builder;
import com.android.customization.model.theme.custom.CustomTheme;
import com.android.customization.module.CustomizationPreferences;
import com.android.wallpaper.R;
import com.android.wallpaper.asset.ResourceAsset;

import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Default implementation of {@link ThemeBundleProvider} that reads Themes' overlays from a stub APK.
 */
public class DefaultThemeProvider extends ResourcesApkProvider implements ThemeBundleProvider {

    private static final String TAG = "DefaultThemeProvider";
    // TODO(b/124796742): remove once custom theme picker is ready to merge
    private static final boolean SHOW_CUSTOM_THEME_OPTION = false;

    private static final String THEMES_ARRAY = "themes";
    private static final String TITLE_PREFIX = "theme_title_";
    private static final String FONT_PREFIX = "theme_overlay_font_";
    private static final String COLOR_PREFIX = "theme_overlay_color_";
    private static final String SHAPE_PREFIX = "theme_overlay_shape_";
    private static final String ICON_ANDROID_PREFIX = "theme_overlay_icon_android_";
    private static final String ICON_LAUNCHER_PREFIX = "theme_overlay_icon_launcher_";
    private static final String ICON_SETTINGS_PREFIX = "theme_overlay_icon_settings_";
    private static final String ICON_SYSUI_PREFIX = "theme_overlay_icon_sysui_";
    private static final String PREVIEW_COLOR_PREFIX = "theme_preview_color_";
    private static final String PREVIEW_SHAPE_PREFIX = "theme_preview_shape_";
    private static final String WALLPAPER_PREFIX = "theme_wallpaper_";
    private static final String WALLPAPER_TITLE_PREFIX = "theme_wallpaper_title_";
    private static final String WALLPAPER_ATTRIBUTION_PREFIX = "theme_wallpaper_attribution_";
    private static final String WALLPAPER_ACTION_PREFIX = "theme_wallpaper_action_";

    private static final String DEFAULT_THEME_NAME= "default";

    private static final String ACCENT_COLOR_LIGHT_NAME = "accent_device_default_light";
    private static final String ACCENT_COLOR_DARK_NAME = "accent_device_default_dark";

    private List<ThemeBundle> mThemes;
    private Map<String, OverlayInfo> mOverlayInfos;
    private final CustomizationPreferences mCustomizationPreferences;

    public DefaultThemeProvider(Context context, CustomizationPreferences customizationPrefs) {
        super(context, context.getString(R.string.themes_stub_package));
        OverlayManager om = context.getSystemService(OverlayManager.class);
        mCustomizationPreferences = customizationPrefs;
        mOverlayInfos = new HashMap<>();

        Consumer<OverlayInfo> addToMap = overlayInfo -> mOverlayInfos.put(
                overlayInfo.packageName, overlayInfo);
        om.getOverlayInfosForTarget(ANDROID_PACKAGE, UserHandle.myUserId()).forEach(addToMap);
        om.getOverlayInfosForTarget(SYSUI_PACKAGE, UserHandle.myUserId()).forEach(addToMap);
        om.getOverlayInfosForTarget(SETTINGS_PACKAGE, UserHandle.myUserId()).forEach(addToMap);
        om.getOverlayInfosForTarget(ResourceConstants.getLauncherPackage(context),
                UserHandle.myUserId()).forEach(addToMap);
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

        String[] themeNames = getItemsFromStub(THEMES_ARRAY);

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

                addFontOverlay(builder, fontOverlayPackage);

                String colorOverlayPackage = getOverlayPackage(COLOR_PREFIX, themeName);

                if (!TextUtils.isEmpty(colorOverlayPackage)) {
                    builder.addOverlayPackage(getOverlayCategory(colorOverlayPackage),
                                colorOverlayPackage)
                            .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME,
                                    colorOverlayPackage))
                            .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME,
                                    colorOverlayPackage))
                            .setColorPreview(getDrawableResourceAsset(
                                    PREVIEW_COLOR_PREFIX, themeName));
                }

                String shapeOverlayPackage = getOverlayPackage(SHAPE_PREFIX, themeName);

                if (!TextUtils.isEmpty(shapeOverlayPackage)) {
                    builder.addOverlayPackage(getOverlayCategory(shapeOverlayPackage),
                                shapeOverlayPackage)
                            .setShapePath(loadString(CONFIG_ICON_MASK, shapeOverlayPackage))
                            .setShapePreview(getDrawableResourceAsset(
                                    PREVIEW_SHAPE_PREFIX, themeName));
                } else {
                    builder.setShapePath(mContext.getResources().getString(
                            Resources.getSystem().getIdentifier(CONFIG_ICON_MASK, "string",
                                    ANDROID_PACKAGE)));
                }

                String iconAndroidOverlayPackage = getOverlayPackage(ICON_ANDROID_PREFIX,
                        themeName);

                addAndroidIconOverlay(builder, iconAndroidOverlayPackage);

                String iconSysUiOverlayPackage = getOverlayPackage(ICON_SYSUI_PREFIX, themeName);

                addSysUiIconOverlay(builder, iconSysUiOverlayPackage);

                String iconLauncherOverlayPackage = getOverlayPackage(ICON_LAUNCHER_PREFIX,
                        themeName);
                addNoPreviewIconOverlay(builder,iconLauncherOverlayPackage);

                String iconSettingsOverlayPackage = getOverlayPackage(ICON_SETTINGS_PREFIX,
                        themeName);

                addNoPreviewIconOverlay(builder, iconSettingsOverlayPackage);

                try {
                    String wallpaperResName = WALLPAPER_PREFIX + themeName;
                    int wallpaperResId = mStubApkResources.getIdentifier(wallpaperResName,
                            "drawable", mStubPackageName);
                    if (wallpaperResId > 0) {
                        builder.setWallpaperInfo(mStubPackageName, wallpaperResName,
                                themeName, wallpaperResId,
                                mStubApkResources.getIdentifier(WALLPAPER_TITLE_PREFIX + themeName,
                                        "string", mStubPackageName),
                                mStubApkResources.getIdentifier(
                                        WALLPAPER_ATTRIBUTION_PREFIX + themeName, "string",
                                        mStubPackageName),
                                mStubApkResources.getIdentifier(WALLPAPER_ACTION_PREFIX + themeName,
                                        "string", mStubPackageName))
                                .setWallpaperAsset(
                                        getDrawableResourceAsset(WALLPAPER_PREFIX, themeName));
                    }
                } catch (NotFoundException e) {
                    // Nothing to do here, if there's no wallpaper we'll just omit wallpaper
                }

                mThemes.add(builder.build());
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load part of theme %s, will skip it", themeName),
                        e);
            }
        }

        if (SHOW_CUSTOM_THEME_OPTION) {
            addCustomTheme();
        }
    }

    private void addNoPreviewIconOverlay(Builder builder, String overlayPackage) {
        if (!TextUtils.isEmpty(overlayPackage)) {
            builder.addOverlayPackage(getOverlayCategory(overlayPackage),
                    overlayPackage);
        }
    }

    private void addSysUiIconOverlay(Builder builder, String iconSysUiOverlayPackage)
            throws NameNotFoundException {
        if (!TextUtils.isEmpty(iconSysUiOverlayPackage)) {
            builder.addOverlayPackage(getOverlayCategory(iconSysUiOverlayPackage),
                    iconSysUiOverlayPackage);
            for (String iconName : SYSUI_ICONS_FOR_PREVIEW) {
                builder.addIcon(loadIconPreviewDrawable(iconName, iconSysUiOverlayPackage));
            }
        }
    }

    private void addAndroidIconOverlay(Builder builder, String iconAndroidOverlayPackage)
            throws NameNotFoundException {
        if (!TextUtils.isEmpty(iconAndroidOverlayPackage)) {
            builder.addOverlayPackage(getOverlayCategory(iconAndroidOverlayPackage),
                        iconAndroidOverlayPackage)
                    .addIcon(loadIconPreviewDrawable(
                            ICON_PREVIEW_DRAWABLE_NAME,
                            iconAndroidOverlayPackage));
        } else {
            builder.addIcon(mContext.getResources().getDrawable(
                    Resources.getSystem().getIdentifier(
                            ICON_PREVIEW_DRAWABLE_NAME,
                            "drawable", ANDROID_PACKAGE), null));
        }
    }

    private void addFontOverlay(Builder builder, String fontOverlayPackage)
            throws NameNotFoundException {
        if (!TextUtils.isEmpty(fontOverlayPackage)) {
            builder.addOverlayPackage(getOverlayCategory(fontOverlayPackage),
                        fontOverlayPackage)
                    .setBodyFontFamily(loadTypeface(
                            ResourceConstants.CONFIG_BODY_FONT_FAMILY,
                            fontOverlayPackage))
                    .setHeadlineFontFamily(loadTypeface(
                            ResourceConstants.CONFIG_HEADLINE_FONT_FAMILY,
                            fontOverlayPackage));
        }
    }

    /**
     * Default theme requires different treatment: if there are overlay packages specified in the
     * stub apk, we'll use those, otherwise we'll get the System default values. But we cannot skip
     * the default theme.
     */
    private void addDefaultTheme() {
        ThemeBundle.Builder builder = new Builder().asDefault();
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
            builder.addOverlayPackage(getOverlayCategory(colorOverlayPackage), colorOverlayPackage)
                    .setColorAccentLight(loadColor(ACCENT_COLOR_LIGHT_NAME, colorOverlayPackage))
                    .setColorAccentDark(loadColor(ACCENT_COLOR_DARK_NAME, colorOverlayPackage))
                    .setColorPreview(getDrawableResourceAsset(PREVIEW_COLOR_PREFIX,
                            DEFAULT_THEME_NAME));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.d(TAG, "Didn't find color overlay for default theme, will use system default", e);
            int colorAccentLight = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_LIGHT_NAME, "color", ANDROID_PACKAGE), null);
            builder.setColorAccentLight(colorAccentLight);

            int colorAccentDark = system.getColor(
                    system.getIdentifier(ACCENT_COLOR_DARK_NAME, "color", ANDROID_PACKAGE), null);
            builder.setColorAccentDark(colorAccentDark)
                    .setColorPreview(getDrawableResourceAsset(PREVIEW_COLOR_PREFIX,
                            DEFAULT_THEME_NAME));
        }

        String fontOverlayPackage = getOverlayPackage(FONT_PREFIX, DEFAULT_THEME_NAME);

        try {
            builder.addOverlayPackage(getOverlayCategory(fontOverlayPackage), fontOverlayPackage)
                    .setBodyFontFamily(loadTypeface(ResourceConstants.CONFIG_BODY_FONT_FAMILY,
                            fontOverlayPackage))
                    .setHeadlineFontFamily(loadTypeface(
                            ResourceConstants.CONFIG_HEADLINE_FONT_FAMILY,
                            fontOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.d(TAG, "Didn't find font overlay for default theme, will use system default", e);
            String headlineFontFamily = system.getString(system.getIdentifier(
                    ResourceConstants.CONFIG_HEADLINE_FONT_FAMILY,"string", ANDROID_PACKAGE));
            String bodyFontFamily = system.getString(system.getIdentifier(
                    ResourceConstants.CONFIG_BODY_FONT_FAMILY,
                    "string", ANDROID_PACKAGE));
            builder.setHeadlineFontFamily(Typeface.create(headlineFontFamily, Typeface.NORMAL))
                    .setBodyFontFamily(Typeface.create(bodyFontFamily, Typeface.NORMAL));
        }

        try {
            String shapeOverlayPackage = getOverlayPackage(SHAPE_PREFIX, DEFAULT_THEME_NAME);
            builder.addOverlayPackage(getOverlayCategory(shapeOverlayPackage), shapeOverlayPackage)
                    .setShapePath(loadString(ICON_PREVIEW_DRAWABLE_NAME, colorOverlayPackage))
                    .setShapePreview(getDrawableResourceAsset(PREVIEW_SHAPE_PREFIX,
                            DEFAULT_THEME_NAME));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.d(TAG, "Didn't find shape overlay for default theme, will use system default", e);
            String iconMaskPath = system.getString(system.getIdentifier(CONFIG_ICON_MASK,
                    "string", ANDROID_PACKAGE));
            builder.setShapePath(iconMaskPath)
                    .setShapePreview(getDrawableResourceAsset(PREVIEW_SHAPE_PREFIX,
                            DEFAULT_THEME_NAME));
        }


        try {
            String iconAndroidOverlayPackage = getOverlayPackage(ICON_ANDROID_PREFIX,
                    DEFAULT_THEME_NAME);
            builder.addOverlayPackage(getOverlayCategory(iconAndroidOverlayPackage),
                        iconAndroidOverlayPackage)
                    .addIcon(loadIconPreviewDrawable(ICON_ANDROID_PREFIX,
                            iconAndroidOverlayPackage));
        } catch (NameNotFoundException | NotFoundException e) {
            Log.d(TAG, "Didn't find Android icons overlay for default theme, using system default",
                    e);
            builder.addIcon(system.getDrawable(system.getIdentifier(
                    ICON_PREVIEW_DRAWABLE_NAME,
                            "drawable", ANDROID_PACKAGE), null));
        }

        try {
            String iconSysUiOverlayPackage = getOverlayPackage(ICON_SYSUI_PREFIX,
                    DEFAULT_THEME_NAME);
            addSysUiIconOverlay(builder, iconSysUiOverlayPackage);
        } catch (NameNotFoundException | NotFoundException e) {
            Log.d(TAG, "Didn't find SystemUi icons overlay for default theme, using system default",
                    e);
            try {
                for (String iconName : SYSUI_ICONS_FOR_PREVIEW) {
                    builder.addIcon(loadIconPreviewDrawable(iconName, SYSUI_PACKAGE));
                }
            } catch (NameNotFoundException | NotFoundException e2) {
                Log.w(TAG, "Didn't find SystemUi package icons, will skip preview", e);
            }
        }

        try {
            String wallpaperResName = WALLPAPER_PREFIX + DEFAULT_THEME_NAME;
            int wallpaperResId = mStubApkResources.getIdentifier(wallpaperResName,
                    "drawable", mStubPackageName);
            if (wallpaperResId > 0) {
                builder.setWallpaperInfo(mStubPackageName, wallpaperResName, DEFAULT_THEME_NAME,
                        mStubApkResources.getIdentifier(
                                wallpaperResName,
                                "drawable", mStubPackageName),
                        mStubApkResources.getIdentifier(WALLPAPER_TITLE_PREFIX + DEFAULT_THEME_NAME,
                                "string", mStubPackageName),
                        mStubApkResources.getIdentifier(
                                WALLPAPER_ATTRIBUTION_PREFIX + DEFAULT_THEME_NAME, "string",
                                mStubPackageName),
                        mStubApkResources.getIdentifier(
                                WALLPAPER_ACTION_PREFIX + DEFAULT_THEME_NAME,
                                "string", mStubPackageName))
                        .setWallpaperAsset(
                                getDrawableResourceAsset(WALLPAPER_PREFIX, DEFAULT_THEME_NAME));
            }
        } catch (NotFoundException e) {
            // Nothing to do here, if there's no wallpaper we'll just omit wallpaper
        }

        mThemes.add(builder.build());
    }

    @Override
    public void storeCustomTheme(CustomTheme theme) {
        mCustomizationPreferences.storeCustomTheme(theme.getSerializedPackages());
    }

    private void addCustomTheme() {
        String serializedTheme = mCustomizationPreferences.getSerializedCustomTheme();
        if (TextUtils.isEmpty(serializedTheme)) {
            mThemes.add(new CustomTheme(mContext.getString(R.string.custom_theme_title),
                    new HashMap<>(), null));
            return;
        }
        ThemeBundle.Builder builder = parseCustomTheme(serializedTheme);
        if (builder != null) {
            builder.setTitle(mContext.getString(R.string.custom_theme_title));
            mThemes.add(builder.build());
        } else {
            Log.w(TAG, "Couldn't read stored custom theme, resetting");
            mThemes.add(new CustomTheme(mContext.getString(R.string.custom_theme_title),
                    new HashMap<>(), null));
        }
    }

    @Override
    public Builder parseCustomTheme(String serializedTheme) {
        try {
            Map<String, String> customPackages = new HashMap<>();

            JSONObject theme = new JSONObject(serializedTheme);
            Iterator<String> keysIterator = theme.keys();

            while (keysIterator.hasNext()) {
                String category = keysIterator.next();
                customPackages.put(category, theme.getString(category));
            }
            CustomTheme.Builder builder = new CustomTheme.Builder();
            builder.setTitle(mContext.getString(R.string.custom_theme_title));
            addFontOverlay(builder, customPackages.get(OVERLAY_CATEGORY_FONT));
            addAndroidIconOverlay(builder, customPackages.get(OVERLAY_CATEGORY_ICON_ANDROID));
            addSysUiIconOverlay(builder, customPackages.get(OVERLAY_CATEGORY_ICON_SYSUI));
            addNoPreviewIconOverlay(builder, customPackages.get(OVERLAY_CATEGORY_ICON_SETTINGS));
            addNoPreviewIconOverlay(builder, customPackages.get(OVERLAY_CATEGORY_ICON_LAUNCHER));

            return builder;
        } catch (JSONException | NameNotFoundException | NotFoundException e) {
            Log.i(TAG, "Couldn't parse serialized custom theme", e);
            return null;
        }
    }

    private String getOverlayPackage(String prefix, String themeName) {
        return getItemStringFromStub(prefix, themeName);
    }

    private ResourceAsset getDrawableResourceAsset(String prefix, String themeName) {
        int drawableResId = mStubApkResources.getIdentifier(prefix + themeName,
                "drawable", mStubPackageName);
        return drawableResId == 0 ? null : new ResourceAsset(mStubApkResources, drawableResId,
                RequestOptions.fitCenterTransform());
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

    private String loadString(String stringName, String packageName)
            throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager().getResourcesForApplication(packageName);
        return overlayRes.getString(overlayRes.getIdentifier(stringName, "string", packageName));
    }

    private Drawable loadIconPreviewDrawable(String drawableName, String packageName)
         throws NameNotFoundException, NotFoundException {

        Resources overlayRes = mContext.getPackageManager().getResourcesForApplication(packageName);
        return overlayRes.getDrawable(
                overlayRes.getIdentifier(drawableName, "drawable", packageName), null);
    }

    @Nullable
    private String getOverlayCategory(String packageName) {
       OverlayInfo info = mOverlayInfos.get(packageName);
       return info != null ? info.category : null;
    }
}
