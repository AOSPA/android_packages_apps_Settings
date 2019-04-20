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
package com.android.customization.model.clock;

import android.content.ContentResolver;
import android.provider.Settings.Secure;

import com.android.customization.module.ThemesUserEventLogger;

/**
 * {@link CustomizationManager} for clock faces that implements apply by writing to secure settings.
 */
public class ClockManager extends BaseClockManager {

    // TODO: use constant from Settings.Secure
    static final String CLOCK_FACE_SETTING = "lock_screen_custom_clock_face";
    private final ContentResolver mContentResolver;
    private final ThemesUserEventLogger mEventLogger;

    public ClockManager(ContentResolver resolver, ClockProvider provider,
            ThemesUserEventLogger logger) {
        super(provider);
        mContentResolver = resolver;
        mEventLogger = logger;
    }

    @Override
    protected void handleApply(Clockface option, Callback callback) {
        boolean stored = Secure.putString(mContentResolver, CLOCK_FACE_SETTING, option.getId());
        if (stored) {
            mEventLogger.logClockApplied(option);
            callback.onSuccess();
        } else {
            callback.onError(null);
        }
    }

    @Override
    protected String lookUpCurrentClock() {
        return Secure.getString(mContentResolver, CLOCK_FACE_SETTING);
    }
}
