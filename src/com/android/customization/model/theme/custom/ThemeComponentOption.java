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
package com.android.customization.model.theme.custom;

import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_COLOR;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_SHAPE;
import static com.android.customization.model.ResourceConstants.SYSUI_ICONS_FOR_PREVIEW;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;
import com.android.customization.model.ResourceConstants;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an option of a component of a custom Theme (for example, a possible color, or font,
 * shape, etc).
 * Extending classes correspond to each component's options and provide the structure to bind
 * preview and thumbnails.
 * // TODO (santie): refactor the logic to bind preview cards to reuse between ThemeFragment and
 * // here
 */
public abstract class ThemeComponentOption implements CustomizationOption<ThemeComponentOption> {

    protected final Map<String, String> mOverlayPackageNames = new HashMap<>();

    protected void addOverlayPackage(String category, String packageName) {
        mOverlayPackageNames.put(category, packageName);
    }

    public Map<String, String> getOverlayPackages() {
        return mOverlayPackageNames;
    }

    @Override
    public String getTitle() {
        return null;
    }

    public abstract void bindPreview(ViewGroup container);

    public static class FontOption extends ThemeComponentOption {

        private final String mLabel;
        private final Typeface mHeadlineFont;
        private final Typeface mBodyFont;

        public FontOption(String packageName, String label, Typeface headlineFont,
                Typeface bodyFont) {
            addOverlayPackage(OVERLAY_CATEGORY_FONT, packageName);
            mLabel = label;
            mHeadlineFont = headlineFont;
            mBodyFont = bodyFont;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public void bindThumbnailTile(View view) {
            ((TextView) view.findViewById(R.id.thumbnail_text)).setTypeface(
                    mHeadlineFont);
        }

        @Override
        public boolean isActive(CustomizationManager<ThemeComponentOption> manager) {
            CustomThemeManager customThemeManager = (CustomThemeManager) manager;
            return Objects.equals(getOverlayPackages().get(OVERLAY_CATEGORY_FONT),
                    customThemeManager.getOverlayPackages().get(OVERLAY_CATEGORY_FONT));
        }

        @Override
        public int getLayoutResId() {
            return R.layout.theme_font_option;
        }

        @Override
        public void bindPreview(ViewGroup container) {
            TextView header = container.findViewById(R.id.theme_preview_card_header);
            header.setText(mLabel);
            header.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_font, 0, 0);

            ViewGroup cardBody = container.findViewById(R.id.theme_preview_card_body_container);
            if (cardBody.getChildCount() == 0) {
                LayoutInflater.from(container.getContext()).inflate(
                        R.layout.preview_card_font_content,
                        cardBody, true);
            }
            TextView title = container.findViewById(R.id.font_card_title);
            title.setTypeface(mHeadlineFont);
            TextView bodyText = container.findViewById(R.id.font_card_body);
            bodyText.setTypeface(mBodyFont);
        }
    }

    public static class IconOption extends ThemeComponentOption {

        public static final int THUMBNAIL_ICON_POSITION = 0;
        private static int[] mIconIds = {
                R.id.preview_icon_0, R.id.preview_icon_1, R.id.preview_icon_2, R.id.preview_icon_3,
                R.id.preview_icon_4, R.id.preview_icon_5
        };

        private List<Drawable> mIcons = new ArrayList<>();
        private String mLabel;

        @Override
        public void bindThumbnailTile(View view) {
            Resources res = view.getContext().getResources();
            Drawable icon = mIcons.get(THUMBNAIL_ICON_POSITION).mutate();
            icon.setTint(res.getColor(R.color.icon_thumbnail_color, null));
            ((ImageView) view.findViewById(R.id.option_icon)).setImageDrawable(
                    icon);
        }

        @Override
        public boolean isActive(CustomizationManager<ThemeComponentOption> manager) {
            CustomThemeManager customThemeManager = (CustomThemeManager) manager;
            if (getOverlayPackages().isEmpty()) {
                return customThemeManager.getOverlayPackages().isEmpty();
            }
             for (Map.Entry<String, String> overlayEntry : getOverlayPackages().entrySet()) {
                 if(!Objects.equals(overlayEntry.getValue(),
                         customThemeManager.getOverlayPackages().get(overlayEntry.getKey()))) {
                     return false;
                 }
             }
             return true;
        }

        @Override
        public int getLayoutResId() {
            return R.layout.theme_icon_option;
        }

        @Override
        public void bindPreview(ViewGroup container) {
            TextView header = container.findViewById(R.id.theme_preview_card_header);
            header.setText(mLabel);
            header.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_wifi_24px, 0, 0);

            ViewGroup cardBody = container.findViewById(R.id.theme_preview_card_body_container);
            if (cardBody.getChildCount() == 0) {
                LayoutInflater.from(container.getContext()).inflate(
                        R.layout.preview_card_icon_content, cardBody, true);
            }
            for (int i = 0; i < mIconIds.length; i++) {
                ((ImageView) container.findViewById(mIconIds[i])).setImageDrawable(
                        mIcons.get(i));
            }
        }

        public void addIcon(Drawable previewIcon) {
            mIcons.add(previewIcon);
        }

        /**
         * @return whether this icon option has overlays and previews for all the required packages
         */
        public boolean isValid(Context context) {
            return getOverlayPackages().keySet().size() ==
                    ResourceConstants.getPackagesToOverlay(context).length
                && mIcons.size() == SYSUI_ICONS_FOR_PREVIEW.length + 1;
        }

