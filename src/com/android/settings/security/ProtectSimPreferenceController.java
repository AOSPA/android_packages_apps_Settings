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

package com.android.settings.security;

import android.content.Context;

/**
 * Preference controller for the "Protect SIM" card preference. The only difference in behaviour
 * from the {@code SimLockPreferenceController} is that it makes the preference available if the
 * flag for automatic SIM PIN management is on, while {@code SimLockPreferenceController} does
 * the opposite.
 */
public class ProtectSimPreferenceController extends SimLockPreferenceController {
    public ProtectSimPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        int availabilityStatus = super.getAvailabilityStatusExcludingFlag();

        if (availabilityStatus == AVAILABLE && !android.security.Flags.autoSimPinManagement()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return availabilityStatus;
    }
}
