/**
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.settings.network.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.NetworkScan;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyScanManager;
import android.util.Log;

import com.android.internal.telephony.OperatorInfo;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class receives the incremental scan results intent from QCRIL Message Tunnel, processes it
 * and sends them to the network search results activity {@link NetworkSelectSettings.java}.
 */

public class LegacyIncrementalScanBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LegacyIncrementalScanBroadcastReceiver";
    private static final String ACTION_INCREMENTAL_NW_SCAN_IND
            = "qualcomm.intent.action.ACTION_INCREMENTAL_NW_SCAN_IND";

    private static final String EXTRA_SCAN_RESULT           = "scan_result";
    private static final String EXTRA_INCREMENTAL_SCAN_DATA = "incr_nw_scan_data";
    private static final String EXTRA_INSTANCE_ID           = "sub_id";

    private static final int QUERY_EXCEPTION                = -1;

    // Network scan was successful and complete
    private static final int NAS_QUERY_COMPLETE             = 0;
    // Network scan was partial
    private static final int NAS_QUERY_PARTIAL              = 1;
    // Network scan was aborted
    private static final int NAS_QUERY_ABORT                = 2;
    // Network scan did not complete due to a radio link failure recovery in progress
    private static final int NAS_QUERY_REJ_IN_RLF           = 3;
    // Sending incremental network scan errors
    private static final int NAS_QUERY_INCREMENT_ERROR      = 4;
    // Periodic network scan gave partial results
    private static final int NAS_QUERY_PARTIAL_PERIODIC     = 5;

    private static int sPhoneCount;
    private Context mContext;

    // QueryDetails for each phoneId
    private QueryDetails[] mQueryDetails;

    private final TelephonyScanManager.NetworkScanCallback mNetworkScanCallback;

    // TODO: This class may not be required since handling of incremental results is already
    // being taken care of in {@link NetworkSelectSettings} class.
    class QueryDetails {
        String[] storedScanInfo;

        QueryDetails() {
            storedScanInfo = null;
        }

        void concatScanInfo(String[] scanInfo) {
            String[] concatScanInfo = new String[storedScanInfo.length + scanInfo.length];
            System.arraycopy(storedScanInfo, 0, concatScanInfo, 0, storedScanInfo.length);
            System.arraycopy(scanInfo, 0, concatScanInfo, storedScanInfo.length,
                    scanInfo.length);
            storedScanInfo = concatScanInfo;
        }

        void reset() {
            storedScanInfo = null;
        }
    }

    public LegacyIncrementalScanBroadcastReceiver(Context context,
                TelephonyScanManager.NetworkScanCallback mInternalNetworkScanCallback) {
        mContext = context;
        TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        sPhoneCount = tm.getActiveModemCount();
        mQueryDetails = new QueryDetails[sPhoneCount];
        for (int i = 0; i < sPhoneCount; i++) {
            mQueryDetails[i] = new QueryDetails();
        }
        mNetworkScanCallback = mInternalNetworkScanCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + intent);
        if (ACTION_INCREMENTAL_NW_SCAN_IND.equals(intent.getAction())) {
            broadcastIncrementalQueryResults(intent);
        }
    }

    private void onResults(List<CellInfo> results) {
        mNetworkScanCallback.onResults(results);
    }

    private void onComplete() {
        mNetworkScanCallback.onComplete();
    }

    private void onError(int error) {
        mNetworkScanCallback.onError(error);
    }

    private void broadcastIncrementalQueryResults(Intent intent) {
        int result = intent.getIntExtra(EXTRA_SCAN_RESULT, QUERY_EXCEPTION);
        int phoneId = intent.getIntExtra(EXTRA_INSTANCE_ID,
                SubscriptionManager.INVALID_SIM_SLOT_INDEX);

        Log.d(TAG, "broadcastIncrementalQueryResults: phoneid: " + phoneId + ", result: " + result);

        if (phoneId < 0 || phoneId >= sPhoneCount) {
            // Invalid phoneId
            onError(NetworkScan.ERROR_INVALID_SCAN);
            return;
        }

        if (result == NAS_QUERY_REJ_IN_RLF) {
            onError(NetworkScan.ERROR_RADIO_INTERFACE_ERROR);
            return;
        }

        if (result == NAS_QUERY_ABORT) {
            onError(NetworkScan.ERROR_MODEM_ERROR);
            return;
        }

        if (result == NAS_QUERY_COMPLETE || result == NAS_QUERY_PARTIAL) {
            String[] scanInfo = intent.getStringArrayExtra(EXTRA_INCREMENTAL_SCAN_DATA);
            QueryDetails queryDetails = mQueryDetails[phoneId];

            Log.d(TAG, "broadcastIncrementalQueryResults"
                    + ", scanInfo.length: " + (scanInfo == null ? 0 : scanInfo.length));

            if (queryDetails.storedScanInfo != null && scanInfo != null) {
                queryDetails.concatScanInfo(scanInfo);
            } else {
                queryDetails.storedScanInfo = scanInfo;
            }

            if (queryDetails.storedScanInfo != null) {
                List<CellInfo> cellInfos = getCellInfosFromScanResult(queryDetails.storedScanInfo);
                onResults(cellInfos);
            }

            if (result == NAS_QUERY_COMPLETE) {
                // Clear the cache, otherwise the results for the next scan will be combined with
                // the current one.
                queryDetails.reset();
                onComplete();
            }
        }
    }

    private List<CellInfo> getCellInfosFromScanResult(String[] scanInfos) {
        Log.d(TAG, "Number of operators: " + (scanInfos.length)/4);
        List<CellInfo> cellInfoList = new ArrayList<CellInfo>();
        if (scanInfos.length >= 4 && (scanInfos.length % 4 == 0)) {
            // The scan results are grouped into four elements per operator.
            for (int i = 0; i < scanInfos.length / 4; i++) {
                int j = 4 * i;
                String operatorAlphaLong   = scanInfos[0 + j];
                String operatorAlphaShort  = scanInfos[1 + j];
                String operatorNumeric     = scanInfos[2 + j];
                String operatorStateString = scanInfos[3 + j];

                OperatorInfo operatorInfo = new OperatorInfo(operatorAlphaLong,
                        operatorAlphaShort,
                        operatorNumeric,
                        operatorStateString);

                CellInfo cellinfo =
                        CellInfoUtil.convertLegacyIncrScanOperatorInfoToCellInfo(operatorInfo);

                Log.d(TAG, "OperatorInfo: " + operatorInfo.toString()
                        + " CellInfo: " + CellInfoUtil.cellInfoToString(cellinfo));

                cellInfoList.add(cellinfo);
            }
        }
        return cellInfoList;
    }
}
