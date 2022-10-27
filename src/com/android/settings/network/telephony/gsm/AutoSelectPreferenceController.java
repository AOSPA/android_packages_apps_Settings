/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony.gsm;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.telephony.Enhanced4gBasePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackBase;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

/**
 * Preference controller for "Auto Select Network"
 */
public class AutoSelectPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        Enhanced4gBasePreferenceController.On4gLteUpdateListener,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient,
        SelectNetworkPreferenceController.OnNetworkScanTypeListener {

    private static final long MINIMUM_DIALOG_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final Handler mUiHandler;
    private static final String TAG = "AutoSelectPreferenceController";
    private PreferenceScreen mPreferenceScreen;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private TelephonyManager mTelephonyManager;
    private boolean mOnlyAutoSelectInHome;
    private List<OnNetworkSelectModeListener> mListeners;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private SubscriptionManager mSubscriptionManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private Client mClient;
    private boolean mServiceConnected;
    @VisibleForTesting
    ProgressDialog mProgressDialog;
    @VisibleForTesting
    SwitchPreference mSwitchPreference;
    private AtomicBoolean mUpdatingConfig;
    private int mCacheOfModeStatus;
    private AtomicLong mRecursiveUpdate;
    private Object mLock = new Object();

    public AutoSelectPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mRecursiveUpdate = new AtomicLong();
        mUpdatingConfig = new AtomicBoolean();
        mCacheOfModeStatus = TelephonyManager.NETWORK_SELECTION_MODE_UNKNOWN;
        mListeners = new ArrayList<>();
        mUiHandler = new Handler(Looper.getMainLooper());
        mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                new HandlerExecutor(mUiHandler));
        mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                () -> updatePreference());
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
    }

    @Override
    public void on4gLteUpdated() {
        updateState(mSwitchPreference);
    }

    private void updatePreference() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
        if (mSwitchPreference != null) {
            mRecursiveUpdate.getAndIncrement();
            updateState(mSwitchPreference);
            mRecursiveUpdate.decrementAndGet();
        }
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mAllowedNetworkTypesListener.register(mContext, mSubId);
        mSubscriptionsListener.start();
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        mSubscriptionsListener.stop();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)
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
        if (!mUpdatingConfig.get()) {
            if (MobileNetworkUtils.isCagSnpnEnabled(mContext)) {
                synchronized (mLock) {
                    getNetworkSelectionMode();
                }
            } else {
                mCacheOfModeStatus = mTelephonyManager.getNetworkSelectionMode();
            }
            for (OnNetworkSelectModeListener lsn : mListeners) {
                lsn.onNetworkSelectModeUpdated(mCacheOfModeStatus);
            }
        }
        return mCacheOfModeStatus == TelephonyManager.NETWORK_SELECTION_MODE_AUTO;
    }

    private void getNetworkSelectionMode() {
        if (mSubscriptionManager != null &&
                !mSubscriptionManager.isActiveSubscriptionId(mSubId)) {
            Log.i(TAG, "getNetworkSelectionMode invalid sub ID " + mSubId);
            mCacheOfModeStatus = TelephonyManager.NETWORK_SELECTION_MODE_UNKNOWN;
            return;
        }
        if (mServiceConnected && mClient != null &&
                mTelephonyManager.getSlotIndex() != SubscriptionManager.DEFAULT_SIM_SLOT_INDEX) {
            try {
                Token token = mExtTelephonyManager.getNetworkSelectionMode(
                        mTelephonyManager.getSlotIndex(), mClient);
            } catch (RuntimeException e) {
                Log.i(TAG, "Exception getNetworkSelectionMode " + e);
            }
            try {
                mLock.wait();
            } catch (Exception e) {
                Log.i(TAG, "Exception :" + e);
            }
        } else {
            mCacheOfModeStatus = TelephonyManager.NETWORK_SELECTION_MODE_UNKNOWN;
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(null);
        final int phoneType = mTelephonyManager.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
             preference.setEnabled(false);
             return;
        }
        final ServiceState serviceState = mTelephonyManager.getServiceState();
        if (serviceState == null) {
            preference.setEnabled(false);
            return;
        }

        if (serviceState.getRoaming()) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(!mOnlyAutoSelectInHome);
            if (mOnlyAutoSelectInHome) {
                preference.setSummary(mContext.getString(
                        R.string.manual_mode_disallowed_summary,
                        mTelephonyManager.getSimOperatorName()));
            }
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Log.i(TAG, "isChecked = " + isChecked);
        if (mRecursiveUpdate.get() != 0) {
            // Changing from software are allowed and changing presentation only.
            return true;
        }
        if (isChecked) {
            setAutomaticSelectionMode();
        } else {
            if (mSwitchPreference != null) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        "com.android.settings.Settings$NetworkSelectActivity");
                intent.putExtra(Settings.EXTRA_SUB_ID, mSubId);
                mSwitchPreference.setIntent(intent);
            }
        }
        return false;
    }

    @VisibleForTesting
    Future setAutomaticSelectionMode() {
        final long startMillis = SystemClock.elapsedRealtime();
        showAutoSelectProgressBar();
        if (mSwitchPreference != null) {
            mSwitchPreference.setIntent(null);
            mSwitchPreference.setEnabled(false);
        }
        return ThreadUtils.postOnBackgroundThread(() -> {
            // set network selection mode in background
            mUpdatingConfig.set(true);
            mTelephonyManager.setNetworkSelectionModeAutomatic();
            mUpdatingConfig.set(false);

            //Update UI in UI thread
            final long durationMillis = SystemClock.elapsedRealtime() - startMillis;
            mUiHandler.postDelayed(() -> {
                mRecursiveUpdate.getAndIncrement();
                mSwitchPreference.setEnabled(true);
                mSwitchPreference.setChecked(isChecked());
                mRecursiveUpdate.decrementAndGet();
                dismissProgressBar();
            }, Math.max(MINIMUM_DIALOG_TIME_MILLIS - durationMillis, 0));
        });
    }

    /**
     * Initialization based on given subscription id.
     **/
    public AutoSelectPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        final PersistableBundle carrierConfig =
                CarrierConfigCache.getInstance(mContext).getConfigForSubId(mSubId);
        mOnlyAutoSelectInHome = carrierConfig != null
                ? carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL)
                : false;

        return this;
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {

        @Override
        public void onConnected() {
            mServiceConnected = true;
            mClient = mExtTelephonyManager.registerCallback(
                    mContext.getPackageName(), mExtPhoneCallback);
            Log.i(TAG, "mExtTelManagerServiceCallback: service connected " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.i(TAG, "mExtTelManagerServiceCallback: service disconnected");
            if (mServiceConnected) {
                mServiceConnected = false;
                mClient = null;
            }
        }
    };

    protected IExtPhoneCallback mExtPhoneCallback = new ExtPhoneCallbackBase() {
        @Override
        public void getNetworkSelectionModeResponse(int slotId, Token token, Status status,
                NetworkSelectionMode modes) {
            Log.i(TAG, "ExtPhoneCallback: getNetworkSelectionModeResponse");
            if (status.get() == Status.SUCCESS) {
                try {
                    mCacheOfModeStatus = modes.getIsManual() ?
                            mTelephonyManager.NETWORK_SELECTION_MODE_MANUAL :
                            mTelephonyManager.NETWORK_SELECTION_MODE_AUTO;
                    MobileNetworkUtils.setAccessMode(mContext, slotId, modes.getAccessMode());
                } catch (Exception e) {
                    // send the setting on error
                }
            }
            synchronized (mLock) {
                mLock.notify();
            }
        }
    };

    public AutoSelectPreferenceController addListener(OnNetworkSelectModeListener lsn) {
        mListeners.add(lsn);

        return this;
    }

    private void showAutoSelectProgressBar() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(
                    mContext.getResources().getString(R.string.register_automatically));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // Ignore exception since the dialog will be gone anyway.
            }
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        updateState(mSwitchPreference);
    }

    @Override
    public void onNetworkScanTypeChanged(int type) {
        Log.i(TAG, "onNetworkScanTypeChanged type = " + type);
        mSwitchPreference.setChecked(true);
        setChecked(true);
    }
    /**
     * Callback when network select mode might get updated
     *
     * @see TelephonyManager#getNetworkSelectionMode()
     */
    public interface OnNetworkSelectModeListener {
        void onNetworkSelectModeUpdated(int mode);
    }
}
