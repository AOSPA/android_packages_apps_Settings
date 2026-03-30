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
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * A dialog fragment that asks the user if they are sure they want to turn on data roaming
 * to avoid accidental charges.
 */
public class RoamingDialogFragment extends InstrumentedDialogFragment implements OnClickListener {

// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    private static final String PREF_TITLE = "pref_title";
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
    private static final String SUB_ID_KEY = "sub_id_key";
    private static final String DIALOG_TYPE = "dialog_type";
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
    private static final String ARG_CIWLAN_MODE_SUPPORTED = "ciwlan_mode_supported";
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev

    public static final int TYPE_ENABLE_DIALOG = 0;
    public static final int TYPE_DISABLE_CIWLAN_DIALOG = 1;
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev

    private CarrierConfigManager mCarrierConfigManager;
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    private String mPrefTitle;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
    private int mType;
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
    private int mSubId;
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
    private boolean mCiwlanModeSupported;
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs

// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
    public static RoamingDialogFragment newInstance(String prefTitle, int type, int subId,
            boolean ciwlanModeSupported) {
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        final RoamingDialogFragment dialogFragment = new RoamingDialogFragment();
        final Bundle args = new Bundle();
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        args.putString(PREF_TITLE, prefTitle);
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        args.putInt(DIALOG_TYPE, type);
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        args.putInt(SUB_ID_KEY, subId);
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        args.putBoolean(ARG_CIWLAN_MODE_SUPPORTED, ciwlanModeSupported);
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Bundle args = getArguments();
        mSubId = args.getInt(SUB_ID_KEY);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        final Bundle bundle = getArguments();
        final Context context = getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        mPrefTitle = bundle.getString(PREF_TITLE).toLowerCase();
        mType = bundle.getInt(DIALOG_TYPE);
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        mCiwlanModeSupported = bundle.getBoolean(ARG_CIWLAN_MODE_SUPPORTED);
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs

// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        switch (mType) {
            case TYPE_ENABLE_DIALOG:
                int message = R.string.roaming_warning;
                final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                        mSubId);
                if (carrierConfig != null && carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CHECK_PRICING_WITH_CARRIER_FOR_DATA_ROAMING_BOOL))
                {
                    message = R.string.roaming_check_price_warning;
                }
                builder.setMessage(getResources().getString(message))
                       .setTitle(getResources().getString(R.string.roaming_alert_title));
                break;
            case TYPE_DISABLE_CIWLAN_DIALOG:
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
                String msg = mCiwlanModeSupported ?
                        context.getString(
                                R.string.toggle_disable_ciwlan_call_will_drop_dialog_body,
                                mPrefTitle) :
                        context.getString(
                                R.string.toggle_disable_ciwlan_call_might_drop_dialog_body,
                                mPrefTitle);
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
                builder.setTitle(context.getString(
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
                                R.string.toggle_disable_ciwlan_call_dialog_title, mPrefTitle))
                       .setMessage(msg);
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
                break;
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        }
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        builder.setIconAttribute(android.R.attr.alertDialogIcon)
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
               .setPositiveButton(android.R.string.ok, this)
               .setNegativeButton(android.R.string.cancel, this);
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        return builder.create();
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_ROAMING_DIALOG;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        final TelephonyManager telephonyManager =
                getContext().getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        if (telephonyManager == null) {
            return;
        }
        switch (mType) {
            case TYPE_ENABLE_DIALOG:
                // let the host know that the positive button has been clicked
                if (which == dialog.BUTTON_POSITIVE) {
                    telephonyManager.setDataRoamingEnabled(true);
                }
                break;
            case TYPE_DISABLE_CIWLAN_DIALOG:
                if (which == dialog.BUTTON_POSITIVE) {
                    telephonyManager.setDataRoamingEnabled(false);
                }
                break;
// QTI_END: 2022-10-07: Telephony: Merge "Show warning when user tries to turn off data/roaming/C_IWLAN toggle" into t-keystone-qcom-dev
        }
    }
}
