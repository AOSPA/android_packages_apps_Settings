/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony.networkchange;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class NetworkProtectionDialogFragment extends InstrumentedDialogFragment {

    private static final String TAG = "NetworkProtectionDialogFragment";
    private static final String DIALOG_DESC_BUNDLE_KEY = "arg_key_dialog_desc";
    private static final String DIALOG_TITLE_BUNDLE_KEY = "arg_key_dialog_title";
    private static final String DIALOG_POSITIVE_BTN_TXT_BUNDLE_KEY = "arg_key_dialog_positive_btn";

    private final OnClickListener mBtnListener;

    private NetworkProtectionDialogFragment(OnClickListener listener) {
        mBtnListener = listener;
    }

    /**
     * Start the dialog and display it on screen.
     */
    public static void show(@NonNull Fragment host, @NonNull String dialogTitle,
                            @NonNull String dialogDesc, @NonNull String positiveBtnTxt,
                            OnClickListener listener) {
        show(host.getChildFragmentManager(), dialogTitle, dialogDesc, positiveBtnTxt, listener);
    }

    /**
     * Start the dialog and display it on screen.
     */
    public static void show(@NonNull FragmentManager childFragmentManager,
                            @NonNull String dialogTitle, @NonNull String dialogDesc,
                            @NonNull String positiveBtnTxt, OnClickListener listener) {
        if (childFragmentManager.findFragmentByTag(TAG) == null) {
            final Bundle bundle = new Bundle();
            bundle.putString(DIALOG_TITLE_BUNDLE_KEY, dialogTitle);
            bundle.putString(DIALOG_DESC_BUNDLE_KEY, dialogDesc);
            bundle.putString(DIALOG_POSITIVE_BTN_TXT_BUNDLE_KEY, positiveBtnTxt);
            final NetworkProtectionDialogFragment dialog =
                    new NetworkProtectionDialogFragment(listener);
            dialog.setArguments(bundle);
            dialog.show(childFragmentManager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_NETWORK_PROTECTION;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final String dialogTitle = bundle.getString(DIALOG_TITLE_BUNDLE_KEY);
        final String dialogDesc = bundle.getString(DIALOG_DESC_BUNDLE_KEY);
        final String positiveBtnTxt = bundle.getString(DIALOG_POSITIVE_BTN_TXT_BUNDLE_KEY);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle).setMessage(dialogDesc)
                .setPositiveButton(positiveBtnTxt, mBtnListener)
                .setNegativeButton(R.string.cancel, mBtnListener);
        AlertDialog dialog = builder.create();
        setCancelable(false);
        return dialog;
    }
}
