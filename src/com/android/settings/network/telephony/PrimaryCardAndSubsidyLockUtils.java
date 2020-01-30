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

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import org.codeaurora.internal.IExtTelephony;

/**
 * Add static utility functions to get information about Primary Card and Subsidy Lock features.
 */
public final class PrimaryCardAndSubsidyLockUtils {

    private static final String TAG = "PrimaryCardAndSubsidyLockUtils";

    // Flag to control debug logging for primary card and subsidy lock features
    public static boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    // Primary card and subsidy lock related system properties
    private static final String PROPERTY_PRIMARY_CARD   = "persist.vendor.radio.primarycard";
    private static final String PROPERTY_DETECT_4G_CARD = "persist.vendor.radio.detect4gcard";
    private static final String PROPERTY_L_W_ENABLED    = "persist.vendor.radio.lw_enabled";
    private static final String PROPERTY_SUBSIDY_LOCK   = "ro.vendor.radio.subsidylock";

    // Settings database configurations
    public static final String CONFIG_CURRENT_PRIMARY_SUB = "config_current_primary_sub";
    public static final String SUBSIDY_STATUS = "subsidy_status";

    // Subsidy lock resticted states
    private static final int SUBSIDYLOCK_UNLOCKED = 103;
    private static final int PERMANENTLY_UNLOCKED = 100;

    // UICC provisioning status
    public static final int CARD_NOT_PROVISIONED = 0;

    private PrimaryCardAndSubsidyLockUtils() {
    }

    public static boolean isPrimaryCardEnabled() {
        return isVendorPropertyEnabled(PROPERTY_PRIMARY_CARD);
    }

    public static boolean isDetect4gCardEnabled() {
        return isVendorPropertyEnabled(PROPERTY_DETECT_4G_CARD);
    }

    public static boolean isPrimaryCardLWEnabled() {
        return isVendorPropertyEnabled(PROPERTY_L_W_ENABLED);
    }

    public static boolean isSubsidyLockFeatureEnabled() {
        return (getVendorPropertyInt(PROPERTY_SUBSIDY_LOCK) == 1);
    }

    public static boolean isSubsidyUnlocked(Context context) {
        return getSubsidyStatus(context) == SUBSIDYLOCK_UNLOCKED;
    }

    public static boolean isSubsidyPermanentlyUnlocked(Context context) {
        return getSubsidyStatus(context) == SUBSIDYLOCK_UNLOCKED;
    }

    private static int getSubsidyStatus(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SUBSIDY_STATUS, -1);
    }

    private static boolean isVendorPropertyEnabled(String propertyName) {
        boolean propVal = false;
        IExtTelephony extTelephony = IExtTelephony.Stub
                .asInterface(ServiceManager.getService("extphone"));
        try {
            propVal = extTelephony.getPropertyValueBool(propertyName, false);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(TAG, "isVendorPropertyEnabled: " + propertyName + ", Exception: ", ex);
        }
        return propVal;
    }

    private static int getVendorPropertyInt(String propertyName) {
        int propVal = -1;
        IExtTelephony extTelephony = IExtTelephony.Stub
                .asInterface(ServiceManager.getService("extphone"));
        try {
            propVal = extTelephony.getPropertyValueInt(propertyName, -1);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(TAG, "getVendorPropertyInt: " + propertyName + ", Exception: ", ex);
        }
        return propVal;
    }

    public static int getUiccCardProvisioningStatus(int phoneId) {
        int provStatus = CARD_NOT_PROVISIONED;
        IExtTelephony extTelephony = IExtTelephony.Stub
                .asInterface(ServiceManager.getService("extphone"));
        try {
            provStatus = extTelephony.getCurrentUiccCardProvisioningStatus(phoneId);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(TAG, "getUiccCardProvisioningStatus: " + phoneId + ", Exception: ", ex);
        }
        return provStatus;
    }
}
