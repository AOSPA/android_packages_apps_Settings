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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Controller that accesses and switches the preference status of
 * magnifying the nav bar and IME feature.
 */
public class MagnifyNavAndImePreferenceController extends MagnificationTogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {
    static final String PREF_KEY = "accessibility_magnify_nav_and_ime";

    private @Nullable Preference mPreference;

    @VisibleForTesting
    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    };

    public MagnifyNavAndImePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onResume() {
        MagnificationCapabilities.registerObserver(mContext, mContentObserver);
    }

    @Override
    public void onPause() {
        MagnificationCapabilities.unregisterObserver(mContext, mContentObserver);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableMagnificationMagnifyNavBarAndIme()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME,
                isChecked ? ON : OFF);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        @MagnificationMode int mode = MagnificationCapabilities.getCapabilities(mContext);
        preference.setEnabled(
                mode == MagnificationMode.FULLSCREEN || mode == MagnificationMode.ALL);

        @StringRes int resId = preference.isEnabled()
                ? R.string.accessibility_screen_magnification_nav_ime_summary
                : R.string.accessibility_screen_magnification_nav_ime_unavailable_summary;
        preference.setSummary(mContext.getString(resId));
    }
}
