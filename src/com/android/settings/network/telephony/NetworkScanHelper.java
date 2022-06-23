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

package com.android.settings.network.telephony;

import android.annotation.IntDef;
import android.content.Context;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.CellInfo;
import android.telephony.NetworkScan;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneCapability;
import android.telephony.RadioAccessSpecifier;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyScanManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * A helper class that builds the common interface and performs the network scan for two different
 * network scan APIs.
 */
public class NetworkScanHelper {
    public static final String TAG = "NetworkScanHelper";

    /**
     * Callbacks interface to inform the network scan results.
     */
    public interface NetworkScanCallback {
        /**
         * Called when the results is returned from {@link TelephonyManager}. This method will be
         * called at least one time if there is no error occurred during the network scan.
         *
         * <p> This method can be called multiple times in one network scan, until
         * {@link #onComplete()} or {@link #onError(int)} is called.
         *
         * @param results
         */
        void onResults(List<CellInfo> results);

        /**
         * Called when the current network scan process is finished. No more
         * {@link #onResults(List)} will be called for the current network scan after this method is
         * called.
         */
        void onComplete();

        /**
         * Called when an error occurred during the network scan process.
         *
         * <p> There is no more result returned from {@link TelephonyManager} if an error occurred.
         *
         * <p> {@link #onComplete()} will not be called if an error occurred.
         *
         * @see {@link NetworkScan.ScanErrorCode}
         */
        void onError(int errorCode);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS, NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS_LEGACY})
    public @interface NetworkQueryType {}

    /**
     * Performs the network scan using {@link TelephonyManager#requestNetworkScan(
     * NetworkScanRequest, Executor, TelephonyScanManager.NetworkScanCallback)} The network scan
     * results will be returned to the caller periodically in a small time window until the network
     * scan is completed. The complete results should be returned in the last called of
     * {@link NetworkScanCallback#onResults(List)}.
     *
     * <p> This is recommended to be used if modem supports the new network scan api
     * {@link TelephonyManager#requestNetworkScan(
     * NetworkScanRequest, Executor, TelephonyScanManager.NetworkScanCallback)}
     */
    public static final int NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS = 1;

    /**
     * Performs the network scan using {@link IExtTelephony#performIncrementalScan(int)}
     * The network scan is triggered using QcRil hooks, and the results will be returned to the
     * caller periodically in a small time window until the network scan is completed.
     *
     * <p> This is recommended to be used if modem does not support the new network scan api
     * {@link TelephonyManager#requestNetworkScan(
     * NetworkScanRequest, Executor, TelephonyScanManager.NetworkScanCallback)}.
     */
    public static final int NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS_LEGACY = 2;

    /** The constants below are used in the async network scan. */
    @VisibleForTesting
    static final boolean INCREMENTAL_RESULTS = true;
    @VisibleForTesting
    static final int SEARCH_PERIODICITY_SEC = 5;
    @VisibleForTesting
    static final int MAX_SEARCH_TIME_SEC = 254;
    @VisibleForTesting
    static final int INCREMENTAL_RESULTS_PERIODICITY_SEC = 3;

    private final NetworkScanCallback mNetworkScanCallback;
    private final TelephonyManager mTelephonyManager;
    private final TelephonyScanManager.NetworkScanCallback mInternalNetworkScanCallback;
    private final Executor mExecutor;
    private final LegacyIncrementalScanBroadcastReceiver mLegacyIncrScanReceiver;
    private Context mContext;
    private NetworkScan mNetworkScanRequester;
    private IntentFilter filter =
            new IntentFilter("qualcomm.intent.action.ACTION_INCREMENTAL_NW_SCAN_IND");

    public NetworkScanHelper(Context context, TelephonyManager tm, NetworkScanCallback callback,
                             Executor executor) {
        mContext = context;
        mTelephonyManager = tm;
        mNetworkScanCallback = callback;
        mInternalNetworkScanCallback = new NetworkScanCallbackImpl();
        mExecutor = executor;
        mLegacyIncrScanReceiver =
                new LegacyIncrementalScanBroadcastReceiver(mContext, mInternalNetworkScanCallback);
    }

    @VisibleForTesting
    NetworkScanRequest createNetworkScanForPreferredAccessNetworks() {
        long networkTypeBitmap3gpp = mTelephonyManager.getPreferredNetworkTypeBitmask()
                & TelephonyManager.NETWORK_STANDARDS_FAMILY_BITMASK_3GPP;

        List<RadioAccessSpecifier> radioAccessSpecifiers = new ArrayList<>();
        // If the allowed network types are unknown or if they are of the right class, scan for
        // them; otherwise, skip them to save scan time and prevent users from being shown networks
        // that they can't connect to.
        if (networkTypeBitmap3gpp == 0
                || (networkTypeBitmap3gpp & TelephonyManager.NETWORK_CLASS_BITMASK_2G) != 0) {
            radioAccessSpecifiers.add(
                    new RadioAccessSpecifier(AccessNetworkType.GERAN, null, null));
        }
        if (networkTypeBitmap3gpp == 0
                || (networkTypeBitmap3gpp & TelephonyManager.NETWORK_CLASS_BITMASK_3G) != 0) {
            radioAccessSpecifiers.add(
                    new RadioAccessSpecifier(AccessNetworkType.UTRAN, null, null));
        }
        if (networkTypeBitmap3gpp == 0
                || (networkTypeBitmap3gpp & TelephonyManager.NETWORK_CLASS_BITMASK_4G) != 0) {
            radioAccessSpecifiers.add(
                    new RadioAccessSpecifier(AccessNetworkType.EUTRAN, null, null));
        }
        // If a device supports 5G stand-alone then the code below should be re-enabled; however
        // a device supporting only non-standalone mode cannot perform PLMN selection and camp on
        // a 5G network, which means that it shouldn't scan for 5G at the expense of battery as
        // part of the manual network selection process.
        //
        if (networkTypeBitmap3gpp == 0
                || (hasNrSaCapability()
                && (networkTypeBitmap3gpp & TelephonyManager.NETWORK_CLASS_BITMASK_5G) != 0)) {
            radioAccessSpecifiers.add(
                    new RadioAccessSpecifier(AccessNetworkType.NGRAN, null, null));
            Log.d(TAG, "radioAccessSpecifiers add NGRAN.");
        }

        return new NetworkScanRequest(
                NetworkScanRequest.SCAN_TYPE_ONE_SHOT,
                radioAccessSpecifiers.toArray(
                        new RadioAccessSpecifier[radioAccessSpecifiers.size()]),
                SEARCH_PERIODICITY_SEC,
                MAX_SEARCH_TIME_SEC,
                INCREMENTAL_RESULTS,
                INCREMENTAL_RESULTS_PERIODICITY_SEC,
                null /* List of PLMN ids (MCC-MNC) */);
    }

    /**
     * Performs a network scan for the given type {@code type}.
     * {@link #NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS} is recommended if modem supports
     * {@link TelephonyManager#requestNetworkScan(
     * NetworkScanRequest, Executor, TelephonyScanManager.NetworkScanCallback)}.
     *
     * @param type used to tell which network scan API should be used.
     */
    public void startNetworkScan(@NetworkQueryType int type) {
        Log.d(TAG, "startNetworkScan: " + type);
        if (type == NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS) {
            if (mNetworkScanRequester != null) {
                return;
            }
            mNetworkScanRequester = mTelephonyManager.requestNetworkScan(
                    createNetworkScanForPreferredAccessNetworks(),
                    mExecutor,
                    mInternalNetworkScanCallback);
            if (mNetworkScanRequester == null) {
                Log.d(TAG, "mNetworkScanRequester == null");
                onError(NetworkScan.ERROR_RADIO_INTERFACE_ERROR);
            }
        } else if (type == NETWORK_SCAN_TYPE_INCREMENTAL_RESULTS_LEGACY) {
            mContext.registerReceiver(mLegacyIncrScanReceiver, filter);
            boolean success = TelephonyUtils.performIncrementalScan(
                    mContext, mTelephonyManager.getSlotIndex());
            Log.d(TAG, "success: " + success);
            if (!success) {
                onError(NetworkScan.ERROR_RADIO_INTERFACE_ERROR);
            }
        }
    }

    /**
     * The network scan of type {@link #NETWORK_SCAN_TYPE_WAIT_FOR_ALL_RESULTS} can't be stopped,
     * however, the result of the current network scan won't be returned to the callback after
     * calling this method.
     */
    public void stopNetworkQuery() {
        if (mNetworkScanRequester != null) {
            mNetworkScanRequester.stopScan();
            mNetworkScanRequester = null;
        } else {
            try {
                int slotIndex = mTelephonyManager.getSlotIndex();
                if (slotIndex >= 0 && slotIndex < mTelephonyManager.getActiveModemCount()) {
                    TelephonyUtils.abortIncrementalScan(mContext, slotIndex);
                } else {
                    Log.d(TAG, "slotIndex is invalid, skipping abort");
                }
                mContext.unregisterReceiver(mLegacyIncrScanReceiver);
            } catch (NullPointerException ex) {
                Log.e(TAG, "abortIncrementalScan Exception: ", ex);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "IllegalArgumentException");
            }
        }
    }

    private void onResults(List<CellInfo> cellInfos) {
        mNetworkScanCallback.onResults(cellInfos);
    }

    private void onComplete() {
        mNetworkScanCallback.onComplete();
    }

    private void onError(int errCode) {
        mNetworkScanCallback.onError(errCode);
    }

    private boolean hasNrSaCapability() {
        return Arrays.stream(
                mTelephonyManager.getPhoneCapability().getDeviceNrCapabilities())
                .anyMatch(i -> i == PhoneCapability.DEVICE_NR_CAPABILITY_SA);
    }

    private final class NetworkScanCallbackImpl extends TelephonyScanManager.NetworkScanCallback {
        public void onResults(List<CellInfo> results) {
            Log.d(TAG, "Async scan onResults() results = "
                    + CellInfoUtil.cellInfoListToString(results));
            NetworkScanHelper.this.onResults(results);
        }

        public void onComplete() {
            Log.d(TAG, "async scan onComplete()");
            NetworkScanHelper.this.onComplete();
        }

        public void onError(@NetworkScan.ScanErrorCode int errCode) {
            Log.d(TAG, "async scan onError() errorCode = " + errCode);
            NetworkScanHelper.this.onError(errCode);
        }
    }
}
