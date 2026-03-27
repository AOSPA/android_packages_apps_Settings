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

package com.android.settings.network;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;

import com.android.settings.SidecarFragment;
import com.android.settings.network.EnableMultiSimSidecar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.system.ResetDashboardFragment;

public class EraseEuiccDataDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, SidecarFragment.Listener {

    public static final String TAG = "EraseEuiccDataDlg";
    private static final String PACKAGE_NAME_EUICC_DATA_MANAGEMENT_CALLBACK =
            "com.android.settings.network";

    private static final int NUM_OF_SIMS_FOR_DSDS = 2;
    private EnableMultiSimSidecar mEnableMultiSimSidecar;
    private TelephonyManager mTelephonyManager;
    private Context mAppContext;

    public static void show(ResetDashboardFragment host) {
        if (host.getActivity() == null) {
            return;
        }
        final EraseEuiccDataDialogFragment dialog = new EraseEuiccDataDialogFragment();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        dialog.show(manager, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_EUICC;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mTelephonyManager = getContext() != null ? getContext().
                getSystemService(TelephonyManager.class) : null;
        if (getActivity() != null) {
            mEnableMultiSimSidecar = EnableMultiSimSidecar.get(getActivity().getFragmentManager());
        }

        // {{ changed: keep a reference and override positive button click }}
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.reset_esim_title)
            .setMessage(R.string.reset_esim_desc)
            .setPositiveButton(R.string.erase_sim_confirm_button, null)
            .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
            .create();

    alertDialog.setCanceledOnTouchOutside(false);

    alertDialog.setOnShowListener(dlg -> {
        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(v -> {
            onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
            // do NOT dismiss here; dismiss only after SUCCESS/ERROR or after starting wipe
        });
    });

    return alertDialog;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
        if (mEnableMultiSimSidecar != null) {
            mEnableMultiSimSidecar.addListener(this);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        if (mEnableMultiSimSidecar != null) {
            mEnableMultiSimSidecar.removeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Fragment fragment = getTargetFragment();
        if (!(fragment instanceof ResetDashboardFragment)) {
            Log.e(TAG, "getTargetFragment return unexpected type");
        }

        if (which == DialogInterface.BUTTON_POSITIVE && mTelephonyManager != null &&
                mTelephonyManager.getCardIdForDefaultEuicc() !=
                TelephonyManager.UNINITIALIZED_CARD_ID) {
            Context context = getContext();
            if (context == null) return;
            mAppContext = context.getApplicationContext();

            // If already DSDS, proceed as before.
            if (mTelephonyManager.isMultiSimEnabled()) {
                MobileNetworkUtils.showLockScreen(mAppContext, () -> runAsyncWipe());
                dismissAllowingStateLoss();
                return;
            }
            // Not DSDS: request DSDS first, then continue in onStateChange(SUCCESS).
            if (mEnableMultiSimSidecar != null ) {
                Log.i(TAG, "DSDS not enabled; enabling DSDS before eUICC wipe");
                mEnableMultiSimSidecar.run(NUM_OF_SIMS_FOR_DSDS);
            } else {
                Log.e(TAG, "EnableMultiSimSidecar is null; cannot enable DSDS");
            }
        } else {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (!(fragment instanceof EnableMultiSimSidecar)) return;

        EnableMultiSimSidecar sidecar = (EnableMultiSimSidecar) fragment;
        int state = sidecar.getState();

        if (state == SidecarFragment.State.SUCCESS) {
            Log.i(TAG, "DSDS enabled successfully; proceeding with eUICC wipe");
            sidecar.reset();
            if (mAppContext == null) return;
            MobileNetworkUtils.showLockScreen(mAppContext, () -> runAsyncWipe());

        // {{ added: now it's safe to close the dialog }}
        dismissAllowingStateLoss();
        } else if (state == SidecarFragment.State.ERROR) {
            Log.e(TAG, "Failed to enable DSDS; cannot proceed with eUICC wipe");
            sidecar.reset();
        }
    }

    private void runAsyncWipe() {
        Runnable runnable = (new ResetNetworkOperationBuilder(mAppContext))
                .resetEsim(PACKAGE_NAME_EUICC_DATA_MANAGEMENT_CALLBACK)
                .build();
        AsyncTask.execute(runnable);
    }
}
