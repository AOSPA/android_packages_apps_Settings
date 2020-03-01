/*
 * Copyright (C) 2020 Paranoid Android
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

package com.android.settings.gestures;

import static android.provider.Settings.System.ADAPTIVE_PLAYBACK;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller class that control adaptive playback time out settings.
 */
public class AdaptivePlaybackController extends AbstractPreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener {

    private static final String TAG = "AdaptivePlaybackController";

    // pair the preference key and timeout value.
    private static final Map<String, Integer> sTimeoutKeyToValueMap = new HashMap<>();

    private final String mPreferenceKey;
    private final ContentResolver mContentResolver;
    private OnChangeListener mOnChangeListener;
    private RadioButtonPreference mPreference;

    public AdaptivePlaybackController(Context context, Lifecycle lifecycle,
            String preferenceKey) {
        super(context);

        mContentResolver = context.getContentResolver();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mPreferenceKey = preferenceKey;

        if (sTimeoutKeyToValueMap.size() == 0) {
            Resources res = context.getResources();
            String[] timeoutKeys = res.getStringArray(
                        R.array.adaptive_playback_timeout_keys);

            int[] timeoutValues = res.getIntArray(
                    R.array.adaptive_playback_timeout_values);

            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                sTimeoutKeyToValueMap.put(timeoutKeys[i], timeoutValues[i]);
            }
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (RadioButtonPreference)
                screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int value = sTimeoutKeyToValueMap.get(mPreferenceKey);
        Settings.System.putIntForUser(mContentResolver, ADAPTIVE_PLAYBACK, value,
                UserHandle.USER_CURRENT);
        if (mOnChangeListener != null) {
            mOnChangeListener.onCheckedChanged(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        // reset RadioButton
        mPreference.setChecked(false);

        // check this preference if needed
        int preferenceValue = sTimeoutKeyToValueMap.get(mPreference.getKey());
        int timeoutValue = Settings.System
                .getIntForUser(mContentResolver, ADAPTIVE_PLAYBACK, 0, UserHandle.USER_CURRENT);
        if (timeoutValue == preferenceValue) {
            mPreference.setChecked(true);
        }
    }

    /**
     * Listener interface handles checked event.
     */
    public interface OnChangeListener {
        /**
         * A hook that is called when preference checked.
         */
        void onCheckedChanged(Preference preference);
    }
}
