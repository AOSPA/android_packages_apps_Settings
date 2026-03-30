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
// QTI_BEGIN: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
import android.telephony.PhoneStateListener;
// QTI_END: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
// QTI_BEGIN: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
import android.util.Log;
// QTI_END: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
import android.view.View;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.network.DefaultSubscriptionReceiver;
import com.android.settings.network.MobileNetworkRepository;
// QTI_BEGIN: 2023-03-22: Telephony: Fix sim display disorder issue in Settings.
import com.android.settings.network.SubscriptionUtil;
// QTI_END: 2023-03-22: Telephony: Fix sim display disorder issue in Settings.
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import java.util.ArrayList;
import java.util.List;

// QTI_BEGIN: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
import org.codeaurora.internal.IExtTelephony;

// QTI_END: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
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
    protected MobileNetworkRepository mMobileNetworkRepository;
    protected LifecycleOwner mLifecycleOwner;
    private DefaultSubscriptionReceiver mDataSubscriptionChangedReceiver;

    private boolean mIsRtlMode;

    List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();

    public DefaultSubscriptionController(Context context, String preferenceKey, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        this(context, preferenceKey);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public DefaultSubscriptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mManager = context.getSystemService(SubscriptionManager.class);
        mIsRtlMode = context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mDataSubscriptionChangedReceiver = new DefaultSubscriptionReceiver(context, this);
    }

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
        if (Flags.isDualSimOnboardingEnabled()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
        final boolean visible = mSubInfoEntityList != null && mSubInfoEntityList.size() > 1;
// QTI_END: 2023-05-09: Telephony: Refactor data preference
// QTI_BEGIN: 2020-01-28: Telephony: Hide calls/SMS SIM prefs if SimSettings app is present
        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
// QTI_END: 2020-01-28: Telephony: Hide calls/SMS SIM prefs if SimSettings app is present
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mMobileNetworkRepository.updateEntity();
        mDataSubscriptionChangedReceiver.registerReceiver();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mMobileNetworkRepository.removeRegister(this);
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
        if (preference != null && !mSubInfoEntityList.isEmpty()) {
            preference.setSummaryProvider(pref -> getSummary());
        }
    }

    @VisibleForTesting
    void updateEntries() {
        if (mPreference == null) {
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
        if (list.isEmpty()) return;

// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
        if (list.size() == 1) {
// QTI_END: 2023-05-09: Telephony: Refactor data preference
            mPreference.setEnabled(false);
// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
            mPreference.setSummaryProvider(pref -> list.get(0).uniqueName);
// QTI_END: 2023-05-09: Telephony: Refactor data preference
            return;
        }

        final int serviceDefaultSubId = getDefaultSubscriptionId();
        boolean subIsAvailable = false;

// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
        for (SubscriptionInfoEntity sub : list) {
            if (sub.isOpportunistic) {
// QTI_END: 2023-05-09: Telephony: Refactor data preference
                continue;
            }
// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
            displayNames.add(sub.uniqueName);
            final int subId = Integer.parseInt(sub.subId);
            subscriptionIds.add(sub.subId);
// QTI_END: 2023-05-09: Telephony: Refactor data preference
            if (subId == serviceDefaultSubId) {
                subIsAvailable = true;
            }
        }

        mPreference.setEnabled(true);
// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
        updatePreferenceState(mPreference);
// QTI_END: 2023-05-09: Telephony: Refactor data preference

// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
        if (isAskEverytimeSupported()) {
            // Add the extra "Ask every time" value at the end.
            displayNames.add(mContext.getString(R.string.calls_and_sms_ask_every_time));
            subscriptionIds.add(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
// QTI_END: 2023-05-09: Telephony: Refactor data preference
// QTI_BEGIN: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature
        }
// QTI_END: 2020-03-22: Telephony: FR61513: Add support to enable sim on/off feature

        mPreference.setEntries(displayNames.toArray(new CharSequence[0]));
        mPreference.setEntryValues(subscriptionIds.toArray(new CharSequence[0]));

        if (subIsAvailable) {
            mPreference.setValue(Integer.toString(serviceDefaultSubId));
        } else {
            mPreference.setValue(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
    }

// QTI_BEGIN: 2023-05-09: Telephony: Refactor data preference
    protected void updatePreferenceState(Preference preference) {
    }

// QTI_END: 2023-05-09: Telephony: Refactor data preference
    @VisibleForTesting
    protected List<SubscriptionInfoEntity> getSubscriptionInfoList() {
        return mSubInfoEntityList;
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
        mSubInfoEntityList = subInfoEntityList;
        updateEntries();
        refreshSummary(mPreference);
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
