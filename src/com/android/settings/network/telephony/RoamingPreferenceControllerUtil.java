/*
* Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
* SPDX-License-Identifier: BSD-3-Clause-Clear
*/

package com.android.settings.network.telephony;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;

public class RoamingPreferenceControllerUtil {

    private static final String TAG = "RoamingPreferenceControllerUtil";

    public static TelephonyManager sTelephonyManager;
    public static SubscriptionManager sSubscriptionManager;
    public static SparseBooleanArray sIsSubInCall;
    public static SparseBooleanArray sIsCiwlanModeSupported;
    public static SparseBooleanArray sIsCiwlanEnabled;
    public static SparseBooleanArray sIsInCiwlanOnlyMode;
    public static SparseBooleanArray sIsImsRegisteredOnCiwlan;

    public static void init(Context context) {
        sTelephonyManager = context.getSystemService(TelephonyManager.class);
        sSubscriptionManager = context.getSystemService(SubscriptionManager.class);
    }

    public static boolean isDialogNeeded(int subId) {
        if (sTelephonyManager == null) {
            return false;
        }
        final boolean isRoaming = MobileNetworkSettings.isRoaming(subId);
        final int DDS = SubscriptionManager.getDefaultDataSubscriptionId();
        final int nDDS = MobileNetworkSettings.getNonDefaultDataSub();
        // Store the call state and C_IWLAN-related settings of all active subscriptions
        int[] activeSubIdList = sSubscriptionManager.getActiveSubscriptionIdList();
        sIsSubInCall = new SparseBooleanArray(activeSubIdList.length);
        sIsCiwlanModeSupported = new SparseBooleanArray(activeSubIdList.length);
        sIsCiwlanEnabled = new SparseBooleanArray(activeSubIdList.length);
        sIsInCiwlanOnlyMode = new SparseBooleanArray(activeSubIdList.length);
        sIsImsRegisteredOnCiwlan = new SparseBooleanArray(activeSubIdList.length);
        for (int i = 0; i < activeSubIdList.length; i++) {
            int subid = activeSubIdList[i];
            TelephonyManager tm = sTelephonyManager.createForSubscriptionId(subid);
            sIsSubInCall.put(subid, tm.getCallStateForSubscription() !=
                    TelephonyManager.CALL_STATE_IDLE);
            sIsCiwlanModeSupported.put(subid, MobileNetworkSettings.isCiwlanModeSupported(subid));
            sIsCiwlanEnabled.put(subid, MobileNetworkSettings.isCiwlanEnabled(subid));
            sIsInCiwlanOnlyMode.put(subid, MobileNetworkSettings.isInCiwlanOnlyMode(subid));
            sIsImsRegisteredOnCiwlan.put(subid, MobileNetworkSettings.isImsRegisteredOnCiwlan(
                    subid));
        }

        // For targets that support MSIM C_IWLAN, the warning is to be shown only for the DDS
        // when either sub is in a call. For other targets, it will be shown only when there is
        // a call on the DDS.
        boolean isMsimCiwlanSupported = MobileNetworkSettings.isMsimCiwlanSupported();
        int subToCheck = DDS;
        if (isMsimCiwlanSupported) {
            if (subId != DDS) {
                // If the code comes here, the user is trying to change the roaming toggle state
                // of the nDDS which we don't care about.
                return false;
            } else {
                // Otherwise, the user is trying to toggle the roaming toggle state of the DDS.
                // In this case, we need to check if the nDDS is in a call. If it is, we will
                // check the C_IWLAN related settings belonging to the nDDS. Otherwise, we will
                // check those of the DDS.
                subToCheck = subToCheckForCiwlanWarningDialog(nDDS, DDS);
                Log.d(TAG, "isDialogNeeded DDS = " + DDS + ", subToCheck = " + subToCheck);
            }
        }

        if (isRoaming && sIsSubInCall.get(subToCheck)) {
            boolean isCiwlanModeSupported = sIsCiwlanModeSupported.get(subToCheck);
            boolean isCiwlanEnabled = sIsCiwlanEnabled.get(subToCheck);
            boolean isInCiwlanOnlyMode = sIsInCiwlanOnlyMode.get(subToCheck);
            boolean isImsRegisteredOnCiwlan = sIsImsRegisteredOnCiwlan.get(subToCheck);
            if (isCiwlanEnabled && (isInCiwlanOnlyMode || !isCiwlanModeSupported)) {
                    Log.d(TAG, "isDialogNeeded: isRoaming = true, isInCall = true" +
                        ", isCiwlanEnabled = true" +
                        ", isInCiwlanOnlyMode = " + isInCiwlanOnlyMode +
                        ", isCiwlanModeSupported = " + isCiwlanModeSupported +
                        ", isImsRegisteredOnCiwlan = " + isImsRegisteredOnCiwlan);
                // If IMS is registered over C_IWLAN-only mode, the device is in a call, and
                // user is trying to disable roaming while UE is romaing, display a warning
                // dialog that disabling roaming will cause a call drop.
                if (isImsRegisteredOnCiwlan) {
                    return true;
                }
            } else {
                Log.d(TAG, "isDialogNeeded: C_IWLAN not enabled or not in C_IWLAN-only mode");
            }
        } else {
            Log.d(TAG, "isDialogNeeded: Not roaming or not in a call");
        }
        return false;
    }

    private static int subToCheckForCiwlanWarningDialog(int ndds, int dds) {
        int subToCheck = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (sIsSubInCall.get(ndds) && sIsCiwlanEnabled.get(ndds) &&
                (sIsInCiwlanOnlyMode.get(ndds) || !sIsCiwlanModeSupported.get(ndds)) &&
                sIsImsRegisteredOnCiwlan.get(ndds)) {
            subToCheck = ndds;
        } else {
            subToCheck = dds;
        }
        return subToCheck;
    }

    /**
      * When there is a nDDS voice call, it is disallowed to turn off data roaming of
      * DDS sub after temp DDS is happened.
      *
      * @return true if option needs to get greyed out
      */
    public static boolean isDisallowed(int subId) {
        if (sTelephonyManager == null) {
            return false;
        }
        final int DDS = SubscriptionManager.getDefaultDataSubscriptionId();
        final int nDDS = MobileNetworkSettings.getNonDefaultDataSub();
        final int activeDataSubId = sSubscriptionManager.getActiveDataSubscriptionId();
        if (DDS == subId) {
            TelephonyManager tm = sTelephonyManager.createForSubscriptionId(nDDS);
            if (tm.getCallStateForSubscription() != TelephonyManager.CALL_STATE_IDLE
                    && (DDS != activeDataSubId)) {
                return true;
            }
        }
        return false;
    }
}
