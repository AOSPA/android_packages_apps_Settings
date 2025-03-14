/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class AudioSharingJoinHandlerDashboardFragment extends DashboardFragment {
    private static final String TAG = "AudioSharingJoinHandlerFrag";

    @Nullable private AudioSharingJoinHandlerController mController;

    @Override
    public int getMetricsCategory() {
        // TODO: use real enum
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_le_audio_sharing_join_handler;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mController = use(AudioSharingJoinHandlerController.class);
        if (mController != null) {
            mController.init(this);
        }
    }
}
