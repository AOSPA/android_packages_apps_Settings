/*
 * Copyright (C) 2018 The Android Open Source Project
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

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.R;

/**
 * Dialog fragment to show dialog for "Backup Calling"
 *
 * When UE is in C_IWLAN-only mode, the preferred network type is not LTE, NR-only, or NR/LTE, and
 * the user tries to enable C_IWLAN, show a dialog to change preferred nw type.
 */
public class BackupCallingDialogFragment extends InstrumentedDialogFragment {

    private static final String LOG_TAG = "BackupCallingDialogFragment";
    private static final String ARG_PREF_TITLE = "pref_title";
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_SUB_ID = "subId";

    public static final int TYPE_ENABLE_CIWLAN_INCOMPATIBLE_NW_TYPE_DIALOG = 0;

    private String mPrefTitle;
    private int mType;
    private int mSubId;

    public static BackupCallingDialogFragment newInstance(String prefTitle, int type, int subId) {
        final BackupCallingDialogFragment dialogFragment = new BackupCallingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PREF_TITLE, prefTitle);
        args.putInt(ARG_DIALOG_TYPE, type);
        args.putInt(ARG_SUB_ID, subId);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        mPrefTitle = bundle.getString(ARG_PREF_TITLE).toLowerCase();
        mType = bundle.getInt(ARG_DIALOG_TYPE);
        mSubId = bundle.getInt(ARG_SUB_ID);

        switch (mType) {
            case TYPE_ENABLE_CIWLAN_INCOMPATIBLE_NW_TYPE_DIALOG:
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.preferred_nw_incompatible_ciwlan_nw_mode_dialog_title)
                        .setMessage(R.string.backup_calling_enable_dialog_incompatible_nw_type)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            default:
                throw new IllegalArgumentException("Unknown type " + mType);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BACKUP_CALLING_DIALOG;
    }
}