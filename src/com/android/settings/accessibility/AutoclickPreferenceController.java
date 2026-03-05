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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

// LINT.IfChange
/** Preference controller for autoclick (dwell timing). */
public class AutoclickPreferenceController extends BasePreferenceController {

    public AutoclickPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
        if (!enabled) {
            return mContext.getResources().getText(R.string.autoclick_disabled);
        }
        final int delayMillis = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);
        return AutoclickUtils.getAutoclickDelaySummary(mContext,
                R.string.accessibility_autoclick_delay_unit_second, delayMillis);
    }
}
// LINT.ThenChange(autoclick/ui/AutoclickScreen.kt)
