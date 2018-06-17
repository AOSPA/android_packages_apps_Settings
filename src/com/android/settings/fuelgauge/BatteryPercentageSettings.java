/*
 * Copyright (C) 2018 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.SettingPref;
import com.android.settings.widget.SwitchBar;

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

public class BatteryPercentageSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener {

    private static final String TAG = "BatteryPercentageSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String KEY_BATTERY_PERCENTAGE_STYLE = "battery_percentage_style";
    private static final long WAIT_FOR_SWITCH_ANIM = 500;

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);

    SwitchBar mSwitchBar;
    private Context mContext;
    private boolean mCreated;
    private SettingPref mBatteryPercentageStylePref;
    private Switch mSwitch;
    private boolean mValidListener;

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            mSwitchBar.show();
            return;
        }
        mCreated = true;
        addPreferencesFromResource(R.xml.battery_percentage_settings);
        mContext = getActivity();
        mSwitchBar = ((SettingsActivity) mContext).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();

        mBatteryPercentageStylePref = new SettingPref(SettingPref.TYPE_SYSTEM,
                KEY_BATTERY_PERCENTAGE_STYLE, SHOW_BATTERY_PERCENT, 0,
                getResources().getIntArray(R.array.battery_percentage_values)) {
            @Override
            protected String getCaption(Resources res, int value) {
                CharSequence[] labels = getResources().getTextArray(
                        R.array.battery_percentage_entries);
                return labels[value].toString();
            }
        };
        mBatteryPercentageStylePref.init(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.setListening(true);
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        updateSwitch();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.setListening(false);
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mHandler.removeCallbacks(mStartMode);
        if (isChecked) {
            mHandler.postDelayed(mStartMode, WAIT_FOR_SWITCH_ANIM);
        } else {
            trySettingBatteryPercentage(false);
        }
    }

    private void trySettingBatteryPercentage(boolean mode) {
        if (Settings.System.putInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT,
                mode ? 1 : 0)) {
            mHandler.post(mUpdateSwitch);
        }
    }

    private void updateSwitch() {
        final boolean mode = Settings.System.getInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT, 0) != 0;
        if (DEBUG) Log.d(TAG, "updateSwitch: isChecked=" + mSwitch.isChecked() + " mode=" + mode);
        if (mode == mSwitch.isChecked()) return;

        // set listener to null so that that code below doesn't trigger onCheckedChanged()
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        mSwitch.setChecked(mode);
        if (mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private final Runnable mUpdateSwitch = new Runnable() {
        @Override
        public void run() {
            updateSwitch();
        }
    };

    private final Runnable mStartMode = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    trySettingBatteryPercentage(true);
                }
            });
        }
    };

    private final class SettingsObserver extends ContentObserver {
        private final Uri SHOW_BATTERY_PERCENT_URI =
                Settings.System.getUriFor(SHOW_BATTERY_PERCENT);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (SHOW_BATTERY_PERCENT_URI.equals(uri)) {
                mBatteryPercentageStylePref.update(mContext);
                updateSwitch();
            }
        }

        public void setListening(boolean listening) {
            final ContentResolver cr = getContentResolver();
            if (listening)
                cr.registerContentObserver(SHOW_BATTERY_PERCENT_URI, false, this);
            else
                cr.unregisterContentObserver(this);
        }
    }
}
