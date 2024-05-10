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
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2022-2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static android.telephony.ims.feature.ImsFeature.FEATURE_MMTEL;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Roaming"
 */
public class RoamingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, MobileNetworkRepository.MobileNetworkCallback {
    private static final String TAG = "RoamingController";
    private static final String DIALOG_TAG = "MobileDataDialog";

    private RestrictedSwitchPreference mSwitchPreference;
    private TelephonyManager mTelephonyManager;
    public SubscriptionManager mSubscriptionManager;
    private CarrierConfigManager mCarrierConfigManager;
    protected MobileNetworkRepository mMobileNetworkRepository;
    protected LifecycleOwner mLifecycleOwner;
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList = new ArrayList<>();

    private DdsDataOptionStateTuner mDdsDataOptionStateTuner;
    private SparseBooleanArray mIsSubInCall;
    private SparseBooleanArray mIsCiwlanModeSupported;
    private SparseBooleanArray mIsCiwlanEnabled;
    private SparseBooleanArray mIsInCiwlanOnlyMode;
    private SparseBooleanArray mIsImsRegisteredOnCiwlan;

    @VisibleForTesting
    FragmentManager mFragmentManager;
    MobileNetworkInfoEntity mMobileNetworkInfoEntity;
    int mDialogType;

    public RoamingPreferenceController(Context context, String key, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner, int subId) {
        super(context, key);
        mSubId = subId;
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (carrierConfig != null && carrierConfig.getBoolean(
                CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this, mSubId);
        mMobileNetworkRepository.updateEntity();

        // If the current instance is for the DDS, listen to the call state changes on nDDS.
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDdsDataOptionStateTuner.register(mContext, mSubId);
        }
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mMobileNetworkRepository.removeRegister(this);
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mDdsDataOptionStateTuner.unregister(mContext);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ? AVAILABLE
                : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isDialogNeeded()) {
            showDialog(mDialogType);
        } else {
            // Update data directly if we don't need dialog
            if (mTelephonyManager != null) {
                mTelephonyManager.setDataRoamingEnabled(isChecked);
                return true;
            }
        }