        public void setLabel(String label) {
            mLabel = label;
        }
    }

    public static class ColorOption extends ThemeComponentOption {

        /**
         * Ids of views used to represent quick setting tiles in the color preview screen
         */
        private static int[] COLOR_TILE_IDS = {
                R.id.preview_color_qs_0_bg, R.id.preview_color_qs_1_bg, R.id.preview_color_qs_2_bg
        };
        static int[] COLOR_TILES_ICON_IDS = {
                R.id.preview_color_qs_0_icon, R.id.preview_color_qs_1_icon,
                R.id.preview_color_qs_2_icon
        };

        /**
         * Ids of views used to represent control buttons in the color preview screen
         */
        private static int[] COLOR_BUTTON_IDS = {
                R.id.preview_check_selected, R.id.preview_radio_selected,
                R.id.preview_toggle_selected,
        };

        @ColorInt private int mColorAccentLight;
        @ColorInt private int mColorAccentDark;
        /**
         * Icons shown as example of QuickSettings tiles in the color preview screen.
         */
        private List<Drawable> mIcons = new ArrayList<>();

        /**
         * Drawable with the currently selected shape to be used as background of the sample
         * QuickSetting icons in the color preview screen.
         */
        private Drawable mShapeDrawable;

        ColorOption(String packageName, @ColorInt int lightColor,
                @ColorInt int darkColor) {
            addOverlayPackage(OVERLAY_CATEGORY_COLOR, packageName);
            mColorAccentLight = lightColor;
            mColorAccentDark = darkColor;
        }

        @Override
        public void bindThumbnailTile(View view) {
            int color = resolveColor(view.getResources());
            ((ImageView) view.findViewById(R.id.option_tile)).getDrawable().setTint(color);
        }

        private int resolveColor(Resources res) {
            Configuration configuration = res.getConfiguration();
            return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? mColorAccentDark : mColorAccentLight;
        }

        @Override
        public boolean isActive(CustomizationManager<ThemeComponentOption> manager) {
            CustomThemeManager customThemeManager = (CustomThemeManager) manager;
            return Objects.equals(getOverlayPackages().get(OVERLAY_CATEGORY_COLOR),
                    customThemeManager.getOverlayPackages().get(OVERLAY_CATEGORY_COLOR));
        }

        @Override
        public int getLayoutResId() {
            return R.layout.theme_color_option;
        }

        @Override
        public void bindPreview(ViewGroup container) {
            TextView header = container.findViewById(R.id.theme_preview_card_header);
            header.setText(R.string.preview_name_color);
            header.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_colorize_24px, 0, 0);

            ViewGroup cardBody = container.findViewById(R.id.theme_preview_card_body_container);
            if (cardBody.getChildCount() == 0) {
                LayoutInflater.from(container.getContext()).inflate(
                        R.layout.preview_card_color_content, cardBody, true);
            }
            @ColorInt int accentColor = resolveColor(container.getResources());
            ColorStateList tintList = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_selected},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[] {
                            accentColor,
                            accentColor
                    }
            );

            for (int i = 0; i < COLOR_BUTTON_IDS.length; i++) {
                CompoundButton button = container.findViewById(COLOR_BUTTON_IDS[i]);
                button.setButtonTintList(tintList);
            }

            Switch toggle = container.findViewById(R.id.preview_toggle_selected);
            toggle.setThumbTintList(tintList);
            toggle.setTrackTintList(tintList);

            ColorStateList seekbarTintList = ColorStateList.valueOf(accentColor);
            SeekBar seekbar = container.findViewById(R.id.preview_seekbar);
            seekbar.setThumbTintList(seekbarTintList);
            seekbar.setProgressTintList(seekbarTintList);
            seekbar.setProgressBackgroundTintList(seekbarTintList);
            // Disable seekbar
            seekbar.setOnTouchListener((view, motionEvent) -> true);
            if (!mIcons.isEmpty() && mShapeDrawable != null) {
                for (int i = 0; i < COLOR_TILE_IDS.length; i++) {
                    Drawable icon = mIcons.get(i).getConstantState().newDrawable();
                    //TODO: load and set the shape.
                    Drawable bgShape = mShapeDrawable.getConstantState().newDrawable();
                    bgShape.setTint(accentColor);

                    ImageView bg = container.findViewById(COLOR_TILE_IDS[i]);
                    bg.setImageDrawable(bgShape);
                    ImageView fg = container.findViewById(COLOR_TILES_ICON_IDS[i]);
                    fg.setImageDrawable(icon);
                }
            }
        }

        public void setPreviewIcons(List<Drawable> icons) {
            mIcons.addAll(icons);
        }

        public void setShapeDrawable(@Nullable Drawable shapeDrawable) {
            mShapeDrawable = shapeDrawable;
        }
    }

    public static class ShapeOption extends ThemeComponentOption {

        ShapeOption(String packageName) {
            addOverlayPackage(OVERLAY_CATEGORY_SHAPE, packageName);
        }

        @Override
        public void bindThumbnailTile(View view) {

        }

        @Override
        public boolean isActive(CustomizationManager<ThemeComponentOption> manager) {
            return false;
        }

        @Override
        public int getLayoutResId() {
            return 0;
        }

        @Override
        public void bindPreview(ViewGroup container) {

        }
    }
}
