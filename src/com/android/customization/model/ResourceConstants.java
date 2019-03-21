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
package com.android.customization.model;

import android.content.Context;
import android.provider.Settings.Secure;

import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Holds common strings used to reference system resources.
 */
public interface ResourceConstants {

    /**
     * Package name for android platform resources.
     */
    String ANDROID_PACKAGE = "android";

    /**
     * Package name for android settings resources.
     */
    String SETTINGS_PACKAGE = "com.android.settings";

    /**
     * Package name for android sysui resources.
     */
    String SYSUI_PACKAGE = "com.android.systemui";

    /**
     * Name of the system resource for icon mask
     */
    String CONFIG_ICON_MASK = "config_icon_mask";

    /**
     * Overlay Categories that theme picker handles.
     */
    String OVERLAY_CATEGORY_COLOR = "android.theme.customization.accent_color";
    String OVERLAY_CATEGORY_FONT = "android.theme.customization.font";
    String OVERLAY_CATEGORY_SHAPE = "android.theme.customization.adaptive_icon_shape";
    String OVERLAY_CATEGORY_ICON_ANDROID = "android.theme.customization.icon_pack.android";
    String OVERLAY_CATEGORY_ICON_SETTINGS = "android.theme.customization.icon_pack.settings";
    String OVERLAY_CATEGORY_ICON_SYSUI = "android.theme.customization.icon_pack.systemui";
    String OVERLAY_CATEGORY_ICON_LAUNCHER = "android.theme.customization.icon_pack.launcher";

    /**
     * Secure Setting used to store the currently set theme.
     */
    String THEME_SETTING = Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES;
    String CONFIG_BODY_FONT_FAMILY = "config_bodyFontFamily";
    String CONFIG_HEADLINE_FONT_FAMILY = "config_headlineFontFamily";
    String ICON_PREVIEW_DRAWABLE_NAME = "ic_wifi_signal_3";
    String[] SYSUI_ICONS_FOR_PREVIEW = {
            "ic_qs_bluetooth_on",
            "ic_dnd",
            "ic_signal_flashlight",
            "ic_qs_auto_rotate",
            "ic_signal_airplane"
    };

    ArrayList<String> sTargetPackages = new ArrayList<>();

    static String[] getPackagesToOverlay(Context context) {
        if (sTargetPackages.isEmpty()) {
            sTargetPackages.addAll(Arrays.asList(ANDROID_PACKAGE, SETTINGS_PACKAGE,
                    SYSUI_PACKAGE));
            sTargetPackages.add(getLauncherPackage(context));
        }
        return sTargetPackages.toArray(new String[0]);
    }

    static String getLauncherPackage(Context context) {
        return context.getString(R.string.launcher_overlayable_package);
    }
}
