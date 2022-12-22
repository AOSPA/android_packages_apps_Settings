/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony.gsm;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;

import com.qti.extphone.ExtTelephonyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Auto Select Network"
 */
public class SelectNetworkPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver {

    private static final String LOG_TAG = "SelectNetworkPreferenceController";
    private PreferenceScreen mPreferenceScreen;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private List<OnNetworkScanTypeListener> mListeners;
    @VisibleForTesting
    SwitchPreference mSwitchPreference;

    public SelectNetworkPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListeners = new ArrayList<>();
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        Log.i(LOG_TAG, "onStart");
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        Log.i(LOG_TAG, "onStop");
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.isCagSnpnEnabled(mContext)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mSwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return MobileNetworkUtils.getAccessMode(mContext,
                mTelephonyManager.getSlotIndex()) ==
                        mExtTelephonyManager.ACCESS_MODE_SNPN ? true : false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Log.i(LOG_TAG, "isChecked = " + isChecked);
        int accessMode = (isChecked == true) ? mExtTelephonyManager.ACCESS_MODE_SNPN :
                mExtTelephonyManager.ACCESS_MODE_PLMN;
        MobileNetworkUtils.setAccessMode(mContext, mTelephonyManager.getSlotIndex(), accessMode);
        for (OnNetworkScanTypeListener lsn : mListeners) {
                lsn.onNetworkScanTypeChanged(accessMode);
            }

        return true;
    }

    public SelectNetworkPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);

        return this;
    }

    public SelectNetworkPreferenceController addListener(OnNetworkScanTypeListener lsn) {
        mListeners.add(lsn);

        return this;
    }

    /**
     * Callback when network scan type changed
     */
    public interface OnNetworkScanTypeListener {
        void onNetworkScanTypeChanged(int type);
    }
}
