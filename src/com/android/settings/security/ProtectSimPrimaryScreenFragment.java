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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.ConvertToEsimPreferenceController;

import java.util.List;

/**
 * Fragment for showing the PrimarySwitchPreference toggle for toggling SIM protection
 * (in general) on/off.
 */
public class ProtectSimPrimaryScreenFragment extends DashboardFragment implements
        EnterSimPinDialogFragment.SimPinEntryListener {
    private static final String TAG = "AutoSimPinLockFrg";

    @Nullable
    private SimPinProtectionToggleController mController;

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mController = use(SimPinProtectionToggleController.class);
        mController.setFragment(this);

        int subId = getSubId(context);
        ConvertToEsimPreferenceController convertToEsimController =
                use(ConvertToEsimPreferenceController.class);
        if (convertToEsimController != null) {
            convertToEsimController.init(subId, this);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.automatic_sim_lock_protection_settings;
    }

    private static int getSubId(Context context) {
        SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        final List<SubscriptionInfo> subInfoList = subscriptionManager
                == null ? null : subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.isActive() && !subInfo.isEmbedded()) {
                return subInfo.getSubscriptionId();
            }
        }

        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUTOMATIC_SIM_PIN_MANAGEMENT;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mController != null) {
            mController.storeEnrollmentState(outState);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (mController != null) {
            mController.loadEnrollmentState(bundle);
        }
    }

    /**
     * To be called by the SIM PIN entry dialog to proceed with enrollment after the user
     * has provided the PIN.
     *
     * @param pin The current SIM PIN as provided by the user.
     */
    @Override
    public void onPinEntered(String pin) {
        if (mController == null) {
            Log.w(TAG, "Controller is not initialized, cannot process PIN");
            return;
        }
        mController.handlePinEntered(pin);
    }

    /**
     * To be called by the SIM PIN entry dialog when the user cancels PIN entry, thus
     * aborting the enrollment.
     */
    @Override
    public void onEntryCancelled() {
        if (mController != null) {
            mController.cancelEnrollment();
        }
    }
}
