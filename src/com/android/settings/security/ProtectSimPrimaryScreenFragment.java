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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.network.telephony.ConvertToEsimPreferenceController;
import com.android.settingslib.widget.IntroPreference;

import java.util.List;

/**
 * Fragment for showing the PrimarySwitchPreference toggle for toggling SIM protection
 * (in general) on/off.
 */
public class ProtectSimPrimaryScreenFragment extends BaseSimPinFragment {
    // Key of the IntroPreference preference for the first active slot.
    private static final String FIRST_SLOT_HEADER = "first_active_slot_intro";
    // Key of the IntroPreference preference for the second active slot.
    private static final String SECOND_SLOT_HEADER = "second_active_slot_intro";

    private static final String FIRST_SLOT_CATEGORY = "category_first_sim_card_slot";
    private static final String SECOND_SLOT_CATEGORY = "category_second_sim_card_slot";
    // There are multiple controllers for changing the SIM PIN preferences - one for each slot.
    private List<ChangeSimPinPreferenceController> mChangePinControllers;
    // There are multiple SIM PIN toggle controllers - one for each slot and potentially multiple
    // for a given slot, to handle manual and automatic PIN management modes.
    private List<SimPinProtectionToggleController> mControllers;
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mAutoManagedSimPinHelper = new AutoManagedSimPinHelper(context);

        mControllers = useAll(SimPinProtectionToggleController.class);
        for (SimPinProtectionToggleController controller : mControllers) {
            controller.setFragment(this);
            // If the controller's preference key indicates it's for the first slot, set slot
            // index as 0. Otherwise, check if it's for the second slot, and set slot index as 1.
            if (controller.getPreferenceKey().endsWith("_0")) {
                controller.setSlotIndex(0);
            } else if (controller.getPreferenceKey().endsWith("_1")) {
                controller.setSlotIndex(1);
            }
        }

        mChangePinControllers = useAll(ChangeSimPinPreferenceController.class);
        for (ChangeSimPinPreferenceController controller : mChangePinControllers) {
            controller.setFragment(this);
            // If the controller's preference key indicates it's for the first slot, set slot
            // index as 0. Otherwise, check if it's for the second slot, and set slot index as 1.
            if (controller.getPreferenceKey().endsWith("_0")) {
                controller.setSlotIndex(0);
            } else if (controller.getPreferenceKey().endsWith("_1")) {
                controller.setSlotIndex(1);
            }
        }

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

    @Override
    public void onResume() {
        super.onResume();

        initFirstActiveSlotHeader();
        initSecondActiveSlotHeader();
    }

    @VisibleForTesting
    public void setAutoManagedSimPinHelperForTesting(AutoManagedSimPinHelper helper) {
        mAutoManagedSimPinHelper = helper;
    }

    private void initSlotHeader(String categoryKey, String slotHeaderKey, int slotIndex) {
        IntroPreference introPreference = findPreference(slotHeaderKey);
        PreferenceCategory category = findPreference(categoryKey);
        if (introPreference == null || category == null) {
            Log.d(TAG, "Missing preference for category " + categoryKey + " or " + slotHeaderKey);
            return;
        }

        int[] activeSlots = mAutoManagedSimPinHelper.getActiveSlots();

        if (activeSlots == null || activeSlots.length < (slotIndex + 1)) {
            introPreference.setVisible(false);
            category.setVisible(false);
            return;
        }

        int activeSlot = activeSlots[slotIndex];

        SubscriptionManager subscriptionManager =
                getContext().getSystemService(SubscriptionManager.class);
        SubscriptionInfo info = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(
                activeSlot);
        if (info != null) {
            introPreference.setTitle(info.getCarrierName());
        } else {
            introPreference.setTitle(
                    getContext().getResources().getString(R.string.sim_editor_title,
                            slotIndex + 1));
        }
        introPreference.setVisible(true);
        category.setVisible(true);
    }

    private void initFirstActiveSlotHeader() {
        initSlotHeader(FIRST_SLOT_CATEGORY, FIRST_SLOT_HEADER, 0);
    }

    private void initSecondActiveSlotHeader() {
        initSlotHeader(SECOND_SLOT_CATEGORY, SECOND_SLOT_HEADER, 1);
    }
}
