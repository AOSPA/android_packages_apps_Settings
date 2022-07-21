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
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

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
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.GlobalSettingsChangeListener;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Roaming"
 */
public class RoamingPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "RoamingController";
    private static final String DIALOG_TAG = "MobileDataDialog";

    private RestrictedSwitchPreference mSwitchPreference;
    private TelephonyManager mTelephonyManager;
    public SubscriptionManager mSubscriptionManager;
    private CarrierConfigManager mCarrierConfigManager;

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
    int mDialogType;

    public RoamingPreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
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

    @Override
    public void onStart() {
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

    @Override
    public void onStop() {
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
        return subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
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
        final RestrictedSwitchPreference switchPreference = (RestrictedSwitchPreference) preference;
        if (!switchPreference.isDisabledByAdmin()) {
            switchPreference.setEnabled(mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            switchPreference.setChecked(isChecked());

            if (mDdsDataOptionStateTuner.isDisallowed()) {
                Log.d(TAG, "nDDS voice call in ongoing");
                // we will get inside this block only when the current instance is for the DDS
                if (isChecked()) {
                    Log.d(TAG, "Do not allow the user to turn off DDS data roaming");
                    preference.setEnabled(false);
                    preference.setSummary(
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
        final boolean isRoamingEnabled = mTelephonyManager.isDataRoamingEnabled();
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                mSubId);
        // Need dialog if we need to turn on roaming and the roaming charge indication is allowed
        if (!isRoamingEnabled && (carrierConfig == null || !carrierConfig.getBoolean(
                CarrierConfigManager.KEY_DISABLE_CHARGE_INDICATION_BOOL))) {
            mDialogType = RoamingDialogFragment.TYPE_ENABLE_DIALOG;
            return true;
        }
        // IMS via internet over another subscription
        boolean isCallIdle = !mDdsDataOptionStateTuner.isInNonDdsVoiceCall();
        IImsRegistration imsRegistrationImpl = mTelephonyManager.getImsRegistration(
                mSubscriptionManager.getSlotIndex(mSubId), FEATURE_MMTEL);
        boolean isImsRegisteredOverCiwlan = false;
        try {
            isImsRegisteredOverCiwlan = imsRegistrationImpl.getRegistrationTechnology() ==
                    REGISTRATION_TECH_CROSS_SIM;
        } catch (RemoteException ex) {
            Log.e(TAG, "getRegistrationTechnology failed", ex);
        }
        Log.d(TAG, "isDialogNeeded: isRoamingEnabled=" + isRoamingEnabled + ", isCallIdle=" +
                isCallIdle + ", isImsRegisteredOverCiwlan=" + isImsRegisteredOverCiwlan);
        // If device is in a C_IWLAN call, and the user is trying to disable roaming, display the
        // warning dialog.
        if (isRoamingEnabled && !isCallIdle && isImsRegisteredOverCiwlan) {
            mDialogType = RoamingDialogFragment.TYPE_DISABLE_CIWLAN_DIALOG;
            return true;
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        if (mTelephonyManager == null) {
            return false;
        }
        return mTelephonyManager.isDataRoamingEnabled();
    }

    public void init(FragmentManager fragmentManager, int subId) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
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
        final RoamingDialogFragment dialogFragment = RoamingDialogFragment.newInstance(type,
                mSubId);

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
}
