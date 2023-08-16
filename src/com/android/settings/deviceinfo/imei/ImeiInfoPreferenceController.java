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
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.PhoneNumberSummaryPreference;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.TelephonyUtils;
import com.android.settings.Utils;

import com.qti.extphone.QtiImeiInfo;

/**
 * Controller that manages preference for single and multi sim devices.
 */
public class ImeiInfoPreferenceController extends BasePreferenceController {

    private static final String TAG = "ImeiInfoPreferenceController";

    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";
    public static final String DEFAULT_KEY = "imei_info";

    private TelephonyManager mTelephonyManager;
    private Fragment mFragment;
    private SlotSimStatus mSlotSimStatus;
    private QtiImeiInfo mQtiImeiInfo[];

    public ImeiInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(Fragment fragment, SlotSimStatus slotSimStatus) {
        mFragment = fragment;
        mSlotSimStatus = slotSimStatus;
        TelephonyUtils.connectExtTelephonyService(mContext);
    }

    private boolean isMultiSim() {
        return (mSlotSimStatus != null) && (mSlotSimStatus.size() > 1);
    }

    private int keyToSlotIndex(String key) {
        int simSlot = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        try {
            simSlot = Integer.valueOf(key.replace(DEFAULT_KEY, "")) - 1;
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
        if ((!SubscriptionUtil.isSimHardwareVisible(mContext)) || (mSlotSimStatus == null)) {
            return;
        }
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);
        Preference preference = category.findPreference(DEFAULT_KEY);
        if (!isAvailable() || preference == null || !preference.isVisible()) {
            return;
        }

        int imeiPreferenceOrder = preference.getOrder();
        category.removePreference(preference);

        // Add additional preferences for each imei slot in the device
        for (int simSlotNumber = 0; simSlotNumber < mSlotSimStatus.size(); simSlotNumber++) {
            Preference multiImeiPreference = createNewPreference(screen.getContext());
            multiImeiPreference.setOrder(imeiPreferenceOrder + 1 + simSlotNumber);
            multiImeiPreference.setKey(DEFAULT_KEY + (1 + simSlotNumber));
            multiImeiPreference.setEnabled(true);
            multiImeiPreference.setCopyingEnabled(true);
            category.addPreference(multiImeiPreference);
       }

        /* current code will parse the slot id from the preference key value
         * the key for the CT new preference is populated as below
         *  current phone count + 1:  new one MEID preference for slot 0
         *  current phone count + 2:  new one IMEI preference for slot 0
         *  current phone count + 3:  new one IMEI preference for slot 1
         */
        int phoneCount = mTelephonyManager.getPhoneCount();
        if (Utils.isSupportCTPA(mContext) && phoneCount >= 2) {
            final int slot0PhoneType = mTelephonyManager.getCurrentPhoneTypeForSlot(0);
            final int slot1PhoneType = mTelephonyManager.getCurrentPhoneTypeForSlot(1);
            int order = imeiPreferenceOrder + phoneCount + 1;
            if (PHONE_TYPE_CDMA != slot0PhoneType && PHONE_TYPE_CDMA != slot1PhoneType) {
                addPreferenceNotInList(screen, order, DEFAULT_KEY + (phoneCount + 1),
                        category, true);
            } else if (PHONE_TYPE_CDMA == slot0PhoneType){
                addPreferenceNotInList(screen, order, DEFAULT_KEY + (phoneCount + 2),
                        category, false);
            } else if (PHONE_TYPE_CDMA == slot1PhoneType) {
                addPreferenceNotInList(screen, order, DEFAULT_KEY + (phoneCount + 3),
                        category, false);
            }
        }
    }

    private void addPreferenceNotInList(PreferenceScreen screen, int order, String key,
                                        PreferenceCategory category, boolean isCDMAPhone) {
        Preference multiSimPreference = createNewPreference(screen.getContext());
        multiSimPreference.setOrder(order);
        multiSimPreference.setKey(key);
        multiSimPreference.setEnabled(true);
        multiSimPreference.setCopyingEnabled(true);
        category.addPreference(multiSimPreference);
        int slotNumber = convertSlotId(keyToSlotIndex(key));
        if (isCDMAPhone) {
            multiSimPreference.setTitle(getTitleForCdmaPhone(slotNumber, false));
            multiSimPreference.setSummary(getSummary());
        } else {
            multiSimPreference.setTitle(getTitleForGsmPhone(slotNumber, false));
            multiSimPreference.setSummary(getSummary());
        }
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference(preference, keyToSlotIndex(preference.getKey()));
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.device_info_protected_single_press);
    }

    private CharSequence getSummary(int simSlot) {
        final int phoneType = getPhoneType(simSlot);
        if (Utils.isSupportCTPA(mContext)) {
            simSlot = convertSlotId(simSlot);

            // only can obtain the MEID by slot 0
            if (PHONE_TYPE_CDMA == phoneType) {
                simSlot = 0;
            }
        }
        return phoneType == PHONE_TYPE_CDMA ? mTelephonyManager.getMeid(simSlot)
                : getImei(simSlot);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = keyToSlotIndex(preference.getKey());
        if (simSlot < 0) {
            return false;
        }

        if (!Utils.isSupportCTPA(mContext)) {
            ImeiInfoDialogFragment.show(mFragment, simSlot, preference.getTitle().toString());
        }

        preference.setSummary(getSummary(simSlot));
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isAvailable = SubscriptionUtil.isSimHardwareVisible(mContext) &&
                mContext.getSystemService(UserManager.class).isAdminUser() &&
                !Utils.isWifiOnly(mContext);
        return isAvailable ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @VisibleForTesting
    protected void updatePreference(Preference preference, int simSlot) {
        preference.setTitle(getTitle(simSlot));
        preference.setSummary(getSummary());
    }

    private String getImei(int slot) {
        String imei = null;
        if (isMinHalVersion2_1()) {
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
        if (isMinHalVersion2_1()) {
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

    private int convertSlotId(int simSlot) {
        final int phoneCount = mTelephonyManager.getPhoneCount();
        if (simSlot < phoneCount) {
            return simSlot;
        }

        if (simSlot == phoneCount || simSlot == phoneCount + 1) {
            return 0;
        }

        return 1;
    }

    private CharSequence getTitle(int simSlot) {
        int convertedSlotId = simSlot;
        if (Utils.isSupportCTPA(mContext)) {
            convertedSlotId = convertSlotId(simSlot);
        }

        boolean isPrimaryImei = isMultiSim() && isPrimaryImei(simSlot);
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? getTitleForCdmaPhone(convertedSlotId, isPrimaryImei)
                : getTitleForGsmPhone(convertedSlotId, isPrimaryImei);
    }

    public int getPhoneType(int slotIndex) {
        if (Utils.isSupportCTPA(mContext)) {
            final int phoneCount = mTelephonyManager.getPhoneCount();
            if (slotIndex == phoneCount) {
                return PHONE_TYPE_CDMA;
            } else if (slotIndex > phoneCount) {
                return PHONE_TYPE_GSM;
            }
            return mTelephonyManager.getCurrentPhoneTypeForSlot(slotIndex);
        }
        SubscriptionInfo subInfo = getSubscriptionInfo(slotIndex);
        return mTelephonyManager.getCurrentPhoneType(subInfo != null ? subInfo.getSubscriptionId()
                : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new PhoneNumberSummaryPreference(context);
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

