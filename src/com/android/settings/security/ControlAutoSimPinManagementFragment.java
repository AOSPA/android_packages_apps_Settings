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

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Fragment for SIM PIN management features (starting with automatic SIM PIN management, manual
 * SIM PIN management will be migrated to this fragment as well).
 *
 * This is the container fragment for showing the automatically-generated SIM PIN.
 */
public class ControlAutoSimPinManagementFragment extends DashboardFragment {
    private static final String TAG = ControlAutoSimPinManagementFragment.class.getSimpleName();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(ShowAutoManagedSimPinController.class).setFragment(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.sim_protection_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUTOMATIC_SIM_PIN_MANAGEMENT;
    }
}
