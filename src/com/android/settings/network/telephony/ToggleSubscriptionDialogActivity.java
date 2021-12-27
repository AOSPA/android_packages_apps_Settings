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

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.EnableMultiSimSidecar;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SwitchToEuiccSubscriptionSidecar;
import com.android.settings.network.SwitchToRemovableSlotSidecar;
import com.android.settings.network.UiccSlotUtil;
import com.android.settings.sim.SimActivationNotifier;

import com.google.common.collect.ImmutableList;

import java.util.List;

/** This dialog activity handles both eSIM and pSIM subscriptions enabling and disabling. */
public class ToggleSubscriptionDialogActivity extends SubscriptionActionDialogActivity
        implements SidecarFragment.Listener, ConfirmDialogFragment.OnConfirmListener {

    private static final String TAG = "ToggleSubscriptionDialogActivity";
    // Arguments
    @VisibleForTesting
    public static final String ARG_enable = "enable";
    // Dialog tags
    private static final int DIALOG_TAG_DISABLE_SIM_CONFIRMATION = 1;
    private static final int DIALOG_TAG_ENABLE_SIM_CONFIRMATION = 2;
    private static final int DIALOG_TAG_ENABLE_DSDS_CONFIRMATION = 3;
    private static final int DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION = 4;
    // Number of SIMs for DSDS
    private static final int NUM_OF_SIMS_FOR_DSDS = 2;

    /**
     * Returns an intent of ToggleSubscriptionDialogActivity.
     *
     * @param context The context used to start the ToggleSubscriptionDialogActivity.
     * @param subId The subscription ID of the subscription needs to be toggled.
     * @param enable Whether the activity should enable or disable the subscription.
     */
    public static Intent getIntent(Context context, int subId, boolean enable) {
        Intent intent = new Intent(context, ToggleSubscriptionDialogActivity.class);
        intent.putExtra(ARG_SUB_ID, subId);
        intent.putExtra(ARG_enable, enable);
        return intent;
    }

    private SubscriptionInfo mSubInfo;
    private SwitchToEuiccSubscriptionSidecar mSwitchToEuiccSubscriptionSidecar;
    private SwitchToRemovableSlotSidecar mSwitchToRemovableSlotSidecar;
    private EnableMultiSimSidecar mEnableMultiSimSidecar;
    private boolean mEnable;
    private boolean mIsEsimOperation;
    private TelephonyManager mTelMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int subId = intent.getIntExtra(ARG_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mTelMgr = getSystemService(TelephonyManager.class);

        UserManager userManager = getSystemService(UserManager.class);
        if (!userManager.isAdminUser()) {
            Log.e(TAG, "It is not the admin user. Unable to toggle subscription.");
            finish();
            return;
        }

        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.e(TAG, "The subscription id is not usable.");
            finish();
            return;
        }

        mSubInfo = SubscriptionUtil.getSubById(mSubscriptionManager, subId);
        mIsEsimOperation = mSubInfo != null && mSubInfo.isEmbedded();
        mSwitchToEuiccSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getFragmentManager());
        mSwitchToRemovableSlotSidecar = SwitchToRemovableSlotSidecar.get(getFragmentManager());
        mEnableMultiSimSidecar = EnableMultiSimSidecar.get(getFragmentManager());
        mEnable = intent.getBooleanExtra(ARG_enable, true);

        if (savedInstanceState == null) {
            if (mEnable) {
                showEnableSubDialog();
            } else {
                showDisableSimConfirmDialog();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchToEuiccSubscriptionSidecar.addListener(this);
        mSwitchToRemovableSlotSidecar.addListener(this);
        mEnableMultiSimSidecar.addListener(this);
    }

    @Override
    protected void onPause() {
        mEnableMultiSimSidecar.removeListener(this);
        mSwitchToRemovableSlotSidecar.removeListener(this);
        mSwitchToEuiccSubscriptionSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchToEuiccSubscriptionSidecar) {
            handleSwitchToEuiccSubscriptionSidecarStateChange();
        } else if (fragment == mSwitchToRemovableSlotSidecar) {
            handleSwitchToRemovableSlotSidecarStateChange();
        } else if (fragment == mEnableMultiSimSidecar) {
            handleEnableMultiSimSidecarStateChange();
        }
    }

    @Override
    public void onConfirm(int tag, boolean confirmed) {
        if (!confirmed
                && tag != DIALOG_TAG_ENABLE_DSDS_CONFIRMATION
                && tag != DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION) {
            finish();
            return;
        }

        switch (tag) {
            case DIALOG_TAG_DISABLE_SIM_CONFIRMATION:
                if (mIsEsimOperation) {
                    Log.i(TAG, "Disabling the eSIM profile.");
                    showProgressDialog(
                            getString(R.string.privileged_action_disable_sub_dialog_progress));
                    mSwitchToEuiccSubscriptionSidecar.run(
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    return;
                }
                Log.i(TAG, "Disabling the pSIM profile.");
                handleTogglePsimAction();
                break;
            case DIALOG_TAG_ENABLE_DSDS_CONFIRMATION:
                if (!confirmed) {
                    Log.i(TAG, "User cancel the dialog to enable DSDS.");
                    showEnableSimConfirmDialog();
                    return;
                }
                if (mTelMgr.doesSwitchMultiSimConfigTriggerReboot()) {
                    Log.i(TAG, "Device does not support reboot free DSDS.");
                    showRebootConfirmDialog();
                    return;
                }
                Log.i(TAG, "Enabling DSDS without rebooting.");
                showProgressDialog(
                        getString(R.string.sim_action_enabling_sim_without_carrier_name));
                mEnableMultiSimSidecar.run(NUM_OF_SIMS_FOR_DSDS);
                break;
            case DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION:
                if (!confirmed) {
                    Log.i(TAG, "User cancel the dialog to reboot to enable DSDS.");
                    showEnableSimConfirmDialog();
                    return;
                }
                Log.i(TAG, "User confirmed reboot to enable DSDS.");
                SimActivationNotifier.setShowSimSettingsNotification(this, true);
                mTelMgr.switchMultiSimConfig(NUM_OF_SIMS_FOR_DSDS);
                break;
            case DIALOG_TAG_ENABLE_SIM_CONFIRMATION:
                Log.i(TAG, "User confirmed to enable the subscription.");
                if (mIsEsimOperation) {
                    showProgressDialog(
                            getString(
                                    R.string.sim_action_switch_sub_dialog_progress,
                                    SubscriptionUtil.getUniqueSubscriptionDisplayName(
                                            mSubInfo, this)));
                    mSwitchToEuiccSubscriptionSidecar.run(mSubInfo.getSubscriptionId());
                    return;
                }
                showProgressDialog(
                        getString(R.string.sim_action_enabling_sim_without_carrier_name));
                mSwitchToRemovableSlotSidecar.run(UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID);
                break;
            default:
                Log.e(TAG, "Unrecognized confirmation dialog tag: " + tag);
                break;
        }
    }

    private void handleSwitchToEuiccSubscriptionSidecarStateChange() {
        switch (mSwitchToEuiccSubscriptionSidecar.getState()) {
            case SidecarFragment.State.SUCCESS:
                Log.i(
                        TAG,
                        String.format(
                                "Successfully %s the eSIM profile.",
                                mEnable ? "enable" : "disable"));
                mSwitchToEuiccSubscriptionSidecar.reset();
                dismissProgressDialog();
                finish();
                break;
            case SidecarFragment.State.ERROR:
                Log.i(
                        TAG,
                        String.format(
                                "Failed to %s the eSIM profile.", mEnable ? "enable" : "disable"));
                mSwitchToEuiccSubscriptionSidecar.reset();
                dismissProgressDialog();
                showErrorDialog(
                        getString(R.string.privileged_action_disable_fail_title),
                        getString(R.string.privileged_action_disable_fail_text));
                break;
        }
    }

    private void handleSwitchToRemovableSlotSidecarStateChange() {
        switch (mSwitchToRemovableSlotSidecar.getState()) {
            case SidecarFragment.State.SUCCESS:
                Log.i(TAG, "Successfully switched to removable slot.");
                mSwitchToRemovableSlotSidecar.reset();
                handleTogglePsimAction();
                dismissProgressDialog();
                finish();
                break;
            case SidecarFragment.State.ERROR:
                Log.e(TAG, "Failed switching to removable slot.");
                mSwitchToRemovableSlotSidecar.reset();
                dismissProgressDialog();
                showErrorDialog(
                        getString(R.string.sim_action_enable_sim_fail_title),
                        getString(R.string.sim_action_enable_sim_fail_text));
                break;
        }
    }

    private void handleEnableMultiSimSidecarStateChange() {
        switch (mEnableMultiSimSidecar.getState()) {
            case SidecarFragment.State.SUCCESS:
                mEnableMultiSimSidecar.reset();
                Log.i(TAG, "Successfully switched to DSDS without reboot.");
                handleEnableSubscriptionAfterEnablingDsds();
                break;
            case SidecarFragment.State.ERROR:
                mEnableMultiSimSidecar.reset();
                Log.i(TAG, "Failed to switch to DSDS without rebooting.");
                dismissProgressDialog();
                showErrorDialog(
                        getString(R.string.dsds_activation_failure_title),
                        getString(R.string.dsds_activation_failure_body_msg2));
                break;
        }
    }

    private void handleEnableSubscriptionAfterEnablingDsds() {
        if (mIsEsimOperation) {
            Log.i(TAG, "DSDS enabled, start to enable profile: " + mSubInfo.getSubscriptionId());
            // For eSIM operations, we simply switch to the selected eSIM profile.
            mSwitchToEuiccSubscriptionSidecar.run(mSubInfo.getSubscriptionId());
            return;
        }

        Log.i(TAG, "DSDS enabled, start to enable pSIM profile.");
        handleTogglePsimAction();
        dismissProgressDialog();
        finish();
    }

    private void handleTogglePsimAction() {
        if (mSubInfo != null) {
            mSubscriptionManager.setUiccApplicationsEnabled(mSubInfo.getSubscriptionId(), mEnable);
            finish();
        } else {
            Log.i(
                    TAG,
                    "The device does not support toggling pSIM. It is enough to just "
                            + "enable the removable slot.");
        }
    }

    /* Handles the enabling SIM action. */
    private void showEnableSubDialog() {
        Log.i(TAG, "Handle subscription enabling.");
        if (isDsdsConditionSatisfied()) {
            showEnableDsdsConfirmDialog();
            return;
        }
        if (!mIsEsimOperation && mTelMgr.isMultiSimEnabled()) {
            Log.i(TAG, "Toggle on pSIM, no dialog displayed.");
            handleTogglePsimAction();
            finish();
            return;
        }
        showEnableSimConfirmDialog();
    }

    private void showEnableDsdsConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_DSDS_CONFIRMATION,
                getString(R.string.sim_action_enable_dsds_title),
                getString(R.string.sim_action_enable_dsds_text),
                getString(R.string.sim_action_yes),
                getString(R.string.sim_action_no_thanks));
    }

    private void showRebootConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION,
                getString(R.string.sim_action_restart_title),
                getString(R.string.sim_action_enable_dsds_text),
                getString(R.string.sim_action_reboot),
                getString(R.string.cancel));
    }

    /* Displays the SIM toggling confirmation dialog. */
    private void showDisableSimConfirmDialog() {
        final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                mSubInfo, this);
        String title =
                mSubInfo == null || TextUtils.isEmpty(displayName)
                        ? getString(
                                R.string.privileged_action_disable_sub_dialog_title_without_carrier)
                        : getString(
                                R.string.privileged_action_disable_sub_dialog_title, displayName);

        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_DISABLE_SIM_CONFIRMATION,
                title,
                null,
                getString(R.string.yes),
                getString(R.string.cancel));
    }

    private void showEnableSimConfirmDialog() {
        List<SubscriptionInfo> activeSubs =
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager);
        SubscriptionInfo activeSub = activeSubs.isEmpty() ? null : activeSubs.get(0);
        if (activeSub == null) {
            Log.i(TAG, "No active subscriptions available.");
            showNonSwitchSimConfirmDialog();
            return;
        }
        Log.i(TAG, "Found active subscription.");
        boolean isBetweenEsim = mIsEsimOperation && activeSub.isEmbedded();
        if (mTelMgr.isMultiSimEnabled() && !isBetweenEsim) {
            showNonSwitchSimConfirmDialog();
            return;
        }
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_SIM_CONFIRMATION,
                getSwitchSubscriptionTitle(),
                getSwitchDialogBodyMsg(activeSub, isBetweenEsim),
                getSwitchDialogPosBtnText(),
                getString(android.R.string.cancel));
    }

    private void showNonSwitchSimConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_SIM_CONFIRMATION,
                getEnableSubscriptionTitle(),
                null /* msg */,
                getString(R.string.yes),
                getString(android.R.string.cancel));
    }

    private String getSwitchDialogPosBtnText() {
        return mIsEsimOperation
                ? getString(
                        R.string.sim_action_switch_sub_dialog_confirm,
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(mSubInfo, this))
                : getString(R.string.sim_switch_button);
    }

    private String getEnableSubscriptionTitle() {
        final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                mSubInfo, this);
        if (mSubInfo == null || TextUtils.isEmpty(displayName)) {
            return getString(R.string.sim_action_enable_sub_dialog_title_without_carrier_name);
        }
        return getString(R.string.sim_action_enable_sub_dialog_title, displayName);
    }

    private String getSwitchSubscriptionTitle() {
        if (mIsEsimOperation) {
            return getString(
                    R.string.sim_action_switch_sub_dialog_title,
                    SubscriptionUtil.getUniqueSubscriptionDisplayName(mSubInfo, this));
        }
        return getString(R.string.sim_action_switch_psim_dialog_title);
    }

    private String getSwitchDialogBodyMsg(SubscriptionInfo activeSub, boolean betweenEsim) {
        final CharSequence subInfoName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                mSubInfo, this);
        final CharSequence activeSubName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                activeSub, this);
        if (betweenEsim && mIsEsimOperation) {
            return getString(
                    R.string.sim_action_switch_sub_dialog_text_downloaded,
                    subInfoName,
                    activeSubName);
        } else if (mIsEsimOperation) {
            return getString(
                    R.string.sim_action_switch_sub_dialog_text,
                    subInfoName,
                    activeSubName);
        } else {
            return getString(
                    R.string.sim_action_switch_sub_dialog_text_single_sim,
                    activeSubName);
        }
    }

    private boolean isDsdsConditionSatisfied() {
        if (mTelMgr.isMultiSimEnabled()) {
            Log.i(TAG, "DSDS is already enabled. Condition not satisfied.");
            return false;
        }
        if (mTelMgr.isMultiSimSupported() != TelephonyManager.MULTISIM_ALLOWED) {
            Log.i(TAG, "Hardware does not support DSDS.");
            return false;
        }
        ImmutableList<UiccSlotInfo> slotInfos = UiccSlotUtil.getSlotInfos(mTelMgr);
        boolean isRemovableSimEnabled =
                slotInfos.stream()
                        .anyMatch(
                                slot ->
                                        slot != null
                                                && slot.isRemovable()
                                                && slot.getIsActive()
                                                && slot.getCardStateInfo()
                                                        == UiccSlotInfo.CARD_STATE_INFO_PRESENT);
        if (mIsEsimOperation && isRemovableSimEnabled) {
            Log.i(TAG, "eSIM operation and removable SIM is enabled. DSDS condition satisfied.");
            return true;
        }
        boolean isEsimProfileEnabled =
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager).stream()
                        .anyMatch(SubscriptionInfo::isEmbedded);
        if (!mIsEsimOperation && isEsimProfileEnabled) {
            Log.i(
                    TAG,
                    "Removable SIM operation and eSIM profile is enabled. DSDS condition"
                        + " satisfied.");
            return true;
        }
        Log.i(TAG, "DSDS condition not satisfied.");
        return false;
    }
}
