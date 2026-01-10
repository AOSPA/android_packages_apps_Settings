/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.accessibility.colorandmotion.ui.ColorAndMotionScreen;
import com.android.settings.dashboard.DashboardFragment;

// TODO(b/445978289): Use CatalystFragment
/** Accessibility settings for color and motion. */
public class ColorAndMotionFragment extends DashboardFragment {

    private static final String TAG = "ColorAndMotionFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Nullable
    @Override
    public String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ColorAndMotionScreen.KEY;
    }
}
