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
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.PathParser;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;
import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.asset.ResourceAsset;
import com.android.wallpaper.model.WallpaperInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Theme component available in the system as a "persona" bundle.
 * Note that in this context a Theme is not related to Android's Styles, but it's rather an
 * abstraction representing a series of overlays to be applied to the system.
 */
public class ThemeBundle implements CustomizationOption<ThemeBundle> {

    private final String mTitle;
    private final PreviewInfo mPreviewInfo;
    private final boolean mIsDefault;
    protected final Map<String, String> mPackagesByCategory;
    @Nullable private final WallpaperInfo mWallpaperInfo;
    private WallpaperInfo mOverrideWallpaper;

    protected ThemeBundle(String title, Map<String, String> overlayPackages,
            boolean isDefault, @Nullable WallpaperInfo wallpaperInfo,
            PreviewInfo previewInfo) {
        mTitle = title;
        mIsDefault = isDefault;
        mPreviewInfo = previewInfo;
        mWallpaperInfo = wallpaperInfo;
        mPackagesByCategory = Collections.unmodifiableMap(overlayPackages);
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public void bindThumbnailTile(View view) {
        Resources res = view.getContext().getResources();

        ((TextView) view.findViewById(R.id.theme_option_font)).setTypeface(
                mPreviewInfo.headlineFontFamily);
        if (mPreviewInfo.shapeDrawable != null) {
            ((ShapeDrawable) mPreviewInfo.shapeDrawable).getPaint().setColor(
                    mPreviewInfo.colorAccentLight);
            ((ImageView) view.findViewById(R.id.theme_option_shape)).setImageDrawable(
                    mPreviewInfo.shapeDrawable);
        }
        if (!mPreviewInfo.icons.isEmpty()) {
            Drawable icon = mPreviewInfo.icons.get(0).mutate();
            icon.setTint(res.getColor(R.color.icon_thumbnail_color, null));
            ((ImageView) view.findViewById(R.id.theme_option_icon)).setImageDrawable(
                    icon);
        }
    }

    @Override
    public boolean isActive(CustomizationManager<ThemeBundle> manager) {
        ThemeManager themeManager = (ThemeManager) manager;
        String serializedOverlays = themeManager.getStoredOverlays();

        if (mIsDefault) {
            return TextUtils.isEmpty(serializedOverlays);
        } else {
            Map<String, String> currentOverlays = themeManager.getCurrentOverlays();
            return mPackagesByCategory.equals(currentOverlays);
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.theme_option;
    }

    public PreviewInfo getPreviewInfo() {
        return mPreviewInfo;
    }

    public void setOverrideThemeWallpaper(WallpaperInfo homeWallpaper) {
        mOverrideWallpaper = homeWallpaper;
    }

    public boolean shouldUseThemeWallpaper() {
        return mOverrideWallpaper == null && mWallpaperInfo != null;
    }

    public Asset getWallpaperPreviewAsset(Context context) {
        return mOverrideWallpaper != null ?
                mOverrideWallpaper.getThumbAsset(context) :
                getPreviewInfo().wallpaperAsset;
    }

    public WallpaperInfo getWallpaperInfo() {
        return mWallpaperInfo;
    }

    boolean isDefault() {
        return mIsDefault;
    }

    Collection<String> getAllPackages() {
        return mPackagesByCategory.values();
    }

    public String getSerializedPackages() {
        if (isDefault()) {
            return "";
        }

        return new JSONObject(mPackagesByCategory).toString();
    }


    public static class PreviewInfo {
        public final Typeface bodyFontFamily;
        public final Typeface headlineFontFamily;
        @ColorInt public final int colorAccentLight;
        @ColorInt public final int colorAccentDark;
        public final List<Drawable> icons;
        public final Drawable shapeDrawable;
        @Nullable public final ResourceAsset wallpaperAsset;
        @Nullable public final ResourceAsset colorPreviewAsset;
        @Nullable public final ResourceAsset shapePreviewAsset;

        private PreviewInfo(Typeface bodyFontFamily, Typeface headlineFontFamily,
                int colorAccentLight, int colorAccentDark, List<Drawable> icons,
                Drawable shapeDrawable, @Nullable ResourceAsset wallpaperAsset,
                @Nullable ResourceAsset colorPreviewAsset,
                @Nullable ResourceAsset shapePreviewAsset) {
            this.bodyFontFamily = bodyFontFamily;
            this.headlineFontFamily = headlineFontFamily;
            this.colorAccentLight = colorAccentLight;
            this.colorAccentDark = colorAccentDark;
            this.icons = icons;
            this.shapeDrawable = shapeDrawable;
            this.wallpaperAsset = wallpaperAsset;
            this.colorPreviewAsset = colorPreviewAsset;
            this.shapePreviewAsset = shapePreviewAsset;
        }

        /**
         * Returns the accent color to be applied corresponding with the current configuration's
         * UI mode.
         * @return one of {@link #colorAccentDark} or {@link #colorAccentLight}
         */
        @ColorInt
        public int resolveAccentColor(Resources res) {
            return (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? colorAccentDark : colorAccentLight;
        }
    }

    public static class Builder {
        private static final float PATH_SIZE = 100f;
        protected String mTitle;
        private Typeface mBodyFontFamily;
        private Typeface mHeadlineFontFamily;
        @ColorInt private int mColorAccentLight;
        @ColorInt private int mColorAccentDark;
        private List<Drawable> mIcons = new ArrayList<>();
        private String mShapePath;
        private boolean mIsDefault;
        private ResourceAsset mWallpaperAsset;
        private ResourceAsset mColorPreview;
        private ResourceAsset mShapePreview;
        private WallpaperInfo mWallpaperInfo;
        protected Map<String, String> mPackages = new HashMap<>();

        public ThemeBundle build() {
            return new ThemeBundle(mTitle, mPackages, mIsDefault, mWallpaperInfo,
                    createPreviewInfo());
        }

        protected PreviewInfo createPreviewInfo() {
            ShapeDrawable shapeDrawable = null;
            if (!TextUtils.isEmpty(mShapePath)) {
                PathShape shape = new PathShape(PathParser.createPathFromPathData(mShapePath),
                        PATH_SIZE, PATH_SIZE);
                shapeDrawable = new ShapeDrawable(shape);
                shapeDrawable.setIntrinsicHeight((int) PATH_SIZE);
                shapeDrawable.setIntrinsicWidth((int) PATH_SIZE);
            }
            return new PreviewInfo(mBodyFontFamily, mHeadlineFontFamily, mColorAccentLight,
                    mColorAccentDark, mIcons, shapeDrawable, mWallpaperAsset,
                    mColorPreview, mShapePreview);
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setBodyFontFamily(@Nullable Typeface bodyFontFamily) {
            mBodyFontFamily = bodyFontFamily;
            return this;
        }

        public Builder setHeadlineFontFamily(@Nullable Typeface headlineFontFamily) {
            mHeadlineFontFamily = headlineFontFamily;
            return this;
        }

        public Builder setColorAccentLight(@ColorInt int colorAccentLight) {
            mColorAccentLight = colorAccentLight;
            return this;
        }

        public Builder setColorAccentDark(@ColorInt int colorAccentDark) {
            mColorAccentDark = colorAccentDark;
            return this;
        }

        public Builder addIcon(Drawable icon) {
            mIcons.add(icon);
            return this;
        }

        public Builder addOverlayPackage(String category, String packageName) {
            mPackages.put(category, packageName);
            return this;
        }

        public Builder setShapePath(String path) {
            mShapePath = path;
            return this;
        }

        public Builder setColorPreview(ResourceAsset colorPreview) {
            mColorPreview = colorPreview;
            return this;
        }

        public Builder setShapePreview(ResourceAsset shapePreview) {
            mShapePreview = shapePreview;
            return this;
        }

        public Builder setWallpaperInfo(String wallpaperPackageName, String wallpaperResName,
                String themeId, @DrawableRes int wallpaperResId, @StringRes int titleResId,
                @StringRes int attributionResId, @StringRes int actionUrlResId) {
            mWallpaperInfo = new ThemeBundledWallpaperInfo(wallpaperPackageName, wallpaperResName,
                    themeId, wallpaperResId, titleResId, attributionResId, actionUrlResId);
            return this;
        }

        public Builder setWallpaperAsset(ResourceAsset wallpaperAsset) {
            mWallpaperAsset = wallpaperAsset;
            return this;
        }

        public Builder asDefault() {
            mIsDefault = true;
            return this;
        }
    }
}
