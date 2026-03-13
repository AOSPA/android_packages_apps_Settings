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
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.network.telephony.ConvertToEsimPreferenceController;

import java.util.List;

/**
 * Fragment for showing the PrimarySwitchPreference toggle for toggling SIM protection
 * (in general) on/off.
 */
public class ProtectSimPrimaryScreenFragment extends BaseSimPinFragment {
    @Nullable
    private ChangeSimPinPreferenceController mChangePinController;
    private List<SimPinProtectionToggleController> mControllers;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mControllers = useAll(SimPinProtectionToggleController.class);
        for (SimPinProtectionToggleController controller : mControllers) {
            controller.setFragment(this);
        }

        mChangePinController = use(ChangeSimPinPreferenceController.class);
        mChangePinController.setFragment(this);

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        for (SimPinProtectionToggleController controller : mControllers) {
            if (controller.isAvailable()) {
                controller.storeEnrollmentState(outState);
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        for (SimPinProtectionToggleController controller : mControllers) {
            if (controller.isAvailable()) {
                controller.loadEnrollmentState(bundle);
            }
        }
    }
}
