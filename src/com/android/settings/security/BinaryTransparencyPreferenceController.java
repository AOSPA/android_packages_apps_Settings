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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import com.android.settings.flags.Flags;

import com.android.settings.core.BasePreferenceController;
import java.util.List;

/**
 * Preference controller for Android Binary Transparency.
 *
 * Tapping this preference would launch the Binary Transparency application.
 * This preference is shown in the More Security & Privacy settings page under Safety Center only if
 * the Binary Transparency feature is enabled and the application for verification of the Android
 * Build is present on the system.
 */
public class BinaryTransparencyPreferenceController extends BasePreferenceController {
    private final PackageManager mPackageManager;

    public BinaryTransparencyPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.enableBinaryTransparencyInSafetyCenter()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // TODO: b/469390058 - Update the name of the launcher intent action when finalized.
        final List<ResolveInfo> resolved =
                mPackageManager.queryIntentActivities(
                        new Intent("com.android.binarytransparency.LAUNCHER")
                                .setPackage("com.google.attestation.verifier"),
                        PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && !resolved.isEmpty() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
