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

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;

/**
 * Represents an option of a component of a custom Theme (for example, a possible color, or font,
 * shape, etc).
 * Extending classes correspond to each component's options and provide the structure to bind
 * preview and thumbnails.
 */
public abstract class ThemeComponentOption implements CustomizationOption<ThemeComponentOption> {

    protected final String mOverlayPackageName;

    ThemeComponentOption(String packageName) {
        mOverlayPackageName = packageName;
    }

    public String getOverlayPackageName() {
        return mOverlayPackageName;
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
            super(packageName);
            mLabel = label;
            mHeadlineFont = headlineFont;
            mBodyFont = bodyFont;
        }

        @Override
        public String getTitle() {
            return mLabel;
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

    public static class IconOption extends ThemeComponentOption {

        IconOption(String packageName) {
            super(packageName);
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

    public static class ColorOption extends ThemeComponentOption {

        ColorOption(String packageName) {
            super(packageName);
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

    public static class ShapeOption extends ThemeComponentOption {

        ShapeOption(String packageName) {
            super(packageName);
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
