/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Fragment for showing the PrimarySwitchPreference toggle for toggling SIM protection
 * (in general) on/off.
 */
public class AutomaticSimPinLockFragment extends DashboardFragment {
    private static final String TAG = "AutoSimPinLockFrg";

    @Nullable
    private AutoSimPinManagementController mController;

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mController = use(AutoSimPinManagementController.class);
        mController.setFragment(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.automatic_sim_lock_protection_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUTOMATIC_SIM_PIN_MANAGEMENT;
    }

    /**
     * To be called by the SIM PIN entry dialog to proceed with enrollment after the user
     * has provided the PIN.
     *
     * @param pin The current SIM PIN as provided by the user.
     */
    public void onPinEntered(String pin) {
        if (mController == null) {
            Log.w(TAG, "Controller is not initialized, cannot process PIN");
            return;
        }
        mController.tryEnrollingToAutoPinManagement(pin);
    }

    /**
     * To be called by the SIM PIN entry dialog when the user cancels the enrollment.
     */
    public void onEnrollmentCancelled() {
        if (mController != null) {
            mController.cancelEnrollment();
        }
    }
}
