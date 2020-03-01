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

import static android.provider.Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.HashMap;
import java.util.Map;

public class AdaptivePlaybackPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, RadioButtonPreference.OnClickListener {

    private static final Map<String, Integer> sTimeoutKeyToValueMap = new HashMap<>();

    private final String mPreferenceKey;
    private OnChangeListener mOnChangeListener;
    private RadioButtonPreference mPreference;

    public AdaptivePlaybackPreferenceController(Context context, String preferenceKey) {
        super(context);

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
        if (isAvailable()) {
            mPreference = (RadioButtonPreference) screen.findPreference(getPreferenceKey());
            mPreference.setOnClickListener(this);
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int value = sTimeoutKeyToValueMap.get(mPreferenceKey);
        Settings.System.putIntForUser(mContext.getContentResolver(), ADAPTIVE_PLAYBACK_TIMEOUT,
                value, UserHandle.USER_CURRENT);
        if (mOnChangeListener != null) {
            mOnChangeListener.onCheckedChanged(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        int preferenceValue = sTimeoutKeyToValueMap.get(mPreference.getKey());
        int timeoutValue = Settings.System.getIntForUser(mContext.getContentResolver(),
                ADAPTIVE_PLAYBACK_TIMEOUT, 0, UserHandle.USER_CURRENT);
        mPreference.setChecked(timeoutValue == preferenceValue);
    }

    public void setPreferenceEnabled(boolean enabled) {
        mPreference.setEnabled(enabled);
    }

    public interface OnChangeListener {
        void onCheckedChanged(Preference preference);
    }
}