        return false;
    }

    @Override
    public void updateState(Preference preference) {
        if (mTelephonyManager == null) {
            return;
        }
        super.updateState(preference);
        mSwitchPreference = (RestrictedSwitchPreference) preference;
        update();
    }

    private void update() {
        if (mSwitchPreference == null) {
            return;
        }
        if (!mSwitchPreference.isDisabledByAdmin()) {
            mSwitchPreference.setEnabled(mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            mSwitchPreference.setChecked(isChecked());

            if (mDdsDataOptionStateTuner.isDisallowed()) {
                Log.d(TAG, "nDDS voice call in ongoing");
                // we will get inside this block only when the current instance is for the DDS
                if (isChecked()) {
                    Log.d(TAG, "Do not allow the user to turn off DDS data roaming");
                    mSwitchPreference.setEnabled(false);
                    mSwitchPreference.setSummary(
                            R.string.mobile_data_settings_summary_dds_roaming_unavailable);
                }
            }
        }
    }

    @VisibleForTesting
    boolean isDialogNeeded() {
        if (mTelephonyManager == null) {
            return false;
        }
        final boolean isRoamingEnabled = isChecked();
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                mSubId);
        // Need dialog if we need to turn on roaming and the roaming charge indication is allowed
        if (!isRoamingEnabled && (carrierConfig == null || !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL))) {
            mDialogType = RoamingDialogFragment.TYPE_ENABLE_DIALOG;
            return true;
        }
        if (isRoamingEnabled) {
            final boolean isRoaming = MobileNetworkSettings.isRoaming(mSubId);
            final int DDS = SubscriptionManager.getDefaultDataSubscriptionId();
            final int nDDS = MobileNetworkSettings.getNonDefaultDataSub();

            // Store the call state and C_IWLAN-related settings of all active subscriptions
            int[] activeSubIdList = mSubscriptionManager.getActiveSubscriptionIdList();
            mIsSubInCall = new SparseBooleanArray(activeSubIdList.length);
            mIsCiwlanModeSupported = new SparseBooleanArray(activeSubIdList.length);
            mIsCiwlanEnabled = new SparseBooleanArray(activeSubIdList.length);
            mIsInCiwlanOnlyMode = new SparseBooleanArray(activeSubIdList.length);
            mIsImsRegisteredOnCiwlan = new SparseBooleanArray(activeSubIdList.length);
            for (int i = 0; i < activeSubIdList.length; i++) {
                int subId = activeSubIdList[i];
                TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
                mIsSubInCall.put(subId, tm.getCallStateForSubscription() !=
                        TelephonyManager.CALL_STATE_IDLE);
                mIsCiwlanModeSupported.put(subId, MobileNetworkSettings.isCiwlanModeSupported(
                        subId));
                mIsCiwlanEnabled.put(subId, MobileNetworkSettings.isCiwlanEnabled(subId));
                mIsInCiwlanOnlyMode.put(subId, MobileNetworkSettings.isInCiwlanOnlyMode(subId));
                mIsImsRegisteredOnCiwlan.put(subId, MobileNetworkSettings.isImsRegisteredOnCiwlan(
                        subId));
            }

            // For targets that support MSIM C_IWLAN, the warning is to be shown only for the DDS
            // when either sub is in a call. For other targets, it will be shown only when there is
            // a call on the DDS.
            boolean isMsimCiwlanSupported = MobileNetworkSettings.isMsimCiwlanSupported();
            int subToCheck = DDS;
            if (isMsimCiwlanSupported) {
                if (mSubId != DDS) {
                    // If the code comes here, the user is trying to change the roaming toggle state
                    // of the nDDS which we don't care about.
                    return false;
                } else {
                    // Otherwise, the user is trying to toggle the roaming toggle state of the DDS.
                    // In this case, we need to check if the nDDS is in a call. If it is, we will
                    // check the C_IWLAN related settings belonging to the nDDS. Otherwise, we will
                    // check those of the DDS.
                    subToCheck = subToCheckForCiwlanWarningDialog(nDDS, DDS);
                    Log.d(TAG, "isDialogNeeded DDS = " + DDS + ", subToCheck = " + subToCheck);
                }
            }

            if (isRoaming && mIsSubInCall.get(subToCheck)) {
                boolean isCiwlanModeSupported = mIsCiwlanModeSupported.get(subToCheck);
                boolean isCiwlanEnabled = mIsCiwlanEnabled.get(subToCheck);
                boolean isInCiwlanOnlyMode = mIsInCiwlanOnlyMode.get(subToCheck);
                boolean isImsRegisteredOnCiwlan = mIsImsRegisteredOnCiwlan.get(subToCheck);
                if (isCiwlanEnabled && (isInCiwlanOnlyMode || !isCiwlanModeSupported)) {
                    Log.d(TAG, "isDialogNeeded: isRoaming = true, isInCall = true, isCiwlanEnabled = true" +
                            ", isInCiwlanOnlyMode = " + isInCiwlanOnlyMode +
                            ", isCiwlanModeSupported = " + isCiwlanModeSupported +
                            ", isImsRegisteredOnCiwlan = " + isImsRegisteredOnCiwlan);
                    // If IMS is registered over C_IWLAN-only mode, the device is in a call, and
                    // user is trying to disable roaming while UE is romaing, display a warning
                    // dialog that disabling roaming will cause a call drop.
                    if (isImsRegisteredOnCiwlan) {
                        mDialogType = RoamingDialogFragment.TYPE_DISABLE_CIWLAN_DIALOG;
                        return true;
                    }
                } else {
                    Log.d(TAG, "isDialogNeeded: C_IWLAN not enabled or not in C_IWLAN-only mode");
                }
            } else {
                Log.d(TAG, "isDialogNeeded: Not roaming or not in a call");
            }
        }
        return false;
    }

    private int subToCheckForCiwlanWarningDialog(int ndds, int dds) {
        int subToCheck = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (mIsSubInCall.get(ndds) && mIsCiwlanEnabled.get(ndds) &&
                (mIsInCiwlanOnlyMode.get(ndds) || !mIsCiwlanModeSupported.get(ndds)) &&
                mIsImsRegisteredOnCiwlan.get(ndds)) {
            subToCheck = ndds;
        } else {
            subToCheck = dds;
        }
        return subToCheck;
    }

    @Override
    public boolean isChecked() {
        if (mTelephonyManager == null) {
            return false;
        }
        return mMobileNetworkInfoEntity == null ? false
                : mMobileNetworkInfoEntity.isDataRoamingEnabled;
    }

    public void init(FragmentManager fragmentManager, int subId, MobileNetworkInfoEntity entity) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
        mMobileNetworkInfoEntity = entity;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        final TelephonyManager telephonyManager = mTelephonyManager
                .createForSubscriptionId(mSubId);
        if (telephonyManager == null) {
            Log.w(TAG, "fail to init in sub" + mSubId);
            mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            return;
        }
        mTelephonyManager = telephonyManager;

        mDdsDataOptionStateTuner =
                new DdsDataOptionStateTuner(mTelephonyManager,
                        mSubscriptionManager,
                        () -> updateState(mSwitchPreference));
    }

    private void showDialog(int type) {
        final RoamingDialogFragment dialogFragment = RoamingDialogFragment.newInstance(
                mSwitchPreference.getTitle().toString(), type, mSubId,
                MobileNetworkSettings.isCiwlanModeSupported(mSubId));

        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    @VisibleForTesting
    public void setMobileNetworkInfoEntity(MobileNetworkInfoEntity mobileNetworkInfoEntity) {
        mMobileNetworkInfoEntity = mobileNetworkInfoEntity;
    }

    @Override
    public void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = mobileNetworkInfoEntityList;
        mMobileNetworkInfoEntityList.forEach(entity -> {
            if (Integer.parseInt(entity.subId) == mSubId) {
                mMobileNetworkInfoEntity = entity;
                update();
                refreshSummary(mSwitchPreference);
                return;
            }
        });
    }

    @Override
    public void onDataRoamingChanged(int subId, boolean enabled) {
        if (subId != mSubId) {
            Log.d(TAG, "onDataRoamingChanged - wrong subId : " + subId + " / " + enabled);
            return;
        }
        update();
    }
}
