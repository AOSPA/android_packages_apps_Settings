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
 * Copyright (c) 2022-2023 Qualcomm Innovation Center, Inc. All rights reserved.
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
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.GlobalSettingsChangeListener;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

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

    /**
     * There're 2 listeners both activated at the same time.
     * For project that access DATA_ROAMING, only first listener is functional.
     * For project that access "DATA_ROAMING + subId", first listener will be stopped when receiving
     * any onChange from second listener.
     */
    private GlobalSettingsChangeListener mListener;
    private GlobalSettingsChangeListener mListenerForSubId;

    private DdsDataOptionStateTuner mDdsDataOptionStateTuner;

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
        if (mListener == null) {
            mListener = new GlobalSettingsChangeListener(mContext,
                    Settings.Global.DATA_ROAMING) {
                public void onChanged(String field) {
                    updateState(mSwitchPreference);
                }
            };
        }
        stopMonitorSubIdSpecific();

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }

        mListenerForSubId = new GlobalSettingsChangeListener(mContext,
                Settings.Global.DATA_ROAMING + mSubId) {
            public void onChanged(String field) {
                stopMonitor();
                updateState(mSwitchPreference);
            }
        };

        // If the current instance is for the DDS, listen to the call state changes on nDDS.
        mDdsDataOptionStateTuner.register(mContext, mSubId);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mMobileNetworkRepository.removeRegister(this);
        stopMonitor();
        stopMonitorSubIdSpecific();
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
            mTelephonyManager.setDataRoamingEnabled(isChecked);
            return true;
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
            final boolean isRoaming = MobileNetworkSettings.isRoaming();
            final boolean isInVoiceCall = mTelephonyManager.getCallStateForSubscription() !=
                    TelephonyManager.CALL_STATE_IDLE;
            boolean isCiwlanModeSupported = false;
            boolean isInCiwlanOnlyMode = false;
            boolean isImsRegisteredOverCiwlan = false;
            if (isRoaming && isInVoiceCall) {
                isCiwlanModeSupported = MobileNetworkSettings.isCiwlanModeSupported();
                isInCiwlanOnlyMode = MobileNetworkSettings.isInCiwlanOnlyMode();
                if (isInCiwlanOnlyMode || !isCiwlanModeSupported) {
                    IImsRegistration imsRegistrationImpl = mTelephonyManager.getImsRegistration(
                            mSubscriptionManager.getSlotIndex(mSubId), FEATURE_MMTEL);
                    if (imsRegistrationImpl != null) {
                        try {
                            isImsRegisteredOverCiwlan =
                                    imsRegistrationImpl.getRegistrationTechnology() ==
                                            REGISTRATION_TECH_CROSS_SIM;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "getRegistrationTechnology failed", ex);
                        }
                    }
                    Log.d(TAG, "isDialogNeeded: isRoaming = " + isRoaming +
                            ", isInVoiceCall = " + isInVoiceCall +
                            ", isInCiwlanOnlyMode = " + isInCiwlanOnlyMode +
                            ", isCiwlanModeSupported = " + isCiwlanModeSupported +
                            ", isImsRegisteredOverCiwlan = " + isImsRegisteredOverCiwlan);
                    // If IMS is registered over C_IWLAN-only mode, the device is in a call, and
                    // user is trying to disable roaming while UE is romaing, display a warning
                    // dialog that disabling roaming will cause a call drop.
                    if (isImsRegisteredOverCiwlan) {
                        mDialogType = RoamingDialogFragment.TYPE_DISABLE_CIWLAN_DIALOG;
                        return true;
                    }
                } else {
                    Log.d(TAG, "isDialogNeeded: not in C_IWLAN-only mode");
                }
            } else {
                Log.d(TAG, "isDialogNeeded: not roaming or not in a call");
            }
        }
        return false;
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
                MobileNetworkSettings.isCiwlanModeSupported());

        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    private void stopMonitor() {
        if (mListener != null) {
            mListener.close();
            mListener = null;
        }
    }

    private void stopMonitorSubIdSpecific() {
        if (mListenerForSubId != null) {
            mListenerForSubId.close();
            mListenerForSubId = null;
        }
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
}
