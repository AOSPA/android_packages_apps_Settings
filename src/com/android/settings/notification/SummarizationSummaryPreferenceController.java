/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for the summarized notifications settings page.
 */
public class SummarizationSummaryPreferenceController extends BasePreferenceController {

    NotificationBackend mBackend;

    public SummarizationSummaryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBackend.showSummarizationSettings() ?  AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (android.app.Flags.nmSummarizationAll()) {
            preference.setTitle(R.string.notification_summarization_description_with_apps);
        }
    }
}
