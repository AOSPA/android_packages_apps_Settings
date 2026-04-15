// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
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
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import android.util.Log;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support

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
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    private final static String LOG_TAG = "DdsDataOptionStateTuner";
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
    private final Runnable mUpdateCallback;
    private final Map<Integer, DdsDataOptionStateTuner> mCallbacks = new TreeMap<>();
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private int mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mActiveDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
    // Used to avoid unregistering receiver multiple times resulting in an exception
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
    private boolean mIsBroadcastRegistered = false;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                mDefaultDataSubId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
                log("mDefaultDataSubId = " + mDefaultDataSubId);
                refreshCallbackRegistration(context.getApplicationContext());
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
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
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        mSubId = subId;
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        // Update default data sub ID
        mDefaultDataSubId = mSubscriptionManager.getDefaultDataSubscriptionId();
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        log("register mDefaultDataSubId = " + mDefaultDataSubId);
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2024-05-29: Telephony: Fix for mobile data option not being greyed out
        IntentFilter intentFilter =
                new IntentFilter(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        context.registerReceiver(mReceiver, intentFilter);
        mIsBroadcastRegistered = true;

// QTI_END: 2024-05-29: Telephony: Fix for mobile data option not being greyed out
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        if (subId != mDefaultDataSubId) {
            // Only attached to DDS sub's instance.
            return;
        }
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        mActiveDataSubId = mSubscriptionManager.getActiveDataSubscriptionId();
        mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
        log("register mActiveDataSubId = " + mActiveDataSubId
                + " mNonDdsCallState = " + mNonDdsCallState);
        registerTelephonyCallbackOnNddsSub(context);
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
    }

    public void unregister(Context context) {
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        log("unregister");
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        for (int subId : mCallbacks.keySet()) {
            mTelephonyManager.createForSubscriptionId(subId)
                    .unregisterTelephonyCallback(mCallbacks.get(subId));
        }
        mCallbacks.clear();
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        mNonDdsCallState = TelephonyManager.CALL_STATE_IDLE;
        mActiveDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated

// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        if (mIsBroadcastRegistered) {
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
            context.unregisterReceiver(mReceiver);
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
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
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
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
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
     * Used to check if non-DDS sub has a voice call ongoing.
// QTI_END: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
     *
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
     * @return true if a non-DDS voice call is ongoing.
// QTI_END: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
     */
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
    public boolean isInNonDdsVoiceCall() {
// QTI_END: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        return mNonDdsCallState != TelephonyManager.CALL_STATE_IDLE;
    }

    @Override
    public void onCallStateChanged(int state) {
        mNonDdsCallState = state;
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        log("mNonDdsCallState = " + mNonDdsCallState);
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        update();
    }

    @Override
    public void onActiveDataSubscriptionIdChanged(int subId) {
        mActiveDataSubId = subId;
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated
        log("mActiveDataSubId = " + mActiveDataSubId);
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
        update();
    }

    private void update() {
        if (mUpdateCallback != null) {
            mUpdateCallback.run();
        }
    }
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
// QTI_BEGIN: 2023-07-25: Telephony: Fix nDDS callback not get updated

    private void log(String msg) {
        Log.d(LOG_TAG, "SUB " + mSubId + " " + msg);
    }
// QTI_END: 2023-07-25: Telephony: Fix nDDS callback not get updated
// QTI_BEGIN: 2022-11-27: Telephony: Add DSDA DDS case support
}
// QTI_END: 2022-11-27: Telephony: Add DSDA DDS case support
