/*
 * Copyright (C) 2020 The AOSPA Project
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
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import com.android.settings.R;
import com.android.settings.Utils;

public class VibrationIntensityPreferenceController extends BasePreferenceController
            implements Preference.OnPreferenceClickListener {
    private static final String TAG = "VibrationIntensityPreferenceController";
    private static final String RING_VIBRATION_INTENSITY = "ring_vibration_intensity";
    private static final String NOTIFICATION_VIBRATION_INTENSITY = "notification_vibration_intensity";
    
    protected static FragmentManager mFragmentManager;
    
    private Vibrator mVibrator;
    private Preference mPreference;
    private Context mContext;
    private String mPreferenceKey;
    private boolean mIsRinger;
    private int mVibrationIntensity;

    private AudioAttributes mAudioAttributes;
    private final Handler mH = new Handler();

    private SettingsObserver mSettingObserver;

    protected static void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public VibrationIntensityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mPreferenceKey = preferenceKey;

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mIsRinger = mPreferenceKey.equals(RING_VIBRATION_INTENSITY);

        mSettingObserver = new SettingsObserver(mH);

        final AudioAttributes.Builder builder = new AudioAttributes.Builder();
        if (mIsRinger) {
            mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.RING_VIBRATION_INTENSITY),
                true, mSettingObserver, UserHandle.USER_CURRENT);
            builder.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
        } else {
            mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.NOTIFICATION_VIBRATION_INTENSITY),
                true, mSettingObserver, UserHandle.USER_CURRENT);
            builder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
        }
        mAudioAttributes = builder.build();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // or get preference.getKey() to select based on preference names
        if (mFragmentManager != null) {
            final VibrationIntensityDialog dialog = new VibrationIntensityDialog();
            dialog.setParameters(mContext, getPreferenceKey(), mPreference);
            dialog.show(mFragmentManager, TAG);
        }
        return true;
    }

    /**
     * Displays preference in this controller.
     */
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateVibrationIntensity();
        mPreference.setOnPreferenceClickListener(this);
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean SelfChange) {
            if (!SelfChange) {
                mVibrator.vibrate((mIsRinger) ? 500 : 250, mAudioAttributes);
            }
            updateVibrationIntensity();
        }
    }

    private void updateVibrationIntensity() {
        if (mIsRinger) {
            mVibrationIntensity = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        } else {
            mVibrationIntensity = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        }
        setText();
    }

    private void setText() {
        switch(mVibrationIntensity) {
            case 0:
                mPreference.setSummary("Disabled");
                break;
            case 1:
                mPreference.setSummary("Light");
                break;
            case 2:
                mPreference.setSummary("Medium");
                break;
            case 3:
                mPreference.setSummary("Strong");
                break;
            case 4:
                mPreference.setSummary("Custom");
                break;
        }
    }
}