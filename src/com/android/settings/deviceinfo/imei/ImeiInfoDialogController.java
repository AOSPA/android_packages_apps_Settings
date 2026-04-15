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

// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
/*
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
package com.android.settings.deviceinfo.imei;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
import android.util.Pair;
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import com.android.settings.network.telephony.TelephonyUtils;

import com.qti.extphone.QtiImeiInfo;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.

public class ImeiInfoDialogController {

    private static final String TAG = "ImeiInfoDialog";

    @VisibleForTesting
    static final int ID_PRL_VERSION_VALUE = R.id.prl_version_value;
    private static final int ID_MIN_NUMBER_LABEL = R.id.min_number_label;
    @VisibleForTesting
    static final int ID_MIN_NUMBER_VALUE = R.id.min_number_value;
    @VisibleForTesting
    static final int ID_MEID_NUMBER_VALUE = R.id.meid_number_value;
    @VisibleForTesting
    static final int ID_IMEI_VALUE = R.id.imei_value;
    @VisibleForTesting
    static final int ID_IMEI_SV_VALUE = R.id.imei_sv_value;
    @VisibleForTesting
    static final int ID_CDMA_SETTINGS = R.id.cdma_settings;
    @VisibleForTesting
    static final int ID_GSM_SETTINGS = R.id.gsm_settings;

    private final ImeiInfoDialogFragment mDialog;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionInfo mSubscriptionInfo;
    private final int mSlotId;
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    private QtiImeiInfo mQtiImeiInfo[];
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    private boolean mIsDsdsToSsConfigValid;
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM

    public ImeiInfoDialogController(@NonNull ImeiInfoDialogFragment dialog, int slotId) {
        mDialog = dialog;
        mSlotId = slotId;
        final Context context = dialog.getContext();
        mSubscriptionInfo = context.getSystemService(SubscriptionManager.class)
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        mIsDsdsToSsConfigValid = TelephonyUtils.isDsdsToSsConfigValid(context);
        if (mSubscriptionInfo != null) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(mSubscriptionInfo.getSubscriptionId());
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        } else if (isValidSlotIndex(slotId, tm) || mIsDsdsToSsConfigValid) {
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            mTelephonyManager = tm;
        } else {
            mTelephonyManager = null;
        }
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
        TelephonyUtils.connectExtTelephonyService(context);
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
        mQtiImeiInfo = TelephonyUtils.getImeiInfo();
    }

    private String getImei(int slot) {
        String imei = null;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
        try {
// QTI_END: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            if (isMinHalVersion2_1() && !mIsDsdsToSsConfigValid) {
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
                imei = mTelephonyManager.getImei(slot);
            } else {
                if (mQtiImeiInfo == null) {
                    mQtiImeiInfo = TelephonyUtils.getImeiInfo();
                }
                if (mQtiImeiInfo != null) {
                    for (int i = 0; i < mQtiImeiInfo.length; i++) {
                        if (null != mQtiImeiInfo[i] && mQtiImeiInfo[i].getSlotId() == slot) {
                            imei = mQtiImeiInfo[i].getImei();
                            break;
                        }
// QTI_END: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
                    }
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
                }
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
                if (TextUtils.isEmpty(imei)) {
                    imei = mTelephonyManager.getImei(slot);
                }
// QTI_END: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
            }
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
        } catch (Exception exception) {
            Log.i(TAG, "Imei not available. " + exception);
// QTI_END: 2024-02-19: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
        }
        return imei;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    }

    /**
     * Sets IMEI/MEID information based on whether the device is CDMA or GSM.
     */
    public void populateImeiInfo() {
        if (mTelephonyManager == null) {
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            if (mIsDsdsToSsConfigValid) {
                updateDialogForGsmPhone();
            }
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            Log.w(TAG, "TelephonyManager for this slot is null. Invalid slot? id=" + mSlotId);
            return;
        }
        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            updateDialogForCdmaPhone();
        } else {
            updateDialogForGsmPhone();
        }
    }

    private void updateDialogForCdmaPhone() {
        final Resources res = mDialog.getContext().getResources();
        mDialog.setText(ID_MEID_NUMBER_VALUE, getMeid());
        // MIN needs to read from SIM. So if no SIM, we should not show MIN on UI
        mDialog.setText(ID_MIN_NUMBER_VALUE, mSubscriptionInfo != null
                ? mTelephonyManager.getCdmaMin(mSubscriptionInfo.getSubscriptionId())
                : "");

        if (res.getBoolean(R.bool.config_msid_enable)) {
            mDialog.setText(ID_MIN_NUMBER_LABEL,
                    res.getString(R.string.status_msid_number));
        }

        mDialog.setText(ID_PRL_VERSION_VALUE, getCdmaPrlVersion());

        if (isCdmaLteEnabled()) {
            // Show IMEI for LTE device
            mDialog.setText(ID_IMEI_VALUE, getImei(mSlotId));
            mDialog.setText(ID_IMEI_SV_VALUE,
                    mTelephonyManager.getDeviceSoftwareVersion(mSlotId));
        } else {
            // device is not GSM/UMTS, do not display GSM/UMTS features
            mDialog.removeViewFromScreen(ID_GSM_SETTINGS);
        }
    }

    private void updateDialogForGsmPhone() {
        mDialog.setText(ID_IMEI_VALUE, getImei(mSlotId));
        mDialog.setText(ID_IMEI_SV_VALUE,
                mTelephonyManager.getDeviceSoftwareVersion(mSlotId));
        // device is not CDMA, do not display CDMA features
        mDialog.removeViewFromScreen(ID_CDMA_SETTINGS);
    }

    @VisibleForTesting
    String getCdmaPrlVersion() {
        // PRL needs to read from SIM. So if no SIM, return empty
        return mSubscriptionInfo != null ? mTelephonyManager.getCdmaPrlVersion() : "";
    }

    @VisibleForTesting
    boolean isCdmaLteEnabled() {
        return mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled();
    }

    boolean isSimPresent(int slotId) {
        final int simState = mTelephonyManager.getSimState(slotId);
        if ((simState != TelephonyManager.SIM_STATE_ABSENT) &&
                (simState != TelephonyManager.SIM_STATE_UNKNOWN)) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    String getMeid() {
        return mTelephonyManager.getMeid(mSlotId);
    }

    @VisibleForTesting
    private boolean isValidSlotIndex(int slotIndex, TelephonyManager telephonyManager) {
        return slotIndex >= 0 && slotIndex < telephonyManager.getPhoneCount();
    }
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone

    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isMinHalVersion2_1() {
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
// QTI_BEGIN: 2023-03-27: Telephony: Use correct API to get HAL Version
        Pair<Integer, Integer> radioVersion = mTelephonyManager.getHalVersion(
                TelephonyManager.HAL_SERVICE_MODEM);
// QTI_END: 2023-03-27: Telephony: Use correct API to get HAL Version
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);
        return (halVersion > makeRadioVersion(2, 0)) ? true:false;
    }
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
}
