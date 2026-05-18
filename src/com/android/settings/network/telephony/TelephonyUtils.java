// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
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

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
/*
// QTI_END: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
// QTI_BEGIN: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
package com.android.settings.network.telephony;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
import android.telephony.TelephonyManager;
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
import android.telephony.UiccSlotInfo;
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
import android.text.TextUtils;
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import android.util.Log;

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
import com.qti.extphone.ExtTelephonyManager;
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import com.qti.extphone.QtiImeiInfo;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
import com.qti.extphone.ServiceCallback;

// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import org.codeaurora.internal.IExtTelephony;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;
// QTI_END: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.

// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
/**
 * Add static utility functions to get information about Primary Card and Subsidy Lock features.
 */
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
public final class TelephonyUtils {
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    private static final String TAG = "TelephonyUtils";
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation

// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    private static volatile int sDsdsToSsConfigStatus = -1;
    private static UiccSlotInfo[] sSlotsInfo;

// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    // Flag to control debug logging for primary card and subsidy lock features
    public static boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    private static final String PROPERTY_ADVANCED_SCAN  = "persist.vendor.radio.enableadvancedscan";
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    private static final String PROPERTY_DSDS_TO_SS = "persist.vendor.radio.dsds_to_ss";
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
    private static final String PROPERTY_SUBSIDY_DEVICE  = "persist.vendor.radio.subsidydevice";
    private static final String ALLOW_USER_SELECT_DDS = "allow_user_select_dds";

// QTI_END: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
    // Modem version prefix tag
    private static final String MODEM_VERSION_PREFIX_HI_TAG = "MPSS.HI."; // Himalaya
    private static final String MODEM_VERSION_PREFIX_DE_TAG = "MPSS.DE."; // Denali

// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    // UICC provisioning status
    public static final int CARD_NOT_PROVISIONED = 0;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
    public static final int CARD_PROVISIONED = 1;
    public static final int CARD_INVALID_STATE = -1;
// QTI_END: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    private static ExtTelephonyManager mExtTelephonyManager;
    private static boolean mIsServiceBound;
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
// QTI_BEGIN: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
    private static volatile boolean sIsSmartDdsSwitchFeatureAvailable = true; // default to true
    private static Optional<Boolean> mIsSubsidyFeatureEnabled = Optional.empty();
// QTI_END: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    private TelephonyUtils() {
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    }

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    private static UiccSlotInfo[] getUiccSlotsInfo(Context context) {
        UiccSlotInfo[] slotsInfo = null;
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            slotsInfo = telephonyManager.getUiccSlotsInfo();
        }
        return slotsInfo;
    }

    public static int getUiccSlotsCount(Context context){
        if (sSlotsInfo == null) {
            sSlotsInfo = getUiccSlotsInfo(context);
        }
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2025-05-25: Telephony: Add null check for UiccSlotsInfo
        return sSlotsInfo == null ? 0 : sSlotsInfo.length;
// QTI_END: 2025-05-25: Telephony: Add null check for UiccSlotsInfo
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    }

// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    /**
     * Gets the number of available slots.
     *
     * If the DSDS-to-SS configuration is detected as valid, it returns the count of physical
     * UICC slots. Otherwise, it returns the number of currently active modems.
     *
     * @param context The context to access system services.
     * @return The number of available slots.
     */
    public static int getSlotsCount(Context context){
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return isDsdsToSsConfigValid(context) ? getUiccSlotsCount(context) :
                telephonyManager.getActiveModemCount();
    }

    /**
     * Checks if the DSDS-to-SS configuration is valid.
     *
     * This configuration is considered valid if:
     *     The status flag for this configuration is enabled for PSIM or ESIM .
     *     The device has more than one physical UICC slot.
     *     Information for the second slot (index 1) is available.
     *
     * @param context The application context.
     * @return {@code true} if the DSDS-to-SS configuration is valid, {@code false} otherwise.
     */
    public static boolean isDsdsToSsConfigValid(Context context){
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        if (sSlotsInfo == null) {
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            sSlotsInfo = getUiccSlotsInfo(context);
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        }
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        return (sDsdsToSsConfigStatus == 1 || sDsdsToSsConfigStatus == 2) && sSlotsInfo != null
                && sSlotsInfo.length > 1 && sSlotsInfo[1] != null;
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    }

    /**
     * Queries all ExtTelephony configuration values needed at service connect time.
     * Queries the DSDS-to-SS configuration status and the SmartDDS switch feature
     * availability. Both are blocking Binder IPC calls and must be invoked on a
     * background thread.
     * sDsdsToSsConfigStatus values:
     *   0 - dsds_to_ss property not enabled
     *   1 - enabled for PSIM
     *   2 - enabled for PSIM and ESIM
     */
    private static void queryExtTelephonyConfig() {
        if (sDsdsToSsConfigStatus == -1) {
            sDsdsToSsConfigStatus = mExtTelephonyManager.
                    getPropertyValueInt(PROPERTY_DSDS_TO_SS, 0);
        }
        Log.d(TAG, "queryExtTelephonyConfig: sDsdsToSsConfigStatus = " + sDsdsToSsConfigStatus);
        try {
            sIsSmartDdsSwitchFeatureAvailable =
                    mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
            Log.d(TAG, "queryExtTelephonyConfig: sIsSmartDdsSwitchFeatureAvailable = " +
                    sIsSmartDdsSwitchFeatureAvailable);
        } catch (RemoteException ex) {
            Log.e(TAG, "queryExtTelephonyConfig: isSmartDdsSwitchFeatureAvailable exception " + ex);
        }
    }

    public static int getDsdsToSsConfigValue() {
        Log.d(TAG, "getDsdsToSsConfigValue value = " + sDsdsToSsConfigStatus);
        return sDsdsToSsConfigStatus;
    }

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    public static boolean isAdvancedPlmnScanSupported(Context context) {
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        boolean propVal = false;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
        if (mIsServiceBound) {
            try {
                propVal = mExtTelephonyManager.getPropertyValueBool(PROPERTY_ADVANCED_SCAN, false);
            } catch (NullPointerException ex) {
                Log.e(TAG, "isAdvancedPlmnScanSupported: , Exception: ", ex);
            }
        } else {
            Log.e(TAG, "isAdvancedPlmnScanSupported: ExtTelephony Service not connected!");
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        }
        return propVal;
    }

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    public static boolean performIncrementalScan(Context context, int slotId) {
        boolean success = false;
        if (mIsServiceBound) {
            success = mExtTelephonyManager.performIncrementalScan(slotId);
        } else {
            Log.e(TAG, "performIncrementalScan: ExtTelephony Service not connected!");
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        }
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
        return success;
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    }

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    public static void abortIncrementalScan(Context context, int slotId) {
        if (mIsServiceBound) {
            mExtTelephonyManager.abortIncrementalScan(slotId);
        } else {
            Log.e(TAG, "abortIncrementalScan: ExtTelephony Service not connected!");
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        }
    }
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support

    /*
     * As many products come from different modem version, it is hard to maintain one
     * carrier config along with vendor product SKU. But MPSS version code is stable
     * very much, it is a good way rather than config's approach.
     */
    public static boolean isDual5gSupported(TelephonyManager telephonyManager) {
        if (telephonyManager == null) {
            Log.e(TAG, "telephonyManager is null");
            return false;
        }
        final String version = telephonyManager.getBasebandVersion();
        Log.d(TAG, "Base band version = " + version);
        if (!TextUtils.isEmpty(version)) {
            String[] tokens = version.split("-");
            if (tokens != null) {
                for (String token : tokens) {
                    if (token != null && token.startsWith(MODEM_VERSION_PREFIX_HI_TAG)) {
                        String verCode =
                                token.substring(MODEM_VERSION_PREFIX_HI_TAG.length(),
                                token.length());
                        Log.d(TAG, "verCode = " + verCode);
                        if (verCode != null && verCode.length() > 2) {
                            String[] subCode = verCode.split("\\.");
                            try {
                                int major = Integer.parseInt(subCode[0]);
                                int minor = Integer.parseInt(subCode[1]);
                                Log.d(TAG, "Ver major = " + major + " minor = " + minor);
                                if (major >= 4 && minor >= 3) {
                                    return true;
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Fail to parse version");
                                return false;
                            }
                        }
                    } else if (token != null && token.startsWith(MODEM_VERSION_PREFIX_DE_TAG)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support

// QTI_BEGIN: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
    public static boolean isSubsidyFeatureEnabled(Context context) {
        if (!mIsSubsidyFeatureEnabled.isPresent()) {
            if (!mIsServiceBound) {
                Log.e(TAG, "isSubsidyFeatureEnabled: ExtTelephony Service not connected!");
                connectExtTelephonyService(context);
            }

            try {
                mIsSubsidyFeatureEnabled =
                        Optional.of(mExtTelephonyManager.getPropertyValueBool(
                        PROPERTY_SUBSIDY_DEVICE, false));
            } catch (NullPointerException ex) {
                Log.e(TAG, "isSubsidyFeatureEnabled: , Exception: ", ex);
            }
        }
        return mIsSubsidyFeatureEnabled.get();
    }

    public static boolean allowUsertoSetDDS(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), ALLOW_USER_SELECT_DDS, 0) == 1;
    }

    public static boolean isSubsidySimCard(Context context, int slotId) {
        boolean isSubsidySim = false;
        if (!mIsServiceBound) {
            Log.e(TAG, "isSubsidySimCard: ExtTelephony Service not connected!");
            connectExtTelephonyService(context);
        }

        try {
            isSubsidySim = mExtTelephonyManager.isPrimaryCarrierSlotId(slotId);
        } catch (NullPointerException ex) {
            Log.e(TAG, "isSubsidySimCard: , Exception: ", ex);
        }
        return isSubsidySim;
    }

// QTI_END: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    public static QtiImeiInfo[] getImeiInfo() {
        QtiImeiInfo[] qtiImeiInfo = null;
        if (isServiceConnected()) {
            qtiImeiInfo = mExtTelephonyManager.getImeiInfo();
        } else {
            Log.e(TAG, "getImeiInfo: ExtTelephony Service not connected!");
        }
        return qtiImeiInfo;
    }

// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
    public static boolean isSmartDdsSwitchFeatureAvailable() {
        return sIsSmartDdsSwitchFeatureAvailable;
    }

// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    public static void connectExtTelephonyService(Context context) {
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
        Log.d(TAG, "Connect to ExtTelephonyService entered, mIsServiceBound = " + mIsServiceBound);
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        sSlotsInfo = getUiccSlotsInfo(context);
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
        if (!mIsServiceBound) {
            Log.d(TAG, "Connect to ExtTelephonyService...");
            mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
            mExtTelephonyManager.connectService(mServiceCallback);
        }
    }

// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
    public static void switchMultiSimConfig(int config) {
        Log.e(TAG, "switchMultiSimConfig config = " + config);
        if (isServiceConnected()) {
            mExtTelephonyManager.switchMultiSimConfig(config);
        } else {
            Log.e(TAG, "switchMultiSimConfig: ExtTelephony Service not connected!");
        }
    }

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
    public static boolean isServiceConnected() {
        return mIsServiceBound;
    }

    private static ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephony Service connected");
            mIsServiceBound = true;
// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
            // Move synchronous Binder calls to background thread to avoid ANR.
            // Shutdown the executor after submitting the task so the thread is
            // released once the task completes.
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> queryExtTelephonyConfig());
            executor.shutdown();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephony Service disconnected...");
            mIsServiceBound = false;
        }
    };
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
}
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
