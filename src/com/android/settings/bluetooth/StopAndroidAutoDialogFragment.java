
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

package com.android.settings.bluetooth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class StopAndroidAutoDialogFragment extends InstrumentedDialogFragment {
    public static final String TAG = "StopAndroidAutoDialog";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_POSITIVE_BUTTON_TEXT = "positive_button_text";

    public interface StopAndroidAutoDialogListener {
        void onDialogConfirmed(String deviceAddress);
        void onDialogCanceled();
    }

    public static StopAndroidAutoDialogFragment newInstance(String deviceAddress, String title,
            String message, String positiveButtonText) {
        final Bundle args = new Bundle();
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putString(KEY_POSITIVE_BUTTON_TEXT, positiveButtonText);
        final StopAndroidAutoDialogFragment fragment = new StopAndroidAutoDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String deviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        final String title = getArguments().getString(KEY_TITLE);
        final String message = getArguments().getString(KEY_MESSAGE);
        final String positiveButtonText = getArguments().getString(KEY_POSITIVE_BUTTON_TEXT);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText,
                        (dialog, which) -> {
                            final StopAndroidAutoDialogListener listener = getListener();
                            if (listener != null) {
                                listener.onDialogConfirmed(deviceAddress);
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                            final StopAndroidAutoDialogListener listener = getListener();
                            if (listener != null) {
                                listener.onDialogCanceled();
                            }
                        });

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final StopAndroidAutoDialogListener listener = getListener();
        if (listener != null) {
            listener.onDialogCanceled();
        }
    }

    @Nullable
    private StopAndroidAutoDialogListener getListener() {
        final Fragment fragment = getTargetFragment();
        if (fragment instanceof StopAndroidAutoDialogListener) {
            return (StopAndroidAutoDialogListener) fragment;
        }
        return null;
    }

    @Override
    public int getMetricsCategory() {
        // This should be a new enum value for metrics logging
        // TODO: weishengsu - Add a new enum value for metrics logging in
        // frameworks/proto_logging/stats/enums/app/settings/settings_enums.proto
        // and also be manually synced to g3.
        return 0;
    }
}
