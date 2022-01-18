/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import static android.provider.SettingsSlicesContract.KEY_AIRPLANE_MODE;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.SettingsSlicesContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.qti.extphone.ExtTelephonyManager;

import java.io.IOException;

public class AirplaneModePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, OnDestroy,
        AirplaneModeEnabler.OnAirplaneModeChangedListener {

    public static final int REQUEST_CODE_EXIT_ECM = 1;
    public static final int REQUEST_CODE_EXIT_SCBM = 2;

    /**
     * Uri for Airplane mode Slice.
     */
    public static final Uri SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_AIRPLANE_MODE)
            .build();
    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    private Fragment mFragment;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;

    public AirplaneModePreferenceController(Context context, String key) {
        super(context, key);

        if (isAvailable(mContext)) {
            mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
        }
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    @VisibleForTesting
    void setAirplaneModeEnabler(AirplaneModeEnabler airplaneModeEnabler) {
        mAirplaneModeEnabler = airplaneModeEnabler;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AIRPLANE_MODE.equals(preference.getKey())) {
            if(mAirplaneModeEnabler.isInEcmMode()) {
                // In ECM mode launch ECM app dialog
                if (mFragment != null) {
                    mFragment.startActivityForResult(
                            new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_EXIT_ECM);
                }
                return true;
            } else if(mAirplaneModeEnabler.isInScbm()) {
                // In SCBM mode launch SCBM app dialog
                if (mFragment != null) {
                    mFragment.startActivityForResult(
                            new Intent(ExtTelephonyManager.ACTION_SHOW_NOTICE_SCM_BLOCK_OTHERS,
                            null), REQUEST_CODE_EXIT_SCBM);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Uri getSliceUri() {
        return SLICE_URI;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mAirplaneModePreference = screen.findPreference(getPreferenceKey());
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_show_toggle_airplane)
                && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        if (isAvailable()) {
            mAirplaneModeEnabler.start();
        }
    }

    @Override
    public void onStop() {
        if (isAvailable()) {
            mAirplaneModeEnabler.stop();
        }
    }

    @Override
    public void onDestroy() {
        mAirplaneModeEnabler.close();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            final boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInEmergencyMode(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        } else if (requestCode == REQUEST_CODE_EXIT_SCBM) {
            final boolean isChoiceYes = resultCode == Activity.RESULT_OK;
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInEmergencyMode(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }

    @Override
    public boolean isChecked() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked() == isChecked) {
            return false;
        }
        mAirplaneModeEnabler.setAirplaneMode(isChecked);
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        if (mAirplaneModePreference != null) {
            mAirplaneModePreference.setChecked(isAirplaneModeOn);
        }
    }

    /**
     * According to slice framework, need override this function and provide background
     * worker class to support slice's dynamic update.
     */
    @Override
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return AirplaneModeSliceWorker.class;
    }

    /**
     * Register content observer for URI Settings.Global.AIRPLANE_MODE_ON.
     * If changed, notify airplane mode slice do rebind.
     */
    public static class AirplaneModeSliceWorker extends SliceBackgroundWorker<Void> {
        private AirplaneModeContentObserver mContentObserver;

        public AirplaneModeSliceWorker(Context context, Uri uri) {
            super(context, uri);
            final Handler handler = new Handler(Looper.getMainLooper());
            mContentObserver = new AirplaneModeContentObserver(handler, this);
        }

        @Override
        protected void onSlicePinned() {
            mContentObserver.register(getContext());
        }

        @Override
        protected void onSliceUnpinned() {
            mContentObserver.unRegister(getContext());
        }

        @Override
        public void close() throws IOException {
            mContentObserver = null;
        }

        public void updateSlice() {
            notifySliceChange();
        }

        public class AirplaneModeContentObserver extends ContentObserver {
            private final AirplaneModeSliceWorker mSliceBackgroundWorker;

            public AirplaneModeContentObserver(Handler handler,
                                               AirplaneModeSliceWorker backgroundWorker) {
                super(handler);
                mSliceBackgroundWorker = backgroundWorker;
            }

            @Override
            public void onChange(boolean selfChange) {
                mSliceBackgroundWorker.updateSlice();
            }

            public void register(Context context) {
                final Uri airplaneModeUri = Settings.Global.getUriFor(
                        Settings.Global.AIRPLANE_MODE_ON);
                context.getContentResolver().registerContentObserver(airplaneModeUri,
                        false, this);
            }

            public void unRegister(Context context) {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }
}
