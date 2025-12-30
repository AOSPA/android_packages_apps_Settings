/*
 * Copyright (c) 2022, 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony.gsm;

import android.content.Context;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Auto Select Network"
 */
public class SelectNetworkPreferenceController extends TelephonyTogglePreferenceController
        implements DefaultLifecycleObserver {

    private static final String LOG_TAG = "SelectNetworkPreferenceController";
    private PreferenceScreen mPreferenceScreen;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private List<OnNetworkScanTypeListener> mListeners;
    private Client mClient;
    private boolean mServiceConnected;
    private Object mLock = new Object();
    @VisibleForTesting
    SwitchPreference mSwitchPreference;

    public SelectNetworkPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListeners = new ArrayList<>();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
        Log.i(LOG_TAG, "onStart");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
        Log.i(LOG_TAG, "onStop");
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
       Log.i(LOG_TAG, "onDestroy");
       if (mServiceConnected) {
           mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
           mExtTelephonyManager.disconnectService();
       }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return (MobileNetworkUtils.isCagSnpnEnabled(mContext) && !isMinHalVersion2_2())
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
        if (mTelephonyManager == null) {
            return false;
        }
        if (MobileNetworkUtils.isCagSnpnEnabled(mContext)) {
            synchronized (mLock) {
                getNetworkSelectionMode();
            }
        }
        return MobileNetworkUtils.getAccessMode(mContext,
                mTelephonyManager.getSlotIndex()) ==
                        mExtTelephonyManager.ACCESS_MODE_SNPN ? true : false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mTelephonyManager == null) {
            return false;
        }
        Log.i(LOG_TAG, "isChecked = " + isChecked);
        int accessMode = (isChecked == true) ? mExtTelephonyManager.ACCESS_MODE_SNPN :
                mExtTelephonyManager.ACCESS_MODE_PLMN;
        MobileNetworkUtils.setAccessMode(mContext, mTelephonyManager.getSlotIndex(), accessMode);
        for (OnNetworkScanTypeListener lsn : mListeners) {
                lsn.onNetworkScanTypeChanged(accessMode);
            }

        return true;
    }

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
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            }
        }
    };

    private void getNetworkSelectionMode() {
        if (mTelephonyManager == null) {
            return;
        }
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

    public SelectNetworkPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);

        return this;
    }

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
