/*
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
package com.android.settings.network.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.network.SubscriptionUtil;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link DdsDataOptionStateTuner} This is a helper class used to tune DDS data option state
 * in some situations like temp DDS switch happened, and we are not going to make it as
 * a singleton currently to avoid being resident in memory when using seldomly.
 */
public class DdsDataOptionStateTuner extends TelephonyCallback
        implements TelephonyCallback.CallStateListener,
        TelephonyCallback.ActiveDataSubscriptionIdListener {
    private final static String LOG_TAG = "DdsDataOptionStateTuner";
    private final Runnable mUpdateCallback;
    private final Map<Integer, DdsDataOptionStateTuner> mCallbacks = new TreeMap<>();
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private int mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mActiveDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
    // Used to avoid unregistering receiver multiple times resulting in an exception
    private boolean mIsBroadcastRegistered = false;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                mDefaultDataSubId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                log("mDefaultDataSubId = " + mDefaultDataSubId);
                refreshCallbackRegistration(context.getApplicationContext());
                update();
            }
        }
    };

    public DdsDataOptionStateTuner(TelephonyManager tm, SubscriptionManager sm,
            Runnable callback) {
        mTelephonyManager = tm;
        mSubscriptionManager = sm;
        mUpdateCallback = callback;
        mDefaultDataSubId = mSubscriptionManager.getDefaultDataSubscriptionId();
        mActiveDataSubId = mSubscriptionManager.getActiveDataSubscriptionId();
    }

    public void register(Context context, int subId) {
        mSubId = subId;
        // Update default data sub ID
        mDefaultDataSubId = mSubscriptionManager.getDefaultDataSubscriptionId();
        log("register mDefaultDataSubId = " + mDefaultDataSubId);
        if (subId != mDefaultDataSubId) {
            // Only attached to DDS sub's instance.
            return;
        }
        mActiveDataSubId = mSubscriptionManager.getActiveDataSubscriptionId();
        mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
        log("register mActiveDataSubId = " + mActiveDataSubId
                + " mNonDdsCallState = " + mNonDdsCallState);
        registerTelephonyCallbackOnNddsSub(context);

        IntentFilter intentFilter =
                new IntentFilter(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        context.registerReceiver(mReceiver, intentFilter);
        mIsBroadcastRegistered = true;
    }

    public void unregister(Context context) {
        log("unregister");
        for (int subId : mCallbacks.keySet()) {
            mTelephonyManager.createForSubscriptionId(subId)
                    .unregisterTelephonyCallback(mCallbacks.get(subId));
        }
        mCallbacks.clear();
        mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
        mActiveDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        if (mIsBroadcastRegistered) {
            context.unregisterReceiver(mReceiver);
            mIsBroadcastRegistered = false;
        }
    }

    private void refreshCallbackRegistration(Context context) {
        if (mCallbacks.keySet().contains(mDefaultDataSubId)) {
            for (int subId : mCallbacks.keySet()) {
                mTelephonyManager.createForSubscriptionId(subId)
                        .unregisterTelephonyCallback(mCallbacks.get(subId));
            }
            mCallbacks.clear();
            registerTelephonyCallbackOnNddsSub(context);
        }
    }

    private void registerTelephonyCallbackOnNddsSub(Context context) {
        final List<SubscriptionInfo> subs =
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager);
        for (SubscriptionInfo subInfo : subs) {
            // Listen to telephony callback events of the non-DDS.
            if (subInfo.getSubscriptionId() != mDefaultDataSubId) {
                mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId())
                        .registerTelephonyCallback(context.getMainExecutor(), this);
                mCallbacks.put(subInfo.getSubscriptionId(), this);
            }
        }
    }

    /**
     * When there is a nDDS voice call, it is disallowed to turn off mobile data of
     * DDS sub after temp DDS is happened.
     *
     * @return true if option needs to get greyed out
     */
    public boolean isDisallowed() {
        return mNonDdsCallState != TelephonyManager.CALL_STATE_IDLE
                && mDefaultDataSubId != mActiveDataSubId;
    }

    /**
     * Used to check if non-DDS sub has a voice call ongoing.
     *
     * @return true if a non-DDS voice call is ongoing.
     */
    public boolean isInNonDdsVoiceCall() {
        return mNonDdsCallState != TelephonyManager.CALL_STATE_IDLE;
    }

    @Override
    public void onCallStateChanged(int state) {
        mNonDdsCallState = state;
        log("mNonDdsCallState = " + mNonDdsCallState);
        update();
    }

    @Override
    public void onActiveDataSubscriptionIdChanged(int subId) {
        mActiveDataSubId = subId;
        log("mActiveDataSubId = " + mActiveDataSubId);
        update();
    }

    private void update() {
        if (mUpdateCallback != null) {
            mUpdateCallback.run();
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "SUB " + mSubId + " " + msg);
    }
}
