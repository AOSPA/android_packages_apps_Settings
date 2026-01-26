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
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.settings.R;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Dialog for entering the current SIM card PIN. Used when the user wants to change
 * the SIM card's PIN.
 */
public class EnterCurrentSimPinDialogFragment extends DialogFragment {
    private static final String TAG = "SimPinDialog";

    static EnterCurrentSimPinDialogFragment newInstance() {
        return new EnterCurrentSimPinDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.provide_current_sim_pin_title)
                .setMessage(R.string.provide_current_sim_pin)
                .setCancelable(true)
                .setPositiveButton(R.string.sim_enter_ok,
                        (dialog, which) -> invokePinEnteredCallback())
                .setNeutralButton(R.string.use_carrier_default_pin,
                        (dialog, which) -> populateWithCarrierDefault())
                .setNegativeButton(com.android.internal.R.string.cancel,
                        (dialog, which) -> invokeCancelCallback());

        builder.setView(R.layout.dialog_provide_sim_pin_entry);
        return builder.create();
    }

    private void populateWithCarrierDefault() {
        EditText pin = getDialog().findViewById(R.id.current_sim_pin);
        // TODO(b/430027795): Implement and wire database of default carrier SIM PINs.
        pin.setText("0000");
    }

    @Nullable
    private AutomaticSimPinLockFragment getSimPinLockFragment() {
        Fragment parent = getParentFragment();
        if (parent == null) {
            Log.w(TAG, "No parent fragment, entered PIN will have no impact.");
            return null;
        }
        if (!(parent instanceof AutomaticSimPinLockFragment)) {
            Log.w(TAG, "Parent fragment is not the expected fragment: " + parent.getClass());
            return null;
        }

        return (AutomaticSimPinLockFragment) parent;
    }

    private void invokePinEnteredCallback() {
        EditText pinInput = getDialog().findViewById(R.id.current_sim_pin);

        String pin = ((TextView) pinInput).getText().toString();
        dismiss();
        AutomaticSimPinLockFragment parent = getSimPinLockFragment();
        if (parent != null) {
            parent.onPinEntered(pin);
        }
    }

    private void invokeCancelCallback() {
        dismiss();
        AutomaticSimPinLockFragment parent = getSimPinLockFragment();
        if (parent != null) {
            parent.onEnrollmentCancelled();
        }
    }
}
