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

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import android.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settingslib.core.instrumentation.Instrumentable;

import com.android.settings.R;
import androidx.appcompat.app.AlertDialog;

public class GroupBluetoothProfileSwitchConfirmDialog extends InstrumentedDialogFragment {

    static final String TAG = "GroupBluetoothProfileSwitchConfirmDialog";
    private static final String KEY_GROUP_ID ="group_id";
    private int mGroupId = -1;
    private GroupUtils mGroupUtils;
    private BluetoothDetailsProfilesController mProfileController;

    public interface BluetoothProfileConfirmListener {

        void onDialogNegativeClick();

        void onDialogPositiveClick();
    }

    public static GroupBluetoothProfileSwitchConfirmDialog newInstance(int groupId) {
        Bundle args = new Bundle(1);
        args.putInt(KEY_GROUP_ID, groupId);
        GroupBluetoothProfileSwitchConfirmDialog dialog = new
                GroupBluetoothProfileSwitchConfirmDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return Instrumentable.METRICS_CATEGORY_UNKNOWN;
    }

    String getGroupTitle() {
        mGroupId = getArguments().getInt(KEY_GROUP_ID);
        return mGroupUtils.getGroupTitle(mGroupId);
    }

    @Override
    public Dialog onCreateDialog(Bundle inState) {
        DialogInterface.OnClickListener onConfirm = (dialog, which) -> {
            onPositiveButtonClicked();
            dialog.dismiss();
        };
        DialogInterface.OnClickListener onCancel = (dialog, which) -> {
            onNegativeButtonClicked();
            dialog.dismiss();
        };

        Context context = getContext();
        mGroupUtils = new GroupUtils(context);
        AlertDialog dialog = new AlertDialog.Builder(context)
        .setPositiveButton(R.string.group_confirm_dialog_apply_button, onConfirm)
        .setNegativeButton(android.R.string.cancel, onCancel).create();
        dialog.setTitle(R.string.group_apply_changes_dialog_title);
        dialog.setMessage(context.getString(R.string.group_confirm_dialog_body, getGroupTitle()));
        return dialog;
    }

    /**
     * Sets the controller that the fragment should use. this method MUST be called
     * before you try to show the dialog or an error will be thrown. An implementation
     * of a pairing controller can be found at {@link BluetoothPairingController}. A
     * controller may not be substituted once it is assigned. Forcibly switching a
     * controller for a new one will lead to undefined behavior.
     */
    void setPairingController(BluetoothDetailsProfilesController profileController) {
        if (isPairingControllerSet()) {
            throw new IllegalStateException("The controller can only be set once. "
                    + "Forcibly replacing it will lead to undefined behavior");
        }
        mProfileController = profileController;
    }

    /**
     * Checks whether mPairingController is set
     * @return True when mPairingController is set, False otherwise
     */
    boolean isPairingControllerSet() {
        return mProfileController != null;
    }

    private void onPositiveButtonClicked() {
        mProfileController.onDialogPositiveClick();
    }

    private void onNegativeButtonClicked() {
        mProfileController.onDialogNegativeClick();
    }
}
