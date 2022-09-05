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

package com.android.settings.network.telephony;

import static android.telephony.ims.feature.ImsFeature.FEATURE_MMTEL;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Preference controller for "Mobile data"
 */
public class MobileDataPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String DIALOG_TAG = "MobileDataDialog";
    private static final String TAG = "MobileDataPreferenceController";

    private SwitchPreference mPreference;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private MobileDataContentObserver mDataContentObserver;
    private FragmentManager mFragmentManager;
    private static boolean mIsMultiSim;
    @VisibleForTesting
    int mDialogType;
    @VisibleForTesting
    boolean mNeedDialog;

    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    private AnotherSubCallStateListener mCallStateListener;

    public MobileDataPreferenceController(Context context, String key) {
        super(context, key);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mDataContentObserver = new MobileDataContentObserver(new Handler(Looper.getMainLooper()));
        mDataContentObserver.setOnMobileDataChangedListener(() -> updateState(mPreference));
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? AVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDataContentObserver.register(mContext, mSubId);
            mTelephonyManager = getTelephonyManager();
            mIsMultiSim = mTelephonyManager.getActiveModemCount() > 1;
            // Register for voice call notifications. What happens to the mobile data toggle in case
            // of a voice call is dependent on the device being in the SSIM or MSIM mode which is
            // checked in updateState()
            if (mSubId == mSubscriptionManager.getDefaultDataSubscriptionId()) {
                mCallStateListener.register(mContext, mSubId);
            }
        }
    }

    @Override
    public void onStop() {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDataContentObserver.unRegister(mContext);
            mCallStateListener.unregister();
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (mNeedDialog) {
                showDialog(mDialogType);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mNeedDialog = isDialogNeeded();

        if (!mNeedDialog) {
            // Update data directly if we don't need dialog
            MobileNetworkUtils.setMobileDataEnabled(mContext, mSubId, isChecked, false);
            if (mWifiPickerTrackerHelper != null
                    && !mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(mSubId)) {
                mWifiPickerTrackerHelper.setCarrierNetworkEnabled(isChecked);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isChecked() {
        mTelephonyManager = getTelephonyManager();
        return mTelephonyManager.isDataEnabled();
    }

    @Override
    public void updateState(Preference preference) {
        if (mTelephonyManager == null) {
            return;
        }
        super.updateState(preference);
        if (isOpportunistic()) {
            preference.setEnabled(false);
            preference.setSummary(R.string.mobile_data_settings_summary_auto_switch);
        } else {
            if (!mCallStateListener.isIdle()) {
                if (mIsMultiSim) {
                    Log.d(TAG, "nDDS voice call in ongoing");
                    // we will get inside this block only when the current instance is for the DDS
                    if (isChecked()) {
                        Log.d(TAG, "Do not allow the user to turn off DDS mobile data");
                        preference.setEnabled(false);
                        preference.setSummary(
                                R.string.mobile_data_settings_summary_default_data_unavailable);
                    }
                }
            } else {
                if (TelephonyUtils.isSubsidyFeatureEnabled(mContext) &&
                        !TelephonyUtils.isSubsidySimCard(mContext,
                        mSubscriptionManager.getSlotIndex(mSubId))) {
                    preference.setEnabled(false);
                } else {
                    preference.setEnabled(true);
                }
                preference.setSummary(R.string.mobile_data_settings_summary);
            }
        }

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            preference.setSelectable(false);
            preference.setSummary(R.string.mobile_data_settings_summary_unavailable);
        } else {
            preference.setSelectable(true);
        }
    }

    private boolean isOpportunistic() {
        SubscriptionInfo info = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
        return info != null && info.isOpportunistic();
    }

    public void init(FragmentManager fragmentManager, int subId) {
        mFragmentManager = fragmentManager;
        mSubId = subId;

        mTelephonyManager = null;
        mTelephonyManager = getTelephonyManager();

        mCallStateListener =
                new AnotherSubCallStateListener(mTelephonyManager,
                        mSubscriptionManager,
                        ()-> updateState(mPreference));
    }

    private TelephonyManager getTelephonyManager() {
        if (mTelephonyManager != null) {
            return mTelephonyManager;
        }
        TelephonyManager telMgr =
                mContext.getSystemService(TelephonyManager.class);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telMgr = telMgr.createForSubscriptionId(mSubId);
        }
        mTelephonyManager = telMgr;
        return telMgr;
    }

    public void setWifiPickerTrackerHelper(WifiPickerTrackerHelper helper) {
        mWifiPickerTrackerHelper = helper;
    }

    @VisibleForTesting
    boolean isDialogNeeded() {
        final boolean enableData = !isChecked();
        final int defaultSubId = mSubscriptionManager.getDefaultDataSubscriptionId();
        boolean needToDisableOthers = mSubscriptionManager
                .isActiveSubscriptionId(defaultSubId) && defaultSubId != mSubId;
        if (mContext.getResources().getBoolean(
                 com.android.internal.R.bool.config_voice_data_sms_auto_fallback)) {
            // Mobile data of both subscriptions can be enabled
            // simultaneously. DDS setting will be controlled by the config.
            needToDisableOthers = false;
        }
        IImsRegistration imsRegistrationImpl = mTelephonyManager.getImsRegistration(
                mSubscriptionManager.getSlotIndex(mSubId), FEATURE_MMTEL);
        boolean isImsRegisteredOverCiwlan = false;
        try {
            isImsRegisteredOverCiwlan = imsRegistrationImpl.getRegistrationTechnology() ==
                    REGISTRATION_TECH_CROSS_SIM;
        } catch (RemoteException ex) {
            Log.e(TAG, "getRegistrationTechnology failed", ex);
        }
        Log.d(TAG, "isDialogNeeded: " + "enableData=" + enableData + ", isMultiSim=" + mIsMultiSim +
                ", needToDisableOthers=" + needToDisableOthers + ", isImsRegisteredOverCiwlan=" +
                isImsRegisteredOverCiwlan + ", callIdle=" + mCallStateListener.isIdle());
        if (enableData && mIsMultiSim && needToDisableOthers) {
            mDialogType = MobileDataDialogFragment.TYPE_MULTI_SIM_DIALOG;
            return true;
        }
        // If device is in a C_IWLAN call, and the user is trying to disable mobile data, display
        // the warning dialog.
        if (!mCallStateListener.isIdle() && isImsRegisteredOverCiwlan && !enableData) {
            mDialogType = MobileDataDialogFragment.TYPE_DISABLE_CIWLAN_DIALOG;
            return true;
        }
        return false;
    }

    private void showDialog(int type) {
        final MobileDataDialogFragment dialogFragment = MobileDataDialogFragment.newInstance(type,
                mSubId);
        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    private static class AnotherSubCallStateListener extends TelephonyCallback
        implements TelephonyCallback.CallStateListener {
        private Runnable mRunnable;
        private int mState = TelephonyManager.CALL_STATE_IDLE;
        private Map<Integer, AnotherSubCallStateListener> mCallbacks;
        private TelephonyManager mTelephonyManager;
        private SubscriptionManager mSubscriptionManager;

        public AnotherSubCallStateListener(TelephonyManager tm, SubscriptionManager sm,
                Runnable runnable) {
            mTelephonyManager = tm;
            mSubscriptionManager = sm;
            mRunnable = runnable;
            mCallbacks = new TreeMap<>();
        }

        public void register(Context context, int subId) {
            final List<SubscriptionInfo> subs =
                    SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager);
            if (mIsMultiSim) {
                for (SubscriptionInfo subInfo : subs) {
                    if (subInfo.getSubscriptionId() != subId) {
                        mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId())
                                .registerTelephonyCallback(context.getMainExecutor(), this);
                        mCallbacks.put(subInfo.getSubscriptionId(), this);
                    }
                }
            } else {
                mTelephonyManager.createForSubscriptionId(subId)
                        .registerTelephonyCallback(context.getMainExecutor(), this);
                mCallbacks.put(subId, this);
            }
        }

        public void unregister() {
            for (int subId : mCallbacks.keySet()) {
                mTelephonyManager.createForSubscriptionId(subId)
                        .unregisterTelephonyCallback(mCallbacks.get(subId));
            }
            mCallbacks.clear();
        }

        public boolean isIdle() {
            return mState == TelephonyManager.CALL_STATE_IDLE;
        }

        @Override
        public void onCallStateChanged(int state) {
            mState = state;
            if (mRunnable != null) {
                mRunnable.run();
            }
        }
    }
}
