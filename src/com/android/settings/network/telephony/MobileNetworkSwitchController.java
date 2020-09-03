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

import android.content.Context;
import android.sysprop.TelephonyProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

/** This controls a switch to allow enabling/disabling a mobile network */
public class MobileNetworkSwitchController extends BasePreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver {
    private static final String TAG = "MobileNetworkSwitchCtrl";
    private SwitchBar mSwitchBar;
    private int mSubId;
    private SubscriptionsChangeListener mChangeListener;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;
    private SubscriptionInfo mSubInfo = null;
    private int mPhoneId;
    private int mCallState;

    public MobileNetworkSwitchController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
        mTelephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void init(Lifecycle lifecycle, int subId) {
        lifecycle.addObserver(this);
        mSubId = subId;
        mPhoneId = mSubscriptionManager.getSlotIndex(mSubId);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mChangeListener.start();
        registerPhoneStateListener();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mChangeListener.stop();
        unRegisterPhoneStateListener();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference pref = screen.findPreference(mPreferenceKey);
        mSwitchBar = pref.findViewById(R.id.switch_bar);
        mSwitchBar.setSwitchBarText(R.string.mobile_network_use_sim_on,
                R.string.mobile_network_use_sim_off);

        mSwitchBar.getSwitch().setOnBeforeCheckedChangeListener((toggleSwitch, isChecked) -> {
            // TODO b/135222940: re-evaluate whether to use
            // mSubscriptionManager#isSubscriptionEnabled
        int uiccStatus = PrimaryCardAndSubsidyLockUtils.getUiccCardProvisioningStatus(mPhoneId);
        Log.d(TAG, "displayPreference: mSubId=" + mSubId + ", mSubInfo=" + mSubInfo +
                 ", uiccStatus=" + uiccStatus);
            if ((mSubInfo != null &&
                    (uiccStatus == PrimaryCardAndSubsidyLockUtils.CARD_PROVISIONED) != isChecked) &&
                    (!mSubscriptionManager.setSubscriptionEnabled(mSubId, isChecked))) {
                return true;
            }
            return false;
        });
        update();
    }

    private void update() {
        if (mSwitchBar == null) {
            return;
        }

        if (mTelephonyManager.getActiveModemCount() == 1 && !mSubscriptionManager.
                canDisablePhysicalSubscription()) {
            Log.d(TAG, "update: Hide SIM option for 1.4 HAL in single sim");
            mSwitchBar.hide();
            return;
        }

        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (info.getSubscriptionId() == mSubId) {
                mSubInfo = info;
                break;
            }
        }

        boolean isEcbmEnabled = TelephonyProperties.in_ecm_mode().orElse(false);
        if ((TelephonyManager.CALL_STATE_IDLE != mCallState) || isEcbmEnabled) {
            Log.d(TAG, "update: disable switchbar, isEcbmEnabled=" + isEcbmEnabled +
                    ", callstate=" + mCallState);
            mSwitchBar.setEnabled(false);
        } else {
            mSwitchBar.setEnabled(true);
        }

        // For eSIM, we always want the toggle. If telephony stack support disabling a pSIM
        // directly, we show the toggle.
        if (mSubInfo == null) {
            mSwitchBar.hide();
        } else {
            mSwitchBar.show();
            int uiccStatus = PrimaryCardAndSubsidyLockUtils.getUiccCardProvisioningStatus(mPhoneId);
            mSwitchBar.setCheckedInternal(uiccStatus == PrimaryCardAndSubsidyLockUtils.CARD_PROVISIONED);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;

    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        update();
    }

    private void registerPhoneStateListener() {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
        tm.listen(getPhoneStateListener(),
                    PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void unRegisterPhoneStateListener() {
        if (mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
    }

    private PhoneStateListener getPhoneStateListener() {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        // ToDo : Add subtext on disabled preference to let user know that default data sim cannot
        // be changed while call is going on
        mPhoneStateListener  = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                mCallState = state;
                update();
            }
        };
        return mPhoneStateListener;
    }
}
