/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Base class for controllers of SIM protection-related preferences.
 */
public abstract class BaseSimProtectionModePreferenceController extends
        BasePreferenceController implements
        SelectorWithWidgetPreference.OnClickListener {
    protected static final String TAG = "SimPinController";

    @Nullable protected SelectorWithWidgetPreference mPreference;
    protected final int mSubId;
    @Nullable protected BaseSimPinFragment mFragment;

    protected AutoManagedSimPinHelper mAutoManagedSimPinHelper;
    protected TelephonyManager mTelephonyManager;

    public BaseSimProtectionModePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mAutoManagedSimPinHelper = new AutoManagedSimPinHelper(mContext);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubId = mAutoManagedSimPinHelper.getSubIdForFirstPhysicalSimSlot();
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFlagEnabled = android.security.Flags.autoSimPinManagement();
        SubscriptionManager sm = mContext.getSystemService(SubscriptionManager.class);
        boolean isValidSubscription = sm.isValidSubscriptionId(mSubId);
        boolean isEmbeddedSim = isValidSubscription && sm.getActiveSubscriptionInfo(
                mSubId).isEmbedded();

        return isFlagEnabled && isValidSubscription && !isEmbeddedSim ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
        mPreference.setChecked(isModeCurrentlyActive());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference).setChecked(isModeCurrentlyActive());
        }
    }

    /**
     * Must be implemented by sub-classes to indicate whether the SIM protection mode is currently
     * active or not. Used to indicate whether the selector preference is checked.
     * @return true if mode is currently active.
     */
    @VisibleForTesting
    public abstract boolean isModeCurrentlyActive();

    public void setFragment(BaseSimPinFragment fragment) {
        mFragment = fragment;
    }

    /**
     * Sets the state for the preference associated with the controller and forces a state update
     * for all preferences in the fragment. Requires all controllers in the fragment to implement
     * the {@code updateState} method.
     *
     * @param checked whether the preference is checked or not.
     */
    protected void setPreferenceCheckedAndRefresh(boolean checked) {
        if (mPreference == null) {
            return;
        }
        mPreference.setChecked(checked);
        if (mFragment != null) {
            mFragment.forceUpdatePreferences();
        }
    }
}
