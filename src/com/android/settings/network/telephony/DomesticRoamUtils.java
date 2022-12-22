/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.settings.network.telephony;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;

import java.util.Arrays;

public class DomesticRoamUtils {
    private static final String TAG = DomesticRoamUtils.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String CHINA_MCC = "460";
    public static final String EMPTY_OPERATOR_NAME = "";
    private enum OPERATOR_TYPE {
        CHINA_MOBILE,
        CHINA_UNION,
        CHINA_TELECOM,
        CHINA_BROADCAST,
        OTHERS
    }

    public static boolean isFeatureEnabled(Context context) {
        Context appContext = context.getApplicationContext();
        boolean isCustomizeEnabled = appContext.getResources().getBoolean(
                R.bool.config_domestic_roam_customization);
        if (DEBUG) {
            Log.d(TAG, "isFeatureEnabled: isCustomizeEnabled = " + isCustomizeEnabled);
        }
        return isCustomizeEnabled;
    }

    public static String getRegisteredOperatorName(Context context, int subId) {
        if (DEBUG) {
            Log.d(TAG, "getRegisteredOperatorName: context = " + context
                    + " subId = " + subId);
        }
        Context appContext = context.getApplicationContext();
        OPERATOR_TYPE homeOperator = getHomeOperatorType(appContext, subId);
        OPERATOR_TYPE visitOperator = getVisitOperatorType(appContext, subId);
        Log.d(TAG, "getRegisteredOperatorName: homeOperator = " + homeOperator
                + " visitOperator = " + visitOperator);
        if (homeOperator == OPERATOR_TYPE.OTHERS
                || visitOperator == OPERATOR_TYPE.OTHERS
                || homeOperator == visitOperator) {
            Log.d(TAG, "getRegisteredOperatorName: not domestic roam status, return empty");
            return EMPTY_OPERATOR_NAME;
        }
        return combineOperatorNames(appContext, homeOperator, visitOperator);
    }

    public static String getMPLMNOperatorName(Context context, int subId, String idMPLMN) {
        if (DEBUG) {
            Log.d(TAG, "getMPLMNOperatorName: context = " + context
                    + " subId = " + subId + " idMPLMN = " + idMPLMN);
        }
        Context appContext = context.getApplicationContext();
        OPERATOR_TYPE homeOperator = getHomeOperatorType(appContext, subId);
        OPERATOR_TYPE mplmnOperator = getMPLMNOperatorType(appContext, subId, idMPLMN);
        Log.d(TAG, "getMPLMNOperatorName: homeOperator = " + homeOperator
                + " mplmnOperator = " + mplmnOperator);
        if (homeOperator == OPERATOR_TYPE.OTHERS
                || mplmnOperator == OPERATOR_TYPE.OTHERS
                || homeOperator == mplmnOperator) {
            Log.d(TAG, "getMPLMNOperatorName: not domestic roam status, return empty");
            return EMPTY_OPERATOR_NAME;
        }
        return combineOperatorNames(appContext, homeOperator, mplmnOperator);
    }

    private static OPERATOR_TYPE getMPLMNOperatorType(Context context, int subId, String idMPLMN) {
        if (DEBUG) {
            Log.d(TAG, "getMPLMNOperatorType: idMPLMN = " + idMPLMN + " subId = " + subId);
        }
        if (TextUtils.isEmpty(idMPLMN)) {
            if (DEBUG) {
                Log.d(TAG, "getMPLMNOperatorType: idMPLMN is empty");
            }
            return OPERATOR_TYPE.OTHERS;
        }
        String[] mplmnList = context.getResources().getStringArray(
                R.array.domestic_roaming_mplmns);
        if (!Arrays.asList(mplmnList).contains(idMPLMN)
                && !isMPLMNRegistered(context, subId, idMPLMN)) {
            return OPERATOR_TYPE.OTHERS;
        }
        return getOperatorType(context, idMPLMN);
    }

    private static boolean isMPLMNRegistered (Context context, int subId, String idMPLMN) {
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if (null != telephonyManager) {
            String operatorId = telephonyManager.getNetworkOperator(subId);
            if (isValidDomesticOperatorId(operatorId) && operatorId.equalsIgnoreCase(idMPLMN)) {
                Log.d(TAG, "isMPLMNRegistered: return true");
               return true;
            }
        }
        return false;
    }

    private static OPERATOR_TYPE getHomeOperatorType(Context context, int subId) {
        SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        if (null != subscriptionManager
                && !subscriptionManager.isActiveSubscriptionId(subId)) {
            if (DEBUG) {
                Log.d(TAG, "getHomeOperatorType: invalid sub");
            }
            return OPERATOR_TYPE.OTHERS;
        }
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if (null != telephonyManager) {
            String operatorId = telephonyManager.getSimOperator(subId);
            return getOperatorType(context, operatorId);
        }
        return OPERATOR_TYPE.OTHERS;
    }

