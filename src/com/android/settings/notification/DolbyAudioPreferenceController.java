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

package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings.System;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class DolbyAudioPreferenceController extends SettingPrefController {

    private static final String KEY_DOLBY_ATMOS = "dolby_atmos";

    public DolbyAudioPreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DOLBY_ATMOS;
    }

    @Override
    public boolean isAvailable() {
        final boolean supportsDolbyAudio = mContext.getResources().getBoolean(R.bool.device_supports_dolby_audio);
        final String targetPackage = mContext.getResources().getString(R.string.dolby_targetPackage);
        final String targetClass = mContext.getResources().getString(R.string.dolby_targetClass);

        return supportsDolbyAudio && !TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass);
    }

}
