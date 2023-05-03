/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.NR;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.WifiCallingQueryImsState;

import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;

import java.util.List;
import java.util.Objects;

/**
 * Preference controller for "Backup Calling"
 **/
public class BackupCallingPreferenceController extends TelephonyTogglePreferenceController
        implements DefaultLifecycleObserver {

    private static final String LOG_TAG = "BackupCallingPrefCtrl";
    private static final String DIALOG_TAG = "BackupCallingDialog";

    private final String PREFERENCE_KEY = "backup_calling_key";

    private Preference mPreference;
    private PreferenceScreen mScreen;
    private Context mContext;
    private PhoneTelephonyCallback mTelephonyCallback;
    private ExtTelephonyManager mExtTelephonyManager;
    private Integer mCallState;
    private boolean mServiceConnected = false;
    private SubscriptionManager mSubscriptionManager;
    private int mDialogType;
    private FragmentManager mFragmentManager;
    private TelephonyManager mTelephonyManager;
    @VisibleForTesting
    boolean mDialogNeeded = false;
    private Uri mCrossSimUri;
    private ContentObserver mCrossSimObserver;

    /**
     * Class constructor of backup calling.
     *
     * @param context of settings
     * @param key assigned within UI entry of XML file
     **/
    public BackupCallingPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context.getApplicationContext();
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyCallback = new PhoneTelephonyCallback();
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {

        // Since ExtTelephony service is called from TelephonyComponentFactory,
        // onConnected() is called even before mExtTelephonyManager.connectService
        // as per ExtTelephonyManager#connectService()
        @Override
        public void onConnected() {
            Log.d(LOG_TAG, "mExtTelManagerServiceCallback: service connected");
            mServiceConnected = true;
            displayPreference(mScreen);
            if (mScreen != null) {
                updateState((SwitchPreference) mScreen.findPreference(PREFERENCE_KEY));
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(LOG_TAG, "mExtTelManagerServiceCallback: service disconnected");
            mServiceConnected = false;
        }
    };

    /**
     * Initialization based on given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     **/
    public BackupCallingPreferenceController init(FragmentManager fragmentManager, int subId) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
        mTelephonyManager = getTelephonyManager();
        mCrossSimUri = Uri.withAppendedPath(
                SubscriptionManager.CROSS_SIM_ENABLED_CONTENT_URI, String.valueOf(mSubId));
        return this;
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        registerCrossSimObserver();
        mTelephonyCallback.register(mContext, mSubId);
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        unregisterCrossSimObserver();
        mTelephonyCallback.unregister();
    }

    private void registerCrossSimObserver() {
        if (mCrossSimObserver == null) {
            mCrossSimObserver = new ContentObserver(mContext.getMainThreadHandler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (mCrossSimUri.equals(uri)) {
                        Log.d(LOG_TAG, "CIWLAN UI preference changed");
                        if (mPreference != null) {
                            updateState(mPreference);
                        }
                    }
                }
            };
        }
        if (mCrossSimUri != null && mCrossSimObserver != null) {
            mContext.getContentResolver().registerContentObserver(mCrossSimUri, true,
                    mCrossSimObserver);
        }
    }

    private void unregisterCrossSimObserver() {
        if (mCrossSimObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mCrossSimObserver);
        }
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

    private class PhoneTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            // Assign the current call state to show the correct preference state even before the
            // first onCallStateChanged() by initial registration.
            if (mTelephonyManager != null) {
                mCallState = mTelephonyManager.getCallState(subId);
                mTelephonyManager.registerTelephonyCallback(context.getMainExecutor(), this);
            }
        }

        public void unregister() {
            mCallState = null;
            if (mTelephonyManager != null) {
                mTelephonyManager.unregisterTelephonyCallback(this);
            }
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        // Check for the dynamic capability from modem.
        if (!hasBackupCallingFeature(subId)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        List<SubscriptionInfo> subIdList = getActiveSubscriptionList();
        SubscriptionInfo subInfo = getSubscriptionInfoFromList(subIdList, subId);
        if (subInfo == null) {  // given subId is not actives
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }

    /**
     * Implementation of abstract methods
     **/
    @Override
    public boolean setChecked(boolean isChecked) {
        // Check UE's C_IWLAN configuration and the current preferred network type. If UE is in
        // C_IWLAN-only mode and the preferred network type does not contain LTE or NR, show a
        // dialog to change the preferred network type.
        mDialogNeeded = isDialogNeeded(isChecked);
        if (!mDialogNeeded) {
            // Update directly if we don't need dialog
            ImsMmTelManager imsMmTelMgr = getImsMmTelManager(mSubId);
            if (imsMmTelMgr == null) {
                return false;
            }
            try {
                imsMmTelMgr.setCrossSimCallingEnabled(isChecked);
            } catch (ImsException exception) {
                Log.e(LOG_TAG, "Failed to change C_IWLAN status to " + isChecked, exception);
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isDialogNeeded(boolean isChecked) {
        boolean isInCiwlanOnlyMode = false;
        // Warn on turning on C_IWLAN when an incompatible network is selected only on targets
        // that support getting the C_IWLAN config
        if (MobileNetworkSettings.isCiwlanModeSupported()) {
            isInCiwlanOnlyMode = MobileNetworkSettings.isInCiwlanOnlyMode();
        }
        if (!isInCiwlanOnlyMode) {
            return false;
        }
        boolean isCiwlanIncompatibleNetworkSelected = false;
        if (isChecked) {
            isCiwlanIncompatibleNetworkSelected = isCiwlanIncompatibleNetworkSelected();
        }
        Log.d(LOG_TAG, "isDialogNeeded: isChecked = " + isChecked +
                ", isCiwlanIncompatibleNetworkSelected = " + isCiwlanIncompatibleNetworkSelected);
        if (isChecked && isCiwlanIncompatibleNetworkSelected) {
            mDialogType =
                    BackupCallingDialogFragment.TYPE_ENABLE_CIWLAN_INCOMPATIBLE_NW_TYPE_DIALOG;
            return true;
        }
        return false;
    }

    private boolean isCiwlanIncompatibleNetworkSelected() {
        long preferredRaf = mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        return ((LTE & preferredRaf) == 0 && (NR & preferredRaf) == 0);
    }

    private void showDialog(int type) {
        final BackupCallingDialogFragment dialogFragment = BackupCallingDialogFragment.newInstance(
                mPreference.getTitle().toString(), type, mSubId);
        dialogFragment.show(mFragmentManager, DIALOG_TAG);
    }

    /**
     * Implementation of abstract methods
     **/
    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (mDialogNeeded) {
                showDialog(mDialogType);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager(mSubId);
        if (imsMmTelMgr == null) {
            return false;
        }
        try {
            return imsMmTelMgr.isCrossSimCallingEnabled();
        } catch (ImsException exception) {
            Log.w(LOG_TAG, "Failed to get C_IWLAN status", exception);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if ((mCallState == null) || (preference == null) ||
                (!(preference instanceof SwitchPreference))) {
            Log.d(LOG_TAG, "Skip update under mCallState = " + mCallState);
            return;
        }
        SubscriptionInfo subInfo = getSubscriptionInfoFromActiveList(mSubId);

        mPreference = preference;

        final SwitchPreference switchPreference = (SwitchPreference) preference;
        // Gray out the setting during calls
        switchPreference.setEnabled(mCallState == TelephonyManager.CALL_STATE_IDLE);
        switchPreference.setChecked((subInfo != null) ? isChecked() : false);

        updateSummary(getLatestSummary(subInfo));
    }

    private String getLatestSummary(SubscriptionInfo subInfo) {
        return Objects.toString((subInfo == null) ? null
                : SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, mContext), "");
    }

    private void updateSummary(String displayName) {
        Preference preference = mPreference;
        if (preference == null) {
            return;
        }
        String summary = displayName;
        String finalText = String.format(
                getResourcesForSubId().getString(R.string.backup_calling_setting_summary),
                summary)
                .toString();
        preference.setSummary(finalText);
    }

    private boolean hasBackupCallingFeature(int subscriptionId) {
        return isCrossSimEnabledByPlatform(mContext, subscriptionId);
    }

    protected boolean isCrossSimEnabledByPlatform(Context context, int subscriptionId) {
        if (!mServiceConnected) {
            Log.d(LOG_TAG, "ExtTelephony service is not connected");
            return false;
        }

        try {
            if (!mExtTelephonyManager.isEpdgOverCellularDataSupported(
                    SubscriptionManager.getPhoneId(subscriptionId))) {
                Log.d(LOG_TAG, "Not supported by platform. subId = " + subscriptionId);
                return false;
            }
        } catch(RemoteException ex) {
            Log.d(LOG_TAG, "isEpdgOverCellularDataSupported Exception" + ex);
            return false;
        }

        // TODO : Change into API which created for accessing
        //        com.android.ims.ImsManager#isCrossSimEnabledByPlatform()
        if ((new WifiCallingQueryImsState(context, subscriptionId)).isWifiCallingSupported()) {
            PersistableBundle bundle = getCarrierConfigForSubId(subscriptionId);
            return (bundle != null) && bundle.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL,
                    false /*default*/);
        }
        Log.d(LOG_TAG, "Not supported by framework. subId = " + subscriptionId);
        return false;
    }

    private ImsMmTelManager getImsMmTelManager(int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            return null;
        }
        ImsManager imsMgr = mContext.getSystemService(ImsManager.class);
        return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(subId);
    }

    private List<SubscriptionInfo> getActiveSubscriptionList() {
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        return SubscriptionUtil.getActiveSubscriptions(subscriptionManager);
    }

    private SubscriptionInfo getSubscriptionInfoFromList(
            List<SubscriptionInfo> subInfoList, int subId) {
        for (SubscriptionInfo subInfo : subInfoList) {
            if ((subInfo != null) && (subInfo.getSubscriptionId() == subId)) {
                return subInfo;
            }
        }
        return null;
    }

    private SubscriptionInfo getSubscriptionInfoFromActiveList(int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            return null;
        }
        return getSubscriptionInfoFromList(getActiveSubscriptionList(), subId);
    }
}
