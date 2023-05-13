/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.network.DefaultSubscriptionReceiver;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.ArrayList;
import java.util.List;

import org.codeaurora.internal.IExtTelephony;

/**
 * This implements common controller functionality for a Preference letting the user see/change
 * what mobile network subscription is used by default for some service controlled by the
 * SubscriptionManager. This can be used for services such as Calls or SMS.
 */
public abstract class DefaultSubscriptionController extends TelephonyBasePreferenceController
        implements LifecycleObserver, Preference.OnPreferenceChangeListener,
        MobileNetworkRepository.MobileNetworkCallback,
        DefaultSubscriptionReceiver.DefaultSubscriptionListener {
    private static final String TAG = "DefaultSubController";

    protected ListPreference mPreference;
    protected SubscriptionManager mManager;
    protected TelecomManager mTelecomManager;
    protected MobileNetworkRepository mMobileNetworkRepository;
    protected LifecycleOwner mLifecycleOwner;
    private DefaultSubscriptionReceiver mDataSubscriptionChangedReceiver;

    private static final String EMERGENCY_ACCOUNT_HANDLE_ID = "E";
    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT =
            new ComponentName("com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService");
    private boolean mIsRtlMode;

    protected TelephonyManager mTelephonyManager;

    //String keys for data preference lookup
    private static final String LIST_DATA_PREFERENCE_KEY = "data_preference";

    private int mPhoneCount;
    private PhoneStateListener[] mPhoneStateListener;
    private int[] mCallState;
    private ArrayList<SubscriptionInfo> mSelectableSubs;

    List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();

    public DefaultSubscriptionController(Context context, String preferenceKey, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        super(context, preferenceKey);
        mManager = context.getSystemService(SubscriptionManager.class);
        mIsRtlMode = context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mDataSubscriptionChangedReceiver = new DefaultSubscriptionReceiver(context, this);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mTelephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneCount = mTelephonyManager.getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mPhoneCount];
        mCallState = new int[mPhoneCount];
        mSelectableSubs = new ArrayList<SubscriptionInfo>();
    }

    /** @return SubscriptionInfo for the default subscription for the service, or null if there
     * isn't one. */
    protected abstract SubscriptionInfoEntity getDefaultSubscriptionInfo();

    /** @return the id of the default subscription for the service, or
     * SubscriptionManager.INVALID_SUBSCRIPTION_ID if there isn't one. */
    protected abstract int getDefaultSubscriptionId();

    /** Called to change the default subscription for the service. */
    protected abstract void setDefaultSubscription(int subscriptionId);

    protected boolean isAskEverytimeSupported() {
        return true;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        boolean visible = false;
        if (mSelectableSubs != null && mSelectableSubs.size() > 1) {
            visible = true;
        } else {
            visible = false;
        }

        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        registerPhoneStateListener();
        mMobileNetworkRepository.updateEntity();
        // Can not get default subId from database until get the callback, add register by subId
        // later.
        mMobileNetworkRepository.addRegisterBySubId(getDefaultSubscriptionId());
        mDataSubscriptionChangedReceiver.registerReceiver();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mMobileNetworkRepository.removeRegister(this);
        unRegisterPhoneStateListener();
        mDataSubscriptionChangedReceiver.unRegisterReceiver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateEntries();
    }

    @Override
    protected void refreshSummary(Preference preference) {
        // Currently, cannot use ListPreference.setSummary() when the summary contains user
        // generated string, because ListPreference.getSummary() is using String.format() to format
        // the summary when the summary is set by ListPreference.setSummary().
        if (preference != null) {
            preference.setSummaryProvider(pref -> getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        final PhoneAccountHandle handle = getDefaultCallingAccountHandle();
        if ((handle != null) && (!isCallingAccountBindToSubscription(handle))) {
            // display VoIP account in summary when configured through settings within dialer
            return getLabelFromCallingAccount(handle);
        }
        final SubscriptionInfoEntity info = getDefaultSubscriptionInfo();
        if (info != null) {
            // display subscription based account
            return info.uniqueName;
        } else {
            if (isAskEverytimeSupported()) {
                return mContext.getString(R.string.calls_and_sms_ask_every_time);
            } else {
                return "";
            }
        }
    }

    @VisibleForTesting
    void updateEntries() {
        if (mPreference == null) {
            return;
        }

        updateSubStatus();
        if (mSelectableSubs.isEmpty()) {
            Log.d(TAG, "updateEntries: mSelectable subs is empty");
            return;
        }

        if (!isAvailable()) {
            mPreference.setVisible(false);
            return;
        }
        mPreference.setVisible(true);

        // TODO(b/135142209) - for now we need to manually ensure we're registered as a change
        // listener, because this might not have happened during displayPreference if
        // getAvailabilityStatus returned CONDITIONALLY_UNAVAILABLE at the time.
        mPreference.setOnPreferenceChangeListener(this);

        // We'll have one entry for each available subscription, plus one for a "ask me every
        // time" entry at the end.
        final ArrayList<CharSequence> displayNames = new ArrayList<>();
        final ArrayList<CharSequence> subscriptionIds = new ArrayList<>();
        List<SubscriptionInfoEntity> list = getSubscriptionInfoList();

        if (mSelectableSubs.size() == 1) {
            mPreference.setEnabled(false);
            mPreference.setSummaryProvider(pref ->
                    SubscriptionUtil.getUniqueSubscriptionDisplayName(
                    mSelectableSubs.get(0), mContext));
            return;
        }

        final int serviceDefaultSubId = getDefaultSubscriptionId();
        boolean subIsAvailable = false;

        for (SubscriptionInfo sub : mSelectableSubs) {
            if (sub.isOpportunistic()) {
                continue;
            }
            displayNames.add(SubscriptionUtil.getUniqueSubscriptionDisplayName(sub, mContext));
            final int subId = sub.getSubscriptionId();
            subscriptionIds.add(Integer.toString(subId));
            if (subId == serviceDefaultSubId) {
                subIsAvailable = true;
            }
        }
        if (TextUtils.equals(getPreferenceKey(), LIST_DATA_PREFERENCE_KEY)) {
            boolean isEcbmEnabled = mTelephonyManager.getEmergencyCallbackMode();
            boolean isScbmEnabled = TelephonyProperties.in_scbm().orElse(false);
            boolean isSmartDdsEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.SMART_DDS_SWITCH, 0) == 1;

            if (!isSmartDdsEnabled) {
                mPreference.setEnabled(isCallStateIdle() && !isEcbmEnabled && !isScbmEnabled &&
                        (!TelephonyUtils.isSubsidyFeatureEnabled(mContext) ||
                        TelephonyUtils.allowUsertoSetDDS(mContext)));
            } else {
                mPreference.setEnabled(false);
            }

        } else {
            if (isAskEverytimeSupported()) {
                // Add the extra "Ask every time" value at the end.
                displayNames.add(mContext.getString(R.string.calls_and_sms_ask_every_time));
                subscriptionIds.add(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
            }
        }

        mPreference.setEntries(displayNames.toArray(new CharSequence[0]));
        mPreference.setEntryValues(subscriptionIds.toArray(new CharSequence[0]));

        if (subIsAvailable) {
            mPreference.setValue(Integer.toString(serviceDefaultSubId));
        } else {
            mPreference.setValue(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
    }

    /**
     * Get default calling account
     *
     * @return current calling account {@link PhoneAccountHandle}
     */
    public PhoneAccountHandle getDefaultCallingAccountHandle() {
        final PhoneAccountHandle currentSelectPhoneAccount =
                getTelecomManager().getUserSelectedOutgoingPhoneAccount();
        if (currentSelectPhoneAccount == null) {
            return null;
        }
        final List<PhoneAccountHandle> accountHandles =
                getTelecomManager().getCallCapablePhoneAccounts(false);
        final PhoneAccountHandle emergencyAccountHandle = new PhoneAccountHandle(
                PSTN_CONNECTION_SERVICE_COMPONENT, EMERGENCY_ACCOUNT_HANDLE_ID);
        if (currentSelectPhoneAccount.equals(emergencyAccountHandle)) {
            return null;
        }
        for (PhoneAccountHandle handle : accountHandles) {
            if (currentSelectPhoneAccount.equals(handle)) {
                return currentSelectPhoneAccount;
            }
        }
        return null;
    }

    @VisibleForTesting
    TelecomManager getTelecomManager() {
        if (mTelecomManager == null) {
            mTelecomManager = mContext.getSystemService(TelecomManager.class);
        }
        return mTelecomManager;
    }

    @VisibleForTesting
    PhoneAccount getPhoneAccount(PhoneAccountHandle handle) {
        return getTelecomManager().getPhoneAccount(handle);
    }

    /**
     * Check if calling account bind to subscription
     *
     * @param handle {@link PhoneAccountHandle} for specific calling account
     */
    public boolean isCallingAccountBindToSubscription(PhoneAccountHandle handle) {
        final PhoneAccount account = getPhoneAccount(handle);
        if (account == null) {
            return false;
        }
        return account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
    }

    /**
     * Get label from calling account
     *
     * @param handle to get label from {@link PhoneAccountHandle}
     * @return label of calling account
     */
    public CharSequence getLabelFromCallingAccount(PhoneAccountHandle handle) {
        CharSequence label = null;
        final PhoneAccount account = getPhoneAccount(handle);
        if (account != null) {
            label = account.getLabel();
        }
        if (label != null) {
            label = mContext.getPackageManager().getUserBadgedLabel(label, handle.getUserHandle());
        }
        return (label != null) ? label : "";
    }

    @VisibleForTesting
    protected List<SubscriptionInfoEntity> getSubscriptionInfoList() {
        return mSubInfoEntityList;
    }

    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        for (int i = 0; i < mPhoneCount; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                callStateIdle = false;
            }
        }
        return callStateIdle;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int subscriptionId = Integer.parseInt((String) newValue);
        setDefaultSubscription(subscriptionId);
        refreshSummary(mPreference);
        return true;
    }

    boolean isRtlMode() {
        return mIsRtlMode;
    }

    @Override
    public void onActiveSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        if (mSelectableSubs != null) mSelectableSubs.clear();
        updateSubStatus();

        mSubInfoEntityList = subInfoEntityList;
        updateEntries();
        refreshSummary(mPreference);
    }

    private void registerPhoneStateListener() {
        //To make sure subinfo is added, before registering for call state change
        updateSubStatus();

        for (int i = 0; i < mSelectableSubs.size(); i++) {
             int subId = mSelectableSubs.get(i).getSubscriptionId();
             TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
             tm.listen(getPhoneStateListener(i),
                     PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void unRegisterPhoneStateListener() {
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                mTelephonyManager.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(int phoneId) {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        final int i = phoneId;
        mPhoneStateListener[phoneId]  = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                mCallState[i] = state;
                updateEntries();
            }
        };
        return mPhoneStateListener[phoneId];
    }

    private void updateSubStatus() {
        if (!mSelectableSubs.isEmpty()) {
            return;
        }

        for (int i = 0; i < mPhoneCount; ++i) {
            final SubscriptionInfo sir = mManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            if (sir != null) {
                mSelectableSubs.add(sir);
            }
        }
    }

    @Override
    public void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
    }

    @Override
    public void onDefaultVoiceChanged(int defaultVoiceSubId) {
        updateEntries();
        refreshSummary(mPreference);
    }

    @Override
    public void onDefaultSmsChanged(int defaultSmsSubId) {
        updateEntries();
        refreshSummary(mPreference);
    }
}
