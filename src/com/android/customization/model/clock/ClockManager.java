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

import android.content.Context;
import android.provider.Settings.Secure;

import com.android.customization.model.CustomizationManager;
import com.android.customization.module.ThemesUserEventLogger;

public class ClockManager implements CustomizationManager<Clockface> {

    // TODO: use constant from Settings.Secure
    private static final String CLOCK_FACE_SETTING = "lock_screen_custom_clock_face";
    private final ClockProvider mClockProvider;
    private final Context mContext;
    private final ThemesUserEventLogger mEventLogger;

    public ClockManager(Context context, ClockProvider provider, ThemesUserEventLogger logger) {
        mClockProvider = provider;
        mContext = context;
        mEventLogger = logger;
    }

    @Override
    public boolean isAvailable() {
        return mClockProvider.isAvailable();
    }

    @Override
    public void apply(Clockface option, Callback callback) {
        boolean stored = Secure.putString(mContext.getContentResolver(),
                CLOCK_FACE_SETTING, option.getId());
        if (stored) {
            mEventLogger.logClockApplied(option);
            callback.onSuccess();
        } else {
            callback.onError(null);
        }
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<Clockface> callback, boolean reload) {
        mClockProvider.fetch(callback, false);
    }

    public String getCurrentClock() {
        return Secure.getString(mContext.getContentResolver(), CLOCK_FACE_SETTING);
    }
}
