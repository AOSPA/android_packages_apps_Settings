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

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;
import com.android.wallpaper.R;

import java.util.HashMap;
import java.util.Map;

public class CustomThemeManager implements CustomizationManager<ThemeComponentOption> {

    private final Map<String, String> overlayPackages = new HashMap<>();
    private final CustomTheme mOriginalTheme;

    public CustomThemeManager(@Nullable CustomTheme existingTheme) {
        if (existingTheme != null && existingTheme.isDefined()) {
            mOriginalTheme = existingTheme;
            overlayPackages.putAll(existingTheme.getPackagesByCategory());
        } else {
            mOriginalTheme = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void apply(ThemeComponentOption option, @Nullable Callback callback) {
        overlayPackages.putAll(option.getOverlayPackages());
        if (callback != null) {
            callback.onSuccess();
        }
    }

    public Map<String, String> getOverlayPackages() {
        return overlayPackages;
    }

    public CustomTheme buildPartialCustomTheme(Context context) {
        return new CustomTheme(mOriginalTheme != null
                ? mOriginalTheme.getTitle() : context.getString(R.string.custom_theme_title),
                overlayPackages, null);
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<ThemeComponentOption> callback, boolean reload) {
        //Unused
    }

    public CustomTheme getOriginalTheme() {
        return mOriginalTheme;
    }
}
