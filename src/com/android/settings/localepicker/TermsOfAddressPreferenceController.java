/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.localepicker;

import android.content.Context;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TermsOfAddressPreferenceController extends BasePreferenceController {

    public TermsOfAddressPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.termsOfAddressEnabled() && LocaleUtils.isTermsOfAddressAvailable(mContext)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }
}
