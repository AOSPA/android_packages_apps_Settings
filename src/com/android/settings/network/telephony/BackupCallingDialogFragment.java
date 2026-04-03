// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
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
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
 */

/*
// QTI_END: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2022-2024 Qualcomm Innovation Center, Inc. All rights reserved.
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-11-27: Telephony: Fix NPE when trying to turn on C_IWLAN
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
// QTI_END: 2024-11-27: Telephony: Fix NPE when trying to turn on C_IWLAN
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev

/**
 * Dialog fragment to show dialog for "Backup Calling"
 *
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
 * When UE is in C_IWLAN-only mode, the preferred network type is not LTE, NR-only, or NR/LTE, and
 * the user tries to enable C_IWLAN, show a dialog to change preferred nw type.
// QTI_END: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
 */
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
public class BackupCallingDialogFragment extends InstrumentedDialogFragment {
// QTI_END: 2023-04-17: Telephony: Disable the C_IWLAN toggle during calls
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev

    private static final String LOG_TAG = "BackupCallingDialogFragment";
    private static final String ARG_DIALOG_TYPE = "dialog_type";

// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
    public static final int TYPE_NW_INCOMPATIBLE_ON_DDS_COMPATIBLE_ON_NDDS_ATTEMPT_DDS = 0;
    public static final int TYPE_NW_INCOMPATIBLE_ON_DDS_COMPATIBLE_ON_NDDS_ATTEMPT_NDDS = 1;
    public static final int TYPE_NW_COMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_DDS = 2;
    public static final int TYPE_NW_COMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_NDDS = 3;
    public static final int TYPE_NW_INCOMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_EITHER_SUB = 4;
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev

    private int mType;

// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-11-27: Telephony: Fix NPE when trying to turn on C_IWLAN
    public static BackupCallingDialogFragment newInstance(int type) {
// QTI_END: 2024-11-27: Telephony: Fix NPE when trying to turn on C_IWLAN
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        final BackupCallingDialogFragment dialogFragment = new BackupCallingDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, type);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();

        mType = bundle.getInt(ARG_DIALOG_TYPE);

// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        int dialogBodyTextId;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        switch (mType) {
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
            case TYPE_NW_INCOMPATIBLE_ON_DDS_COMPATIBLE_ON_NDDS_ATTEMPT_DDS:
                dialogBodyTextId =
                        R.string.ciwlan_dialog_nw_incompatible_dds_compatible_ndds_attempt_dds;
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                break;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
            case TYPE_NW_INCOMPATIBLE_ON_DDS_COMPATIBLE_ON_NDDS_ATTEMPT_NDDS:
                dialogBodyTextId =
                        R.string.ciwlan_dialog_nw_incompatible_dds_compatible_ndds_attempt_ndds;
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                break;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
            case TYPE_NW_COMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_DDS:
                dialogBodyTextId =
                        R.string.ciwlan_dialog_nw_compatible_dds_incompatible_ndds_attempt_dds;
                break;
            case TYPE_NW_COMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_NDDS:
                dialogBodyTextId =
                        R.string.ciwlan_dialog_nw_compatible_dds_incompatible_ndds_attempt_ndds;
                break;
            case TYPE_NW_INCOMPATIBLE_ON_DDS_INCOMPATIBLE_ON_NDDS_ATTEMPT_EITHER_SUB:
                dialogBodyTextId =
                        R.string.ciwlan_dialog_nw_incompatible_dds_incompatible_ndds_attempt_either;
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                break;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
            default:
                throw new IllegalArgumentException("Unknown type " + mType);
        }
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        return new AlertDialog.Builder(context)
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
                .setTitle(R.string.incompatible_pref_nw_ciwlan_dialog_title)
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                .setMessage(dialogBodyTextId)
                .setPositiveButton(android.R.string.ok, null)
                .create();
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BACKUP_CALLING_DIALOG;
    }
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
}
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
