/*
 * Copyright (C) 2017 The Android Open Source Project
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

// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
/*
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
package com.android.settings.deviceinfo.imei;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode

import android.content.Context;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import android.text.TextUtils;
import android.util.Log;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
import android.util.Pair;
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.PhoneNumberUtil;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.flags.Flags;
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
import com.android.settings.network.telephony.MobileNetworkUtils;
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import com.android.settings.network.telephony.TelephonyUtils;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.

// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import com.qti.extphone.QtiImeiInfo;

// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller that manages preference for single and multi sim devices.
 *
 * @deprecated Since PHONE_TYPE_CDMA has been deprecated in TelephonyManager, this controller
 * will be deprecated and removed after V.
 */
@Deprecated(forRemoval = true)
public class ImeiInfoPreferenceController extends BasePreferenceController {

// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    private static final String TAG = "ImeiInfoPreferenceController";
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.

    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";
    public static final String DEFAULT_KEY = "imei_info";
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
    private static final String DEFAULT_MEID_KEY = "meid_info";
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone

    private TelephonyManager mTelephonyManager;
    private Fragment mFragment;
    private SlotSimStatus mSlotSimStatus;
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    private QtiImeiInfo mQtiImeiInfo[];
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
    private boolean mIsDsdsToSsConfigValid;
    private int mSlotCount = -1;
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
    private boolean mIsCdmaSupported = true;
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone

    public ImeiInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(Fragment fragment, SlotSimStatus slotSimStatus) {
        mFragment = fragment;
        mSlotSimStatus = slotSimStatus;
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
        TelephonyUtils.connectExtTelephonyService(mContext);
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
        mIsDsdsToSsConfigValid = TelephonyUtils.isDsdsToSsConfigValid(mContext);
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        mSlotCount = TelephonyUtils.getUiccSlotsCount(mContext);
        mIsCdmaSupported = MobileNetworkUtils.isCdmaSupported(mContext);
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
    }

    private boolean isMultiSim() {
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        return (mSlotSimStatus != null) && (mSlotSimStatus.size() > 1)
                || (mIsDsdsToSsConfigValid && mSlotCount > 1);
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
    }

    private int keyToSlotIndex(String key) {
        int simSlot = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        try {
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
            if (key.startsWith(DEFAULT_MEID_KEY)) {
                simSlot = Integer.valueOf(key.replace(DEFAULT_MEID_KEY, "")) - 1;
            } else {
                simSlot = Integer.valueOf(key.replace(DEFAULT_KEY, "")) - 1;
            }
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
        } catch (Exception exception) {
            Log.i(TAG, "Invalid key : " + key);
        }
        return simSlot;
    }

    private SubscriptionInfo getSubscriptionInfo(int simSlot) {
        return (mSlotSimStatus == null) ? null : mSlotSimStatus.getSubscriptionInfo(simSlot);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable() || (mSlotSimStatus == null)) {
            return;
        }
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        mSlotCount = TelephonyUtils.getUiccSlotsCount(mContext);
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
        PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
        Preference preference = screen.findPreference(DEFAULT_KEY);
        if (preference == null || !preference.isVisible()) {
            return;
        }

        int imeiPreferenceOrder = preference.getOrder();
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
        category.removePreference(preference);
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        final int slotCount = mIsDsdsToSsConfigValid? mSlotCount : mSlotSimStatus.size();
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone

        if (Flags.catalystMyDeviceInfoPrefScreen()) {
            return;
        }

        // Add additional preferences for each imei slot in the device
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
        // Loop through all active SIMs or all slots if mIsDsdsToSsConfigValid is enabled
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        for (int simSlotNumber = 0; simSlotNumber < slotCount; simSlotNumber++) {
            if (simSlotNumber == 0 && isCdmaPreferenceRequired()) {
                addPreferenceForCdma(screen, category, simSlotNumber, imeiPreferenceOrder);
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
// QTI_BEGIN: 2025-03-23: Android_UI: Remove MEID in about phone for CT
            }

// QTI_END: 2025-03-23: Android_UI: Remove MEID in about phone for CT
            Preference multiImeiPreference = createNewPreference(screen.getContext());
            multiImeiPreference.setOrder(imeiPreferenceOrder + 1 + simSlotNumber);
            multiImeiPreference.setKey(DEFAULT_KEY + (1 + simSlotNumber));
            multiImeiPreference.setEnabled(true);
            multiImeiPreference.setCopyingEnabled(true);

            category.addPreference(multiImeiPreference);
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
            multiImeiPreference.setTitle(getTitle(simSlotNumber));
            multiImeiPreference.setSummary(getSummary(simSlotNumber));
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
        }
    }

// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
    private void addPreferenceForCdma(PreferenceScreen screen, PreferenceCategory category,
            int slotId, int order) {
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
        Preference multiSimPreference = createNewPreference(screen.getContext());
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        multiSimPreference.setOrder(order + slotId);
        multiSimPreference.setKey(DEFAULT_MEID_KEY + (1 + slotId));
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
        multiSimPreference.setEnabled(true);
        multiSimPreference.setCopyingEnabled(true);
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
// QTI_BEGIN: 2021-09-23: Android_UI: Settings: Insert new IMEI/MEID preference into device detail category
        category.addPreference(multiSimPreference);
// QTI_END: 2021-09-23: Android_UI: Settings: Insert new IMEI/MEID preference into device detail category
        multiSimPreference.setTitle(getTitleForCdmaPhone(slotId));
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        multiSimPreference.setSummary(getMeid(slotId));
    }

    private boolean isCdmaPreferenceRequired() {
        return mIsCdmaSupported && Utils.isSupportCTPA(mContext);
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference(preference, keyToSlotIndex(preference.getKey()));
    }

    private CharSequence getSummary(int simSlot) {
        List<String> imeiList = getImeiList();
        if (imeiList.isEmpty()) {
            return "";
        }
        String imei = "";
        if (simSlot >= imeiList.size()) {
            imei = imeiList.getFirst();
        } else {
            imei = imeiList.get(simSlot);
        }
        return PhoneNumberUtil.expandByTts(imei);
    }

    private String getImeiBySlot(int simSlot) {
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? mTelephonyManager.getMeid(simSlot)
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
                : getImei(simSlot);
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    }

// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
    private String getMeid(int simSlot) {
        return mTelephonyManager.getMeid(simSlot);
    }

// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
    private List<String> getImeiListBySlot() {
        List<String> imeiListBySlot = new ArrayList<>();

        for (int i = 0; i < mSlotSimStatus.size(); i++) {
            String imeiItem = getImeiBySlot(i);
            if (!TextUtils.isEmpty(imeiItem)) {
                imeiListBySlot.add(imeiItem);
            }
        }
        return imeiListBySlot;
    }

    private String getPrimaryImei() {
        String primaryImei = "";
        try {
            primaryImei = mTelephonyManager.getPrimaryImei();
        } catch (Exception exception) {
            Log.i(TAG, "PrimaryImei not available. " + exception);
        }
        return primaryImei;
    }

    /**
     * As per GSMA specification TS37, below Primary IMEI requirements are mandatory to support
     * TS37_2.2_REQ_5
     * TS37_2.2_REQ_8 (Attached the document has description about this test cases)
     *
     * b/434700998, using the lower IMEI as the primary IMEI.
     * IMEI 1 = primary IMEI i.e. lower IMEI
     * IMEI 2 = non-primary IMEI
     */
    private List<String> getImeiList() {
        List<String> imeiList = getImeiListBySlot();
        String primaryImei = getPrimaryImei();

        if (!TextUtils.isEmpty(primaryImei)) {
            imeiList.remove(primaryImei);
            Collections.sort(imeiList);
            imeiList.addFirst(primaryImei);
        }
        return imeiList;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = keyToSlotIndex(preference.getKey());
        if (simSlot < 0) {
            return false;
        }
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        if (preference.getKey().startsWith(DEFAULT_MEID_KEY)) {
            preference.setSummary(getMeid(simSlot));
        } else {
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
// QTI_BEGIN: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode
            ImeiInfoDialogFragment.show(mFragment, simSlot, preference.getTitle().toString());
// QTI_END: 2023-08-11: Android_UI: Settings: Fix MEID not displayed in CT mode

// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
            preference.setSummary(getSummary(simSlot));
        }
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Utils.isMobileDataCapable(mContext) && !Utils.isVoiceCapable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mContext.getSystemService(UserManager.class).isAdminUser()) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @VisibleForTesting
    protected void updatePreference(Preference preference, int simSlot) {
        if (simSlot < 0) {
            preference.setVisible(false);
            return;
        }
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        if (preference.getKey().startsWith(DEFAULT_MEID_KEY)) {
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
            preference.setTitle(getTitleForCdmaPhone(simSlot));
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
            preference.setSummary(getMeid(simSlot));
            return;
        }
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
        preference.setTitle(getTitle(simSlot));
        preference.setSummary(getSummary(simSlot));
    }

// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    private String getImei(int slot) {
        String imei = null;
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
        try {
// QTI_END: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
            if (isMinHalVersion2_1() && !mIsDsdsToSsConfigValid) {
// QTI_END: 2025-02-26: Telephony: Show both IMEIs when device is with single SIM
// QTI_BEGIN: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
                imei = mTelephonyManager.getImei(slot);
            } else {
                if (mQtiImeiInfo == null) {
                    mQtiImeiInfo = TelephonyUtils.getImeiInfo();
                }
                if (mQtiImeiInfo != null) {
                    for (int i = 0; i < mQtiImeiInfo.length; i++) {
                        if (null != mQtiImeiInfo[i] && mQtiImeiInfo[i].getSlotId() == slot) {
                            imei = mQtiImeiInfo[i].getImei();
                            break;
                        }
// QTI_END: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
                    }
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
                }
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
                if (TextUtils.isEmpty(imei)) {
                    imei = mTelephonyManager.getImei(slot);
                }
// QTI_END: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
            }
// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
// QTI_BEGIN: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
        } catch (Exception exception) {
            Log.i(TAG, "Imei not available. " + exception);
// QTI_END: 2023-10-11: Telephony: Avoid IllegalStateException in Settings app
// QTI_BEGIN: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
        }
        return imei;
    }

// QTI_END: 2021-11-30: Telephony: Primary Imei Status Support Settings->AboutPhone.
    private CharSequence getTitleForGsmPhone(int simSlot) {
        // using simSlot as index
        return isMultiSim() ? mContext.getString(R.string.imei_multi_sim, simSlot + 1)
                : mContext.getString(R.string.status_imei);
    }

    private CharSequence getTitleForCdmaPhone(int simSlot) {
        // using simSlot as index
        return isMultiSim() ? mContext.getString(R.string.meid_multi_sim, simSlot + 1)
                : mContext.getString(R.string.status_meid_number);
    }

    private CharSequence getTitle(int simSlot) {
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? getTitleForCdmaPhone(simSlot)
                : getTitleForGsmPhone(simSlot);
    }

    public int getPhoneType(int slotIndex) {
// QTI_BEGIN: 2025-06-10: Telephony: Refactor MEID display function for About phone
        if (!mIsCdmaSupported || Utils.isSupportCTPA(mContext)) {
            return PHONE_TYPE_GSM;
        }
        if (mIsDsdsToSsConfigValid && slotIndex < mSlotCount
                && slotIndex == mTelephonyManager.getPhoneCount()) {
            return PHONE_TYPE_GSM;
// QTI_END: 2025-06-10: Telephony: Refactor MEID display function for About phone
        }
        SubscriptionInfo subInfo = getSubscriptionInfo(slotIndex);
        return mTelephonyManager.getCurrentPhoneType(subInfo != null ? subInfo.getSubscriptionId()
                : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new Preference(context);
    }
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone

    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isMinHalVersion2_1() {
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
// QTI_BEGIN: 2023-03-27: Telephony: Use correct API to get HAL Version
        Pair<Integer, Integer> radioVersion = mTelephonyManager.getHalVersion(
                TelephonyManager.HAL_SERVICE_MODEM);
// QTI_END: 2023-03-27: Telephony: Use correct API to get HAL Version
// QTI_BEGIN: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);
        return (halVersion > makeRadioVersion(2, 0)) ? true:false;
    }
// QTI_END: 2023-03-23: Telephony: Primary IMEI is not displayed in About Phone
}

