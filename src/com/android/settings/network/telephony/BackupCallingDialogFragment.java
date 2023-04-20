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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.R;

/**
 * Dialog fragment to show dialog for "Backup Calling"
 *
 * 1. When IMS is registered over C_IWLAN-only mode, device is in a call, and user tries to disable
 *    C_IWLAN, show a dialog to confirm.
 * 2. When UE is in C_IWLAN-only mode, the preferred network type is not LTE, NR-only, or NR/LTE,
 *    and the user tries to enable C_IWLAN, show a dialog to change preferred nw type.
 */
public class BackupCallingDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String LOG_TAG = "BackupCallingDialogFragment";
    private static final String ARG_PREF_TITLE = "pref_title";
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_SUB_ID = "subId";

    public static final int TYPE_DISABLE_CIWLAN_DIALOG = 0;
    public static final int TYPE_ENABLE_CIWLAN_INCOMPATIBLE_NW_TYPE_DIALOG = 1;

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
            case TYPE_DISABLE_CIWLAN_DIALOG:
                return new AlertDialog.Builder(context)
                        .setTitle(context.getString(
                                R.string.toggle_disabling_ciwlan_call_dialog_title, mPrefTitle))
                        .setMessage(context.getString(
                                R.string.toggle_disabling_ciwlan_call_dialog_body, mPrefTitle))
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (mType) {
            case TYPE_DISABLE_CIWLAN_DIALOG:
                disableCiwlan();
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + mType);
        }
    }

    private void disableCiwlan() {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
        if (imsMmTelMgr == null) {
            Log.e(LOG_TAG, "imsMmTelMgr null");
            return;
        }
        try {
            imsMmTelMgr.setCrossSimCallingEnabled(false);
        } catch (ImsException exception) {
            Log.e(LOG_TAG, "Failed to disable cross SIM calling", exception);
        }
    }

    private ImsMmTelManager getImsMmTelManager() {
        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return null;
        }
        ImsManager imsMgr = getContext().getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(mSubId);
    }
}