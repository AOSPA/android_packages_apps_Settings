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

import android.view.View;

import com.android.customization.model.theme.ThemeBundle;
import com.android.wallpaper.R;

import java.util.Map;

public class CustomTheme extends ThemeBundle {

    public CustomTheme(String title, Map<String, String> overlayPackages,
            PreviewInfo previewInfo) {
        super(title, overlayPackages, false, null, previewInfo);
    }

    @Override
    public void bindThumbnailTile(View view) {
        if (isDefined()) {
            super.bindThumbnailTile(view);
        }
    }

    @Override
    public int getLayoutResId() {
        return isDefined() ? R.layout.theme_option : R.layout.custom_theme_option;
    }

    @Override
    public boolean shouldUseThemeWallpaper() {
        return false;
    }

    public boolean isDefined() {
        return getPreviewInfo() != null;
    }
}
