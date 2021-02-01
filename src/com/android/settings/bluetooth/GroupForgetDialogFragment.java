/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.android.settings.bluetooth;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import com.android.settings.R;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.Instrumentable;

/** Implements an AlertDialog for confirming that a user wishes to unpair or "forget"
*  a Group of devices*/
public class GroupForgetDialogFragment extends InstrumentedDialogFragment {

    private static final boolean DBG = ConnectedDeviceDashboardFragment.DBG_GROUP;
    protected static final String TAG = "GroupForgetDialogFragment";
    private static final String KEY_GROUPID = "groupid";
    private static List<CachedBluetoothDevice> mDevices ;
    private int mGroupId;
    private GroupUtils mGroupUtils;
    public static GroupForgetDialogFragment newInstance(int groupId) {
        Bundle args = new Bundle(1);
        args.putInt(KEY_GROUPID, groupId);
        GroupForgetDialogFragment dialog = new GroupForgetDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    String getGroupTitle() {
        mGroupId = getArguments().getInt(KEY_GROUPID);
        return mGroupUtils.getGroupTitle(mGroupId);
    }

    @Override
    public int getMetricsCategory() {
        return Instrumentable.METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    public Dialog onCreateDialog(Bundle inState) {
        DialogInterface.OnClickListener onConfirm = (dialog, which) -> {
            forget();
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        };
        Context context = getContext();
        mGroupUtils = new GroupUtils(context);
        AlertDialog dialog = new AlertDialog.Builder(context)
        .setPositiveButton(R.string.groupaudio_unpair_dialog_forget_confirm_button,
        onConfirm)
        .setNegativeButton(android.R.string.cancel, null)
        .create();
        dialog.setTitle(R.string.groupaudio_unpair_dialog_title);
        dialog.setMessage(context.getString(R.string.groupaudio_unpair_dialog_body,
                getGroupTitle()));
        return dialog;
    }

    private void forget() {
        mGroupUtils.forgetGroup(mGroupId);
    }
}
