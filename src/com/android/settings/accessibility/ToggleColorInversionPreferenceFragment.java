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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.colorinversion.ui.ColorInversionScreen;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Settings page for color inversion.
 */
public class ToggleColorInversionPreferenceFragment extends DashboardFragment {
    private static final String TAG = "ToggleColorInversionPreferenceFragment";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View rootView = getActivity().getWindow().peekDecorView();
        if (rootView != null) {
            rootView.setAccessibilityPaneTitle(getString(
                    R.string.accessibility_display_inversion_preference_title));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_inversion;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ColorInversionScreen.KEY;
    }
}
