/*
 * Copyright (C) 2024 cyberknight777
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;

public class OffscreenGestureSettingsController extends BasePreferenceController {

    public OffscreenGestureSettingsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        // Disable offscreen gestures page if KeyHandler is disabled.
        final boolean mKeyHandlerEnabled = mContext.getResources().getBoolean(R.bool.config_enableKeyHandler);
        return mKeyHandlerEnabled ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
