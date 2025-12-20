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
package com.android.settings.notification.modes;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

/** Preference controller for dnd sync toggle. */
public class ZenModeDndSyncPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    ZenModeDndSyncPreferenceController(Context context, String key, ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        ((SwitchPreferenceCompat) preference)
                .setChecked(requireNonNull(mBackend).isModeSyncEnabled());
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return android.service.notification.Flags.enableDndSync()
                && zenMode.isManualDnd()
                && zenMode.isEnabled()
                && requireNonNull(mBackend).isModeSyncSupported();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        requireNonNull(mBackend).setModeSyncEnabled(enabled);
        return true;
    }
}
