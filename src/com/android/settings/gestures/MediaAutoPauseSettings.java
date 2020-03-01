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

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

public class MediaAutoPauseSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "MediaAutoPauseSettings";

    private MediaAutoPauseEnabler mMediaAutoPauseEnabler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.media_autopause_settings);
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMediaAutoPauseEnabler != null) {
            mMediaAutoPauseEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        SettingsActivity activity = (SettingsActivity) getActivity();
        mMediaAutoPauseEnabler = new MediaAutoPauseEnabler(activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaAutoPauseEnabler != null) {
            mMediaAutoPauseEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaAutoPauseEnabler != null) {
            mMediaAutoPauseEnabler.pause();
        }
    }

    private class MediaAutoPauseEnabler implements SwitchBar.OnSwitchChangeListener {

        private final Context mContext;
        private final SwitchBar mSwitchBar;
        private boolean mListeningToOnSwitchChange;

        public MediaAutoPauseEnabler(SwitchBar switchBar) {
            mContext = switchBar.getContext();
            mSwitchBar = switchBar;

            mSwitchBar.show();

            boolean enabled = Settings.System.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.MEDIA_AUTO_PAUSE, 1, UserHandle.USER_CURRENT) != 0;
            mSwitchBar.setChecked(enabled);
        }

        public void teardownSwitchBar() {
            pause();
            mSwitchBar.hide();
        }

        public void resume() {
            if (!mListeningToOnSwitchChange) {
                mSwitchBar.addOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = true;
            }
        }

        public void pause() {
            if (mListeningToOnSwitchChange) {
                mSwitchBar.removeOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = false;
            }
        }

        @Override
        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            Settings.System.putIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.MEDIA_AUTO_PAUSE, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
        }

    }
}
