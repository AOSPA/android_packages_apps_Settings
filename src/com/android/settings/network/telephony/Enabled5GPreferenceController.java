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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.telephony.MobileNetworkUtils;


/**
 * Preference controller for "Enabled 5G Switch"
*/
public class Enabled5GPreferenceController extends TelephonyTogglePreferenceController
         implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "Enable5g";
    private static final int NETWORK_MODE_TYPE_INVALID = -1;
    private static final String USER_SELECTED_NW_MODE_KEY = "user_selected_network_type_";

    Preference mPreference;
    private PhoneCallStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    @VisibleForTesting
    Integer mCallState;

    private ContentObserver mSubsidySettingsObserver;

    private SharedPreferences mSharedPreferences;
    private boolean mChangedBy5gToggle = false;

    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference != null) {
                Log.d(TAG,"DDS is changed");
                updateState(mPreference);
            }
        }
    };
    public Enabled5GPreferenceController(Context context, String key) {
        super(context, key);
    }

    public Enabled5GPreferenceController init(int subId) {
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneCallStateListener();
        }

        if (SubscriptionManager.isValidSubscriptionId(mSubId) && mSubId == subId) {
            return this;
        }
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
            .createForSubscriptionId(mSubId);
        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> update());
        }
        mSharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(),
                mContext.MODE_PRIVATE);
        return this;
    }

    private void update() {
        Log.d(TAG, "update.");
        updatePreference();
        //if user select network mode from prefered network list, then reset cache to invalid.
        if (!mChangedBy5gToggle) {
            cachePreviousSelectedNwType(NETWORK_MODE_TYPE_INVALID);
        }
        mChangedBy5gToggle = false;
    }
    private void updatePreference() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        final PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
        if (carrierConfig == null || mTelephonyManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        int defaultDdsSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        final boolean isNrAllowed =
                checkSupportedRadioBitmask(mTelephonyManager.getAllowedNetworkTypes(),
                TelephonyManager.NETWORK_TYPE_BITMASK_NR);
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

        final boolean isVisible = SubscriptionManager.isValidSubscriptionId(subId)
                && !carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENABLED_5G_BOOL)
                && isNrRadioSupported
                && isNrAllowed
                && (isDualNrSupported || isSingleNrSupportedOnly);
        return isVisible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
        if (mPhoneStateListener != null) {
            mPhoneStateListener.register(mContext, mSubId);
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop() {
        if (mDefaultDataChangedReceiver != null) {
            mContext.unregisterReceiver(mDefaultDataChangedReceiver);
        }
        if (mPhoneStateListener != null) {
            mPhoneStateListener.unregister();
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mTelephonyManager == null) {
            return;
        }
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setVisible(isAvailable());
        long preferredNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
                getAllowedNetworkMode());
        switchPreference.setChecked(isNrNetworkModeType(preferredNetworkBitMask));
        switchPreference.setEnabled(isCallStateIdle());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        int oldNetworkMode = getAllowedNetworkMode();
        long newNetworkBitMask;
        if (TelephonyManager.NETWORK_MODE_NR_ONLY != oldNetworkMode) {
            long oldNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(oldNetworkMode);
            if (isChecked) {
                long networkTypeBitmap4g = oldNetworkBitMask
                        & TelephonyManager.NETWORK_CLASS_BITMASK_4G;
                long networkTypeBitmap3g = oldNetworkBitMask
                        & TelephonyManager.NETWORK_CLASS_BITMASK_3G;
                if (networkTypeBitmap4g == 0 && networkTypeBitmap3g == 0) {
                    //Enable from 2G to 5G.
                    //Use NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA as default value
                    //with LTE
                    oldNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
                            TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                    cachePreviousSelectedNwType(oldNetworkMode);
                } else if(networkTypeBitmap4g == 0) {
                    //Enable from 3G to 5G.
                    //For EVDO only, map to TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO
                    //as no proper mapping value include LTE.
                    if (oldNetworkMode == TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA) {
                            oldNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
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
                newNetworkBitMask = MobileNetworkUtils
                        .getRafFromNetworkType(userSelectedNwMode);
                cachePreviousSelectedNwType(NETWORK_MODE_TYPE_INVALID);
            } else {
                newNetworkBitMask = isChecked ?
                        (oldNetworkBitMask | TelephonyManager.NETWORK_TYPE_BITMASK_NR)
                        : (oldNetworkBitMask & ~TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            }
        } else {
            newNetworkBitMask = MobileNetworkUtils
                    .getRafFromNetworkType(TelephonyManager.NETWORK_MODE_LTE_ONLY);
        }
        mTelephonyManager.setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER, newNetworkBitMask);
        mChangedBy5gToggle = true;
        return true;
    }

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

    private int getAllowedNetworkMode() {
        return MobileNetworkUtils.getNetworkTypeFromRaf(
                (int) mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
    }

    @Override
    public boolean isChecked(){
        long preNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
                getAllowedNetworkMode());
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
}
