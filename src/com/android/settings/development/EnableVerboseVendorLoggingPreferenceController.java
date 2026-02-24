/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.NoSuchElementException;

public class EnableVerboseVendorLoggingPreferenceController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "EnableVerboseVendorLoggingPreferenceController";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String ENABLE_VERBOSE_VENDOR_LOGGING_KEY = "enable_verbose_vendor_logging";
    private static final int DUMPSTATE_HAL_VERSION_UNKNOWN = -1;
    private static final int DUMPSTATE_HAL_VERSION_2_0 = 2; // AIDL v1

    private static final String DUMP_STATE_AIDL_SERVICE_NAME =
            android.hardware.dumpstate.IDumpstateDevice.DESCRIPTOR + "/default";

    private int mDumpstateHalVersion;

    public EnableVerboseVendorLoggingPreferenceController(Context context) {
        super(context);
        mDumpstateHalVersion = DUMPSTATE_HAL_VERSION_UNKNOWN;
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_VERBOSE_VENDOR_LOGGING_KEY;
    }

    @Override
    public boolean isAvailable() {
        // Only show preference when IDumpstateDevice AIDL service is available
        return isIDumpstateDeviceAidlServiceAvailable();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        // IDumpstateDevice IPC may be blocking when system is extremely heavily-loaded.
        // Post to background thread to avoid ANR. Ignore the returned Future.
        ThreadUtils.postOnBackgroundThread(() ->
                setVerboseLoggingEnabled(isEnabled));
        return true;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void updateState(Preference preference) {
        ThreadUtils.postOnBackgroundThread(() -> {
                    final boolean enabled = getVerboseLoggingEnabled();
                    ThreadUtils.getUiThreadHandler().post(() ->
                            ((TwoStatePreference) mPreference).setChecked(enabled));
                }
        );
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        ThreadUtils.postOnBackgroundThread(() ->
                setVerboseLoggingEnabled(false));
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    boolean isIDumpstateDeviceAidlServiceAvailable() {
        android.hardware.dumpstate.IDumpstateDevice aidlService = getDumpstateDeviceAidlService();
        return aidlService != null;
    }

    @VisibleForTesting
    void setVerboseLoggingEnabled(boolean enable) {
        // First check if AIDL service is available
        android.hardware.dumpstate.IDumpstateDevice aidlService = getDumpstateDeviceAidlService();
        if (aidlService != null) {
            try {
                aidlService.setVerboseLoggingEnabled(enable);
            } catch (RemoteException re) {
                if (DBG) Log.e(TAG, "aidlService.setVerboseLoggingEnabled fail: " + re);
            }
        }
    }

    @VisibleForTesting
    boolean getVerboseLoggingEnabled() {
        android.hardware.dumpstate.IDumpstateDevice aidlService = getDumpstateDeviceAidlService();
        if (aidlService != null) {
            try {
                return aidlService.getVerboseLoggingEnabled();
            } catch (RemoteException re) {
                if (DBG) Log.e(TAG, "aidlService.getVerboseLoggingEnabled fail: " + re);
            }
        }
        return false;
    }

    /**
     * Return a {@link android.hardware.dumpstate.IDumpstateDevice} instance or null if service is
     * not available.
     */
    @VisibleForTesting
    @Nullable android.hardware.dumpstate.IDumpstateDevice getDumpstateDeviceAidlService() {
        android.hardware.dumpstate.IDumpstateDevice service = null;
        try {
            service = android.hardware.dumpstate.IDumpstateDevice.Stub.asInterface(
                    ServiceManager.waitForDeclaredService(DUMP_STATE_AIDL_SERVICE_NAME));
        } catch (NoSuchElementException e) {
            if (DBG) Log.e(TAG, "Get AIDL service fail: " + e);
        }

        if (service != null) {
            mDumpstateHalVersion = DUMPSTATE_HAL_VERSION_2_0;
        }
        return service;
    }
}
