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

/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.deviceinfo.imei;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;

import android.content.Context;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.flags.Flags;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyUtils;

import com.qti.extphone.QtiImeiInfo;

/**
 * Controller that manages preference for single and multi sim devices.
 *
 * @deprecated Since PHONE_TYPE_CDMA has been deprecated in TelephonyManager, this controller
 * will be deprecated and removed after V.
 */
@Deprecated(forRemoval = true)
public class ImeiInfoPreferenceController extends BasePreferenceController {

    private static final String TAG = "ImeiInfoPreferenceController";

    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";
    public static final String DEFAULT_KEY = "imei_info";
    private static final String DEFAULT_MEID_KEY = "meid_info";

    private TelephonyManager mTelephonyManager;
    private Fragment mFragment;
    private SlotSimStatus mSlotSimStatus;
    private QtiImeiInfo mQtiImeiInfo[];
    private boolean mIsDsdsToSsConfigValid;
    private int mSlotCount = -1;
    private boolean mIsCdmaSupported = true;

    public ImeiInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(Fragment fragment, SlotSimStatus slotSimStatus) {
        mFragment = fragment;
        mSlotSimStatus = slotSimStatus;
        TelephonyUtils.connectExtTelephonyService(mContext);
        mIsDsdsToSsConfigValid = TelephonyUtils.isDsdsToSsConfigValid();
        mSlotCount = TelephonyUtils.getUiccSlotsCount(mContext);
        mIsCdmaSupported = MobileNetworkUtils.isCdmaSupported(mContext);
    }

    private boolean isMultiSim() {
        return (mSlotSimStatus != null) && (mSlotSimStatus.size() > 1)
                || (mIsDsdsToSsConfigValid && mSlotCount > 1);
    }

    private int keyToSlotIndex(String key) {
        int simSlot = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        try {
            if (key.startsWith(DEFAULT_MEID_KEY)) {
                simSlot = Integer.valueOf(key.replace(DEFAULT_MEID_KEY, "")) - 1;
            } else {
                simSlot = Integer.valueOf(key.replace(DEFAULT_KEY, "")) - 1;
            }
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
        mSlotCount = TelephonyUtils.getUiccSlotsCount(mContext);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);
        Preference preference = screen.findPreference(DEFAULT_KEY);
        if (preference == null || !preference.isVisible()) {
            return;
        }

        int imeiPreferenceOrder = preference.getOrder();
        category.removePreference(preference);
        final int slotCount = mIsDsdsToSsConfigValid? mSlotCount : mSlotSimStatus.size();

        // Add additional preferences for each imei slot in the device
        // Loop through all active SIMs or all slots if mIsDsdsToSsConfigValid is enabled
        for (int simSlotNumber = 0; simSlotNumber < slotCount; simSlotNumber++) {
            if (simSlotNumber == 0 && isCdmaPreferenceRequired()) {
                addPreferenceForCdma(screen, category, simSlotNumber, imeiPreferenceOrder);
            }

            Preference multiImeiPreference = createNewPreference(screen.getContext());
            multiImeiPreference.setOrder(imeiPreferenceOrder + 1 + simSlotNumber);
            multiImeiPreference.setKey(DEFAULT_KEY + (1 + simSlotNumber));
            multiImeiPreference.setEnabled(true);
            multiImeiPreference.setCopyingEnabled(true);

            category.addPreference(multiImeiPreference);
            multiImeiPreference.setTitle(getTitle(simSlotNumber));
            multiImeiPreference.setSummary(getSummary(simSlotNumber));
        }
    }

    private void addPreferenceForCdma(PreferenceScreen screen, PreferenceCategory category,
            int slotId, int order) {
        Preference multiSimPreference = createNewPreference(screen.getContext());
        multiSimPreference.setOrder(order + slotId);
        multiSimPreference.setKey(DEFAULT_MEID_KEY + (1 + slotId));
        multiSimPreference.setEnabled(true);
        multiSimPreference.setCopyingEnabled(true);
        category.addPreference(multiSimPreference);
        multiSimPreference.setTitle(getTitleForCdmaPhone(slotId, false));
        multiSimPreference.setSummary(getMeid(slotId));
    }

