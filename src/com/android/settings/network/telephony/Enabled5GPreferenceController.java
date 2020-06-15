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
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;


/**
 * Preference controller for "Enabled 5G Switch"
*/

public class Enabled5GPreferenceController extends TelephonyTogglePreferenceController
         implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "Enable5g";

    Preference mPreference;
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;
    private TelephonyManager mTelephonyManager;

    private ContentObserver mPreferredNetworkModeObserver;
    private ContentObserver mSubsidySettingsObserver;

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
        mPreferredNetworkModeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                if (mPreference != null) {
                    Log.d(TAG, "mPreferredNetworkModeObserver#onChange");
                    updateState(mPreference);
                }
            }
        };
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
    }

    public Enabled5GPreferenceController init(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(mSubId) && mSubId == subId) {
            return this;
        }
        mSubId = subId;
        mCarrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
            .createForSubscriptionId(mSubId);
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        if (carrierConfig == null || mTelephonyManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        int defaultDdsSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        final boolean isDds = defaultDdsSubId == subId;
        final boolean is5gEnabledByCarrier = (mTelephonyManager.getAllowedNetworkTypes()
                & TelephonyManager.NETWORK_TYPE_BITMASK_NR) > 0;
        final boolean isVisible = SubscriptionManager.isValidSubscriptionId(subId)
            && !carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENABLED_5G_BOOL)
            && is5gEnabledByCarrier
            && isDds;
        return isVisible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE + mSubId), true,
                mPreferredNetworkModeObserver);
        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
    }

    @Override
    public void onStop() {
        if (mPreferredNetworkModeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mPreferredNetworkModeObserver);
        }
        if (mDefaultDataChangedReceiver != null) {
            mContext.unregisterReceiver(mDefaultDataChangedReceiver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setVisible(isAvailable());
        long preferredNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
                Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    TelephonyManager.DEFAULT_PREFERRED_NETWORK_MODE));
        switchPreference.setChecked(isNrNetworkModeType(preferredNetworkBitMask));
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        int preNetworkMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                TelephonyManager.DEFAULT_PREFERRED_NETWORK_MODE);
        long newNetworkBitMask;
        if (TelephonyManager.NETWORK_MODE_NR_ONLY != preNetworkMode) {
            long preNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(preNetworkMode);
            newNetworkBitMask = isChecked ?
                (preNetworkBitMask | TelephonyManager.NETWORK_TYPE_BITMASK_NR)
                : (preNetworkBitMask & ~TelephonyManager.NETWORK_TYPE_BITMASK_NR);
        } else {
            newNetworkBitMask = MobileNetworkUtils
                .getRafFromNetworkType(TelephonyManager.NETWORK_MODE_LTE_ONLY);
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                MobileNetworkUtils.getNetworkTypeFromRaf((int)newNetworkBitMask));
        if (mTelephonyManager.setPreferredNetworkTypeBitmask(newNetworkBitMask)) {
            Log.d(TAG, "setPreferredNetworkTypeBitmask");
            return true;
        }
        return false;
    }

    @Override
    public boolean isChecked(){
        long preNetworkBitMask = MobileNetworkUtils.getRafFromNetworkType(
                Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    TelephonyManager.DEFAULT_PREFERRED_NETWORK_MODE));
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
}
