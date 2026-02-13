/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.FloatRange;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.SliderPreference;

import java.text.NumberFormat;
import java.util.Locale;

/** Preference controller that controls the transparency slider in accessibility button page. */
public class FloatingMenuTransparencyPreferenceController extends SliderPreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    @VisibleForTesting
    @FloatRange(from = 0.0, to = 1.0)
    static final float DEFAULT_TRANSPARENCY = 0.45f;
    @VisibleForTesting
    static final float MAXIMUM_TRANSPARENCY = 1.0f;
    private static final int FADE_ENABLED = 1;
    private static final float MIN_PROGRESS = 0f;
    private static final float MAX_PROGRESS = 90f;
    @VisibleForTesting
    static final float PRECISION = 100f;

    private final ContentResolver mContentResolver;
    private Locale mCachedLocale;
    private NumberFormat mPercentFormat;
    @VisibleForTesting
    final ContentObserver mContentObserver;
    private SliderPreference mPreference;

    public FloatingMenuTransparencyPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateAvailabilityStatus();
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        return AccessibilityUtil.isFloatingMenuEnabled(mContext)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setUpdatesContinuously(true);
        mPreference.setMax(getMax());
        mPreference.setMin(getMin());
        mPreference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
        updateAvailabilityStatus();
        updateState(mPreference);
        mPreference.setSliderStateDescription(
                formatStateDescription(convertTransparencyIntToFloat(mPreference.getValue())));
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_BUTTON_MODE), /* notifyForDescendants= */
                false, mContentObserver);
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED),
                        /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    @Override
    public int getSliderPosition() {
        return convertTransparencyFloatToInt(getTransparency());
    }

    @Override
    public boolean setSliderPosition(int position) {
        final float transparencyValue = convertTransparencyIntToFloat(position);
        final float opacityValue = MAXIMUM_TRANSPARENCY - transparencyValue;
        if (mPreference != null) {
            mPreference.setSliderStateDescription(formatStateDescription(transparencyValue));
        }
        return Settings.Secure.putFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, opacityValue);
    }

    @Override
    public int getMax() {
        return (int) MAX_PROGRESS;
    }

    @Override
    public int getMin() {
        return (int) MIN_PROGRESS;
    }

    private void updateAvailabilityStatus() {
        final boolean fadeEnabled = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED, FADE_ENABLED)
                == FADE_ENABLED;

        mPreference.setEnabled(AccessibilityUtil.isFloatingMenuEnabled(mContext) && fadeEnabled);
        mPreference.setSliderStateDescription(formatStateDescription(mPreference.getValue()));
    }

    private int convertTransparencyFloatToInt(float value) {
        return Math.round(value * PRECISION);
    }

    private float convertTransparencyIntToFloat(int value) {
        return (float) value / PRECISION;
    }

    private float getTransparency() {
        float transparencyValue = MAXIMUM_TRANSPARENCY - (Settings.Secure.getFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, DEFAULT_TRANSPARENCY));
        final float minValue = MIN_PROGRESS / PRECISION;
        final float maxValue = MAX_PROGRESS / PRECISION;

        return (transparencyValue < minValue || transparencyValue > maxValue)
                ? DEFAULT_TRANSPARENCY : transparencyValue;
    }

    private CharSequence formatStateDescription(float percentage) {
        if (!mPreference.isEnabled() && Flags.floatingMenuTransparencySliderAnnouncesDisabled()) {
            return mContext.getString(com.android.settingslib.R.string.disabled);
        }

        // Cache the locale-appropriate NumberFormat.  Configuration locale is guaranteed
        // non-null, so the first time this is called we will always get the appropriate
        // NumberFormat, then never regenerate it unless the locale changes on the fly.
        final Locale curLocale = mContext.getResources().getConfiguration().getLocales().get(0);
        if (!curLocale.equals(mCachedLocale)) {
            mCachedLocale = curLocale;
            mPercentFormat = NumberFormat.getPercentInstance(curLocale);
        }
        return mPercentFormat.format(percentage);
    }
}
