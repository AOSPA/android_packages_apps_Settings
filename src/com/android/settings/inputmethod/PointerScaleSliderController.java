/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.PointerIcon.DEFAULT_POINTER_SCALE;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SliderPreference;


public class PointerScaleSliderController extends SliderPreferenceController
        implements DefaultLifecycleObserver {

    private final int mProgressMin;
    private final int mProgressMax;
    private final float mScaleMin;
    private final float mScaleMax;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    private final @NonNull ContentResolver mContentResolver;
    private @Nullable SliderPreference mPreference;

    static final Uri POINTER_SCALE_URI = Settings.System.getUriFor(Settings.System.POINTER_SCALE);

    final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mPreference == null || uri == null) {
                        return;
                    }
                    updateState(mPreference);
                }
            };

    public PointerScaleSliderController(@NonNull Context context, @NonNull String key) {
        super(context, key);

        Resources res =  context.getResources();
        mProgressMin = res.getInteger(R.integer.pointer_scale_seek_bar_start);
        mProgressMax = res.getInteger(R.integer.pointer_scale_seek_bar_end);
        mScaleMin = res.getFloat(R.dimen.pointer_scale_size_start);
        mScaleMax = res.getFloat(R.dimen.pointer_scale_size_end);

        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

        mContentResolver = context.getContentResolver();
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return android.view.flags.Flags.enableVectorCursorA11ySettings() ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            return;
        }
        mPreference.setUpdatesContinuously(true);
        updateState(mPreference);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mContentResolver.registerContentObserver(
                POINTER_SCALE_URI,
                /* notifyForDescendants= */ false,
                mSettingsObserver);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public int getSliderPosition() {
        return scaleToProgress(Settings.System.getFloatForUser(mContext.getContentResolver(),
                Settings.System.POINTER_SCALE, DEFAULT_POINTER_SCALE,
                UserHandle.USER_CURRENT));
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }

        float value = progressToScale(position);
        Settings.System.putFloatForUser(mContentResolver,
                Settings.System.POINTER_SCALE, value,
                UserHandle.USER_CURRENT);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_POINTER_ICON_SCALE_CHANGED,
                Float.toString(value));
        return true;
    }

    private float progressToScale(int progress) {
        return (((progress - mProgressMin) * (mScaleMax - mScaleMin)) / (mProgressMax
                - mProgressMin)) + mScaleMin;
    }

    private int scaleToProgress(float scale) {
        return (int) (
                (((scale - mScaleMin) * (mProgressMax - mProgressMin)) / (mScaleMax - mScaleMin))
                        + mProgressMin);
    }

    @Override
    public int getMin() {
        return mProgressMin;
    }

    @Override
    public int getMax() {
        return mProgressMax;
    }
}
