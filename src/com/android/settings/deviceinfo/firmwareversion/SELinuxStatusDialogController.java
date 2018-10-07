/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SELinux;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.widget.TextView;

import com.android.settings.R;

public class SELinuxStatusDialogController {

    @VisibleForTesting
    private static final int SELINUX_STATUS_LABEL_ID = R.id.selinux_status_label;
    @VisibleForTesting
    private static final int SELINUX_STATUS_VALUE_ID = R.id.selinux_status_value;

    private final FirmwareVersionDialogFragment mDialog;
    private final Context mContext;

    public SELinuxStatusDialogController(FirmwareVersionDialogFragment dialog) {
	    mDialog = dialog;
	    mContext = mDialog.getContext();
    }

    public void displaySELinuxInfo() {
        if (!SELinux.isSELinuxEnabled()) {
            mDialog.setText(SELINUX_STATUS_VALUE_ID, (CharSequence) mContext.getResources().getString(R.string.selinux_status_disabled));
        } else if (!SELinux.isSELinuxEnforced()) {
            mDialog.setText(SELINUX_STATUS_VALUE_ID, (CharSequence) mContext.getResources().getString(R.string.selinux_status_permissive));
        } else {
            mDialog.setText(SELINUX_STATUS_VALUE_ID, (CharSequence) mContext.getResources().getString(R.string.selinux_status_enforcing));
        }
    }
     /**
     * Populates the SELinux info field.
     */
    public void initialize() {
        displaySELinuxInfo();
    }
}

