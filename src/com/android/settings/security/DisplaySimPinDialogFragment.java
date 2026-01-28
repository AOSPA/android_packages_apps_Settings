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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;


/**
 * Dialog fragment for showing the generated SIM PIN to the user (the user needs this PIN
 * to use the SIM on other devices).
 */
public class DisplaySimPinDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {
    private boolean mSuccess;
    private String mPin;

    static DisplaySimPinDialogFragment newInstance(boolean success, String pin) {
        DisplaySimPinDialogFragment f = new DisplaySimPinDialogFragment();

        Bundle args = new Bundle();
        args.putBoolean("success", success);
        args.putString("pin", pin);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mSuccess = args.getBoolean("success");
        mPin = args.getString("pin");
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String message = null;
        if (mSuccess) {
            message = getContext().getResources().getString(R.string.sim_platform_managed_pin_value)
                    + " " + mPin;
        } else {
            message = getContext().getResources().getString(
                    R.string.sim_platform_managed_pin_failure);
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sim_display_platform_managed_pin)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(com.android.internal.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dismiss();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUTOMATIC_SIM_PIN_MANAGEMENT;
    }
}