    private boolean isCdmaPreferenceRequired() {
        return mIsCdmaSupported && Utils.isSupportCTPA(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference(preference, keyToSlotIndex(preference.getKey()));
    }

    private CharSequence getSummary(int simSlot) {
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? mTelephonyManager.getMeid(simSlot)
                : getImei(simSlot);
    }

    private String getMeid(int simSlot) {
        return mTelephonyManager.getMeid(simSlot);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = keyToSlotIndex(preference.getKey());
        if (simSlot < 0) {
            return false;
        }
        if (preference.getKey().startsWith(DEFAULT_MEID_KEY)) {
            preference.setSummary(getMeid(simSlot));
        } else {
            ImeiInfoDialogFragment.show(mFragment, simSlot, preference.getTitle().toString());

            preference.setSummary(getSummary(simSlot));
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.catalystMyDeviceInfoPrefScreen()) {
            return UNSUPPORTED_ON_DEVICE;
        }
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
        if (preference.getKey().startsWith(DEFAULT_MEID_KEY)) {
            preference.setTitle(getTitleForCdmaPhone(simSlot, false));
            preference.setSummary(getMeid(simSlot));
            return;
        }
        preference.setTitle(getTitle(simSlot));
        preference.setSummary(getSummary(simSlot));
    }

    private String getImei(int slot) {
        String imei = null;
        try {
            if (isMinHalVersion2_1() && !mIsDsdsToSsConfigValid) {
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
                    }
                }
                if (TextUtils.isEmpty(imei)) {
                    imei = mTelephonyManager.getImei(slot);
                }
            }
        } catch (Exception exception) {
            Log.i(TAG, "Imei not available. " + exception);
        }
        return imei;
    }

    private CharSequence getTitleForGsmPhone(int simSlot, boolean isPrimaryImei) {
        int titleId = isPrimaryImei ? R.string.imei_multi_sim_primary : R.string.imei_multi_sim;
        return isMultiSim() ? mContext.getString(titleId, simSlot + 1)
                : mContext.getString(R.string.status_imei);
    }

    private CharSequence getTitleForCdmaPhone(int simSlot, boolean isPrimaryImei) {
        int titleId = isPrimaryImei ? R.string.meid_multi_sim_primary : R.string.meid_multi_sim;
        return isMultiSim() ? mContext.getString(titleId, simSlot + 1)
                : mContext.getString(R.string.status_meid_number);
    }

    protected boolean isPrimaryImei(int simSlot) {
        CharSequence imei = getSummary(simSlot);
        if (imei == null) {
            return false;
        }
        String primaryImei = null;
        if (isMinHalVersion2_1() && !mIsDsdsToSsConfigValid) {
            try {
                primaryImei = mTelephonyManager.getPrimaryImei();
            } catch (Exception exception) {
                Log.i(TAG, "PrimaryImei not available. " + exception);
            }
            return ((primaryImei != null) && primaryImei.equals(imei.toString()));
        } else {
            if (mQtiImeiInfo == null) {
                mQtiImeiInfo = TelephonyUtils.getImeiInfo();
            }
            if (mQtiImeiInfo != null) {
                for (int i = 0; i < mQtiImeiInfo.length; i++) {
                    if (null != mQtiImeiInfo[i] && mQtiImeiInfo[i].getSlotId() == simSlot &&
                            mQtiImeiInfo[i].getImeiType() == QtiImeiInfo.IMEI_TYPE_PRIMARY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private CharSequence getTitle(int simSlot) {

        boolean isPrimaryImei = isMultiSim() && isPrimaryImei(simSlot);

        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? getTitleForCdmaPhone(simSlot, isPrimaryImei)
                : getTitleForGsmPhone(simSlot, isPrimaryImei);
    }

    public int getPhoneType(int slotIndex) {
        if (!mIsCdmaSupported || Utils.isSupportCTPA(mContext)) {
            return PHONE_TYPE_GSM;
        }
        if (mIsDsdsToSsConfigValid && slotIndex < mSlotCount
                && slotIndex == mTelephonyManager.getPhoneCount()) {
            return PHONE_TYPE_GSM;
        }
        SubscriptionInfo subInfo = getSubscriptionInfo(slotIndex);
        return mTelephonyManager.getCurrentPhoneType(subInfo != null ? subInfo.getSubscriptionId()
                : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new Preference(context);
    }

    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isMinHalVersion2_1() {
        Pair<Integer, Integer> radioVersion = mTelephonyManager.getHalVersion(
                TelephonyManager.HAL_SERVICE_MODEM);
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);
        return (halVersion > makeRadioVersion(2, 0)) ? true:false;
    }
}

