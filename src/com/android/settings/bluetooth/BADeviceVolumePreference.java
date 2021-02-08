/*
 *Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *Not a contribution
 */

/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import java.util.Objects;

/** A slider preference that directly controls an BA device volume **/
public class BADeviceVolumePreference extends SeekBarPreference {
    private static final String TAG = "BADeviceVolumePreference";

    protected SeekBar mSeekBar;
    private ImageView mIconView;

    public BADeviceVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_ba_device_volume_slider);
    }

    public BADeviceVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_ba_device_volume_slider);
    }

    public BADeviceVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_ba_device_volume_slider);
    }

    public BADeviceVolumePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_ba_device_volume_slider);
    }
}

