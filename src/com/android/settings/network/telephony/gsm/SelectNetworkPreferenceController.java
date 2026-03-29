/*
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
 * Copyright (c) 2022, 2024 Qualcomm Innovation Center, Inc. All rights reserved.
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony.gsm;

import android.content.Context;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
// QTI_BEGIN: 2024-04-15: Telephony: Disable SNPN feature
import android.util.Pair;
// QTI_END: 2024-04-15: Telephony: Disable SNPN feature

// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.annotation.NonNull;
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.annotation.VisibleForTesting;
// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.lifecycle.DefaultLifecycleObserver;
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.lifecycle.Lifecycle;
// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.lifecycle.LifecycleOwner;
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
import com.qti.extphone.ExtTelephonyManager;
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Auto Select Network"
 */
public class SelectNetworkPreferenceController extends TelephonyTogglePreferenceController
// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
        implements DefaultLifecycleObserver {
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback

    private static final String LOG_TAG = "SelectNetworkPreferenceController";
    private PreferenceScreen mPreferenceScreen;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
    private SubscriptionManager mSubscriptionManager;
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
    private List<OnNetworkScanTypeListener> mListeners;
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
    private Client mClient;
    private boolean mServiceConnected;
    private Object mLock = new Object();
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
    @VisibleForTesting
    SwitchPreference mSwitchPreference;

    public SelectNetworkPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListeners = new ArrayList<>();
    }

// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
    @Override
    public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
        Log.i(LOG_TAG, "onStart");
    }

// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
    @Override
    public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
        Log.i(LOG_TAG, "onStop");
    }

// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
    @Override
    public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
       Log.i(LOG_TAG, "onDestroy");
       if (mServiceConnected) {
           mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
           mExtTelephonyManager.disconnectService();
       }
    }

// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
    @Override
    public int getAvailabilityStatus(int subId) {
// QTI_BEGIN: 2024-04-15: Telephony: Disable SNPN feature
        return (MobileNetworkUtils.isCagSnpnEnabled(mContext) && !isMinHalVersion2_2())
// QTI_END: 2024-04-15: Telephony: Disable SNPN feature
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
// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (mTelephonyManager == null) {
            return false;
        }
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        if (MobileNetworkUtils.isCagSnpnEnabled(mContext)) {
            synchronized (mLock) {
                getNetworkSelectionMode();
            }
        }
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
        return MobileNetworkUtils.getAccessMode(mContext,
                mTelephonyManager.getSlotIndex()) ==
                        mExtTelephonyManager.ACCESS_MODE_SNPN ? true : false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (mTelephonyManager == null) {
            return false;
        }
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
        Log.i(LOG_TAG, "isChecked = " + isChecked);
        int accessMode = (isChecked == true) ? mExtTelephonyManager.ACCESS_MODE_SNPN :
                mExtTelephonyManager.ACCESS_MODE_PLMN;
        MobileNetworkUtils.setAccessMode(mContext, mTelephonyManager.getSlotIndex(), accessMode);
        for (OnNetworkScanTypeListener lsn : mListeners) {
                lsn.onNetworkScanTypeChanged(accessMode);
            }

        return true;
    }

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {

        @Override
        public void onConnected() {
            mServiceConnected = true;
            int[] events = new int[] {};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mContext.getPackageName(), mExtPhoneCallbackListener, events);
            Log.i(LOG_TAG, "mExtTelManagerServiceCallback: service connected " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.i(LOG_TAG, "mExtTelManagerServiceCallback: service disconnected");
            if (mServiceConnected) {
                mServiceConnected = false;
                mClient = null;
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
            }
        }
    };

    private void getNetworkSelectionMode() {
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (mTelephonyManager == null) {
            return;
        }
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        if (mSubscriptionManager != null &&
                !mSubscriptionManager.isActiveSubscriptionId(mSubId)) {
            Log.i(LOG_TAG, "getNetworkSelectionMode invalid sub ID " + mSubId);
            return;
        }
        if (mServiceConnected && mClient != null &&
                mTelephonyManager.getSlotIndex() != SubscriptionManager.DEFAULT_SIM_SLOT_INDEX) {
            try {
                Token token = mExtTelephonyManager.getNetworkSelectionMode(
                        mTelephonyManager.getSlotIndex(), mClient);
            } catch (RuntimeException e) {
                Log.i(LOG_TAG, "Exception getNetworkSelectionMode " + e);
            }
            try {
                mLock.wait();
            } catch (Exception e) {
                Log.i(LOG_TAG, "Exception :" + e);
            }
        }
    }

    protected ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void getNetworkSelectionModeResponse(int slotId, Token token, Status status,
                NetworkSelectionMode modes) {
            Log.i(LOG_TAG, "ExtPhoneCallback: getNetworkSelectionModeResponse");
            if (status.get() == Status.SUCCESS) {
                try {
                    MobileNetworkUtils.setAccessMode(mContext, slotId, modes.getAccessMode());
                } catch (Exception e) {
                    Log.i(LOG_TAG, "Exception :" + e);
                }
            }
            synchronized (mLock) {
                mLock.notify();
            }
        }
    };

// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature
    public SelectNetworkPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature

        return this;
    }

// QTI_BEGIN: 2024-04-15: Telephony: Disable SNPN feature
    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isMinHalVersion2_2() {
        try {
            Pair<Integer, Integer> radioVersion = mTelephonyManager.getHalVersion(
                    TelephonyManager.HAL_SERVICE_MODEM);
            int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);
            return halVersion > makeRadioVersion(2, 1);
        } catch (Exception exception) {
            Log.e(LOG_TAG, "Radio version not available. " + exception);
        }
        return false;
    }

// QTI_END: 2024-04-15: Telephony: Disable SNPN feature
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
