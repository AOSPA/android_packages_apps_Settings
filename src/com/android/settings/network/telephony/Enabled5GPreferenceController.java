// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
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
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions

/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
package com.android.settings.network.telephony;

// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
import android.content.BroadcastReceiver;
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import android.content.Context;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
import android.content.Intent;
import android.content.IntentFilter;
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
import android.content.SharedPreferences;
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import android.telephony.PhoneStateListener;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import android.telephony.RadioAccessFamily;
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import androidx.annotation.VisibleForTesting;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
import com.android.internal.telephony.TelephonyIntents;
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.android.settings.R;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
import com.android.settings.network.AllowedNetworkTypesListener;
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
import com.android.settings.network.telephony.MobileNetworkUtils;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
import com.android.settings.network.telephony.mode.NetworkModes;
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
/**
 * Preference controller for "Enabled 5G Switch"
*/
public class Enabled5GPreferenceController extends TelephonyTogglePreferenceController
         implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "Enable5g";
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
    private static final int NETWORK_MODE_TYPE_INVALID = -1;
    private static final String USER_SELECTED_NW_MODE_KEY = "user_selected_network_type_";
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch

    Preference mPreference;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    private PhoneCallStateListener mPhoneStateListener;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    private TelephonyManager mTelephonyManager;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    @VisibleForTesting
    Integer mCallState;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch

    private ContentObserver mSubsidySettingsObserver;

// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
    private SharedPreferences mSharedPreferences;
    private boolean mChangedBy5gToggle = false;

// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference != null) {
                Log.d(TAG,"DDS is changed");
                updateState(mPreference);
            }
        }
    };
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    public Enabled5GPreferenceController(Context context, String key) {
        super(context, key);
    }

    public Enabled5GPreferenceController init(int subId) {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneCallStateListener();
        }

// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        if (SubscriptionManager.isValidSubscriptionId(mSubId) && mSubId == subId) {
            return this;
        }
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
            .createForSubscriptionId(mSubId);
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                    () -> update());
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        }
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
        mSharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(),
                mContext.MODE_PRIVATE);
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        return this;
    }

// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
    private void update() {
        Log.d(TAG, "update.");
        updatePreference();
        //if user select network mode from prefered network list, then reset cache to invalid.
        if (!mChangedBy5gToggle) {
            cachePreviousSelectedNwType(NETWORK_MODE_TYPE_INVALID);
        }
        mChangedBy5gToggle = false;
    }
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    private void updatePreference() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    @Override
    public int getAvailabilityStatus(int subId) {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
        final PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
// QTI_BEGIN: 2020-06-05: Telephony: Fix NullPointerException
        if (carrierConfig == null || mTelephonyManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_END: 2020-06-05: Telephony: Fix NullPointerException
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        int defaultDdsSubId = SubscriptionManager.getDefaultDataSubscriptionId();
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
        final boolean isNrAllowed =
                checkSupportedRadioBitmask(mTelephonyManager.getAllowedNetworkTypes(),
                TelephonyManager.NETWORK_TYPE_BITMASK_NR);
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
        /*
         * Indicates whether NR can be registered on both SUBs at the same time.
         */
        final boolean isDualNrSupported = TelephonyUtils.isDual5gSupported(mTelephonyManager);
        /*
         * Indicates whether this SUB has NR capability or not.
         */
        final boolean isNrRadioSupported =
                checkSupportedRadioBitmask(mTelephonyManager.getSupportedRadioAccessFamily(),
                TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        final boolean isSingleNrSupportedOnly = !isDualNrSupported && (defaultDdsSubId == subId);

// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        final boolean isVisible = SubscriptionManager.isValidSubscriptionId(subId)
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
                && !carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENABLED_5G_BOOL)
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
                && isNrRadioSupported
// QTI_BEGIN: 2021-03-10: Telephony: Add dual 5g option support
                && isNrAllowed
// QTI_END: 2021-03-10: Telephony: Add dual 5g option support
                && (isDualNrSupported || isSingleNrSupportedOnly);
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        return isVisible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        if (mPhoneStateListener != null) {
            mPhoneStateListener.register(mContext, mSubId);
        }
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    }

    @Override
    public void onStop() {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Update 5G switch after DDS is changed
        if (mDefaultDataChangedReceiver != null) {
            mContext.unregisterReceiver(mDefaultDataChangedReceiver);
        }
// QTI_END: 2020-04-28: Telephony: Update 5G switch after DDS is changed
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        if (mPhoneStateListener != null) {
            mPhoneStateListener.unregister();
        }
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    }

    @Override
    public void updateState(Preference preference) {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
        if (mTelephonyManager == null) {
            return;
        }
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-04-28: Telephony: Show 5G switch properly
        switchPreference.setVisible(isAvailable());
// QTI_END: 2020-04-28: Telephony: Show 5G switch properly
        long preferredNetworkBitMask = RadioAccessFamily.getRafFromNetworkType(
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
                getAllowedNetworkMode());
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        switchPreference.setChecked(isNrNetworkModeType(preferredNetworkBitMask));
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        switchPreference.setEnabled(isCallStateIdle());
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    }

    @Override
    public boolean setChecked(boolean isChecked) {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)
                || (mTelephonyManager == null)) {
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
            return false;
        }
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        int oldNetworkMode = getAllowedNetworkMode();
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        long newNetworkBitMask;
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        if (TelephonyManager.NETWORK_MODE_NR_ONLY != oldNetworkMode) {
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
            long oldNetworkBitMask = RadioAccessFamily.getRafFromNetworkType(oldNetworkMode);
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
            if (isChecked) {
                long networkTypeBitmap4g = oldNetworkBitMask
                        & TelephonyManager.NETWORK_CLASS_BITMASK_4G;
                long networkTypeBitmap3g = oldNetworkBitMask
                        & TelephonyManager.NETWORK_CLASS_BITMASK_3G;
                if (networkTypeBitmap4g == 0 && networkTypeBitmap3g == 0) {
                    //Enable from 2G to 5G.
                    //Use NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA as default value
                    //with LTE
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                    oldNetworkBitMask = RadioAccessFamily.getRafFromNetworkType(
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                            TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                    cachePreviousSelectedNwType(oldNetworkMode);
                } else if(networkTypeBitmap4g == 0) {
                    //Enable from 3G to 5G.
                    //For EVDO only, map to TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO
                    //as no proper mapping value include LTE.
                    if (oldNetworkMode == TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA) {
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                            oldNetworkBitMask = RadioAccessFamily.getRafFromNetworkType(
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                                TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO);
                    } else {
                        oldNetworkBitMask = oldNetworkBitMask
                            | TelephonyManager.NETWORK_TYPE_BITMASK_LTE;
                    }
                    cachePreviousSelectedNwType(oldNetworkMode);
                } else {
                    cachePreviousSelectedNwType(NETWORK_MODE_TYPE_INVALID);
                }
            }
            int userSelectedNwMode = getPreviousSelectedNwType();
            if ((userSelectedNwMode != NETWORK_MODE_TYPE_INVALID) && !isChecked) {
                Log.d(TAG, "userSelectedNwMode: " + userSelectedNwMode);
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                newNetworkBitMask = RadioAccessFamily
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                        .getRafFromNetworkType(userSelectedNwMode);
                cachePreviousSelectedNwType(NETWORK_MODE_TYPE_INVALID);
            } else {
                newNetworkBitMask = isChecked ?
                        (oldNetworkBitMask | TelephonyManager.NETWORK_TYPE_BITMASK_NR)
                        : (oldNetworkBitMask & ~TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            }
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        } else {
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
            newNetworkBitMask = RadioAccessFamily
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
                    .getRafFromNetworkType(TelephonyManager.NETWORK_MODE_LTE_ONLY);
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        }
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        mTelephonyManager.setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, newNetworkBitMask);
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
        mChangedBy5gToggle = true;
// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        return true;
    }

// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2021-07-20: Telephony: Enable 5G from 2G or 3G
    private void cachePreviousSelectedNwType(int oldNetworkMode) {
        Log.d(TAG, "cachePreviousSelectedNwType: " + oldNetworkMode);
        int slotId = SubscriptionManager.getSlotIndex(mSubId);
        mSharedPreferences.edit()
                .putInt(USER_SELECTED_NW_MODE_KEY + slotId, oldNetworkMode).apply();
    }

    private int getPreviousSelectedNwType() {
        int slotId = SubscriptionManager.getSlotIndex(mSubId);
        return mSharedPreferences.getInt(USER_SELECTED_NW_MODE_KEY
                + slotId, NETWORK_MODE_TYPE_INVALID);
    }

// QTI_END: 2021-07-20: Telephony: Enable 5G from 2G or 3G
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    private int getAllowedNetworkMode() {
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        long allowedNetworkTypes = NetworkModes.NETWORK_MODE_UNKNOWN;
// QTI_BEGIN: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
        try {
            allowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                    TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        } catch (Exception ex) {
            Log.e(TAG, "getAllowedNetworkTypesForReason exception", ex);
        }
// QTI_END: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
        return RadioAccessFamily.getNetworkTypeFromRaf((int) allowedNetworkTypes);
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
    }

    @Override
    public boolean isChecked(){
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
        long preNetworkBitMask = RadioAccessFamily.getRafFromNetworkType(
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
                getAllowedNetworkMode());
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
        return isNrNetworkModeType(preNetworkBitMask);
    }

    private boolean isNrNetworkModeType(long currentRadioBitmask) {
        return checkSupportedRadioBitmask(currentRadioBitmask,
                TelephonyManager.NETWORK_TYPE_BITMASK_NR);
    }

    boolean checkSupportedRadioBitmask(long supportedRadioBitmask, long targetBitmask) {
        Log.d(TAG, "supportedRadioBitmask: " + supportedRadioBitmask);
        if ((targetBitmask & supportedRadioBitmask) > 0) {
            return true;
        }
        return false;
    }
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.

    boolean isCallStateIdle() {
        boolean callStateIdle = true;
        if (mCallState != null && mCallState != TelephonyManager.CALL_STATE_IDLE) {
            callStateIdle = false;
        }
        Log.d(TAG, "isCallStateIdle:" + callStateIdle);
        return callStateIdle;
    }

    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super(Looper.getMainLooper());
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);

        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2020-03-30: Telephony: Redesign 5G switch
}
// QTI_END: 2020-03-30: Telephony: Redesign 5G switch