    private static OPERATOR_TYPE getVisitOperatorType(Context context, int subId) {
        SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        if (null != subscriptionManager
                && !subscriptionManager.isActiveSubscriptionId(subId)) {
            if (DEBUG) {
                Log.d(TAG, "getVisitOperatorType: invalid sub");
            }
            return OPERATOR_TYPE.OTHERS;
        }
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subId);
        if (null != telephonyManager) {
            String operatorId = telephonyManager.getNetworkOperator(subId);
            return getOperatorType(context, operatorId);
        }
        return OPERATOR_TYPE.OTHERS;
    }

    private static OPERATOR_TYPE getOperatorType(Context context, String operatorId) {
        if (DEBUG) {
            Log.d(TAG, "getOperatorType: operatorId = " + operatorId);
        }

        if (!isValidDomesticOperatorId(operatorId)) {
            if (DEBUG) {
                Log.d(TAG, "getOperatorType: invalid domestic operator id");
            }
            return OPERATOR_TYPE.OTHERS;
        }

        String[] arrayCMCCIds = context.getResources().getStringArray(R.array.china_mobile_ids);
        if (Arrays.asList(arrayCMCCIds).contains(operatorId)) {
            return OPERATOR_TYPE.CHINA_MOBILE;
        }

        String[] arrayCUIds = context.getResources().getStringArray(R.array.china_union_ids);
        if (Arrays.asList(arrayCUIds).contains(operatorId)) {
            return OPERATOR_TYPE.CHINA_UNION;
        }

        String[] arrayCTIds = context.getResources().getStringArray(R.array.china_telecom_ids);
        if (Arrays.asList(arrayCTIds).contains(operatorId)) {
            return OPERATOR_TYPE.CHINA_TELECOM;
        }

        String[] arrayCBNIds = context.getResources().getStringArray(R.array.china_broadcast_ids);
        if (Arrays.asList(arrayCBNIds).contains(operatorId)) {
            return OPERATOR_TYPE.CHINA_BROADCAST;
        }
        return OPERATOR_TYPE.OTHERS;
    }

    private static boolean isValidDomesticOperatorId(String operatorId) {
        // check the input params
        if (TextUtils.isEmpty(operatorId)) {
            if (DEBUG) {
                Log.d(TAG, "isValidDomesticOperatorId: operatorId is empty");
            }
            return false;
        }

        // check the operator id length
        int idLength = operatorId.length();
        if (idLength != 5 && idLength != 6) {
            if (DEBUG) {
                Log.d(TAG, "isValidDomesticOperatorId: idLength = " + idLength);
            }
            return false;
        }

        // check the mcc if it is China
        String mcc = operatorId.substring(0, 3);
        if (!CHINA_MCC.equalsIgnoreCase(mcc)) {
            if (DEBUG) {
                Log.d(TAG, "isValidDomesticOperatorId: mcc = " + mcc);
            }
            return false;
        }

        return true;
    }

    private static String combineOperatorNames(Context context, OPERATOR_TYPE homeOperator,
                                               OPERATOR_TYPE visitOperator) {
        StringBuilder outputBuilder = new StringBuilder();
        switch (homeOperator) {
            case CHINA_MOBILE:
                outputBuilder.append(context.getResources().getString(R.string.china_mobile));
                break;
            case CHINA_UNION:
                outputBuilder.append(context.getResources().getString(R.string.china_union));
                break;
            case CHINA_BROADCAST:
                outputBuilder.append(context.getResources().getString(R.string.china_broadcast));
                break;
            case CHINA_TELECOM:
                outputBuilder.append(context.getResources().getString(R.string.china_telecom));
                break;
            case OTHERS:
                Log.w(TAG, "combineOperatorNames: home invalid case");
                return EMPTY_OPERATOR_NAME;
        }

        outputBuilder.append(context.getResources().getString(R.string.separator));

        switch (visitOperator) {
            case CHINA_MOBILE:
                outputBuilder.append(context.getResources().getString(R.string.china_mobile));
                break;
            case CHINA_UNION:
                outputBuilder.append(context.getResources().getString(R.string.china_union));
                break;
            case CHINA_BROADCAST:
                outputBuilder.append(context.getResources().getString(R.string.china_broadcast));
                break;
            case CHINA_TELECOM:
                outputBuilder.append(context.getResources().getString(R.string.china_telecom));
                break;
            case OTHERS:
                Log.w(TAG, "combineOperatorNames: visit invalid case");
                return EMPTY_OPERATOR_NAME;
        }
        Log.d(TAG, "combineOperatorNames: outputBuilder = " + outputBuilder);
        return outputBuilder.toString();
    }
}
