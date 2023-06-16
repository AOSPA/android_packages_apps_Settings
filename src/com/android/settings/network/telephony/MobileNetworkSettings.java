/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * ​​​Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import static android.telephony.ims.feature.ImsFeature.FEATURE_MMTEL;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
import static android.telephony.NetworkRegistrationInfo.DOMAIN_PS;

// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import co.aospa.settings.network.telephony.Smart5gPreferenceController;

import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_ON_CIWLAN_CONFIG_CHANGE;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_GET_RADIO_ICON_RESPONSE;
import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_ON_RADIO_ICON_CHANGE;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
import android.content.BroadcastReceiver;
// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
import android.content.Context;
import android.content.Intent;
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
import android.content.IntentFilter;
// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
import android.os.Bundle;
// QTI_BEGIN: 2020-06-10: Telephony: Update SIM options according to SIM status
import android.os.RemoteException;
// QTI_END: 2020-06-10: Telephony: Update SIM options according to SIM status
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
import android.telephony.CarrierConfigManager;
// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import android.util.SparseArray;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Settings.MobileNetworkActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.datausage.BillingCyclePreferenceController;
import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.network.CarrierWifiTogglePreferenceController;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.cdma.CdmaSubscriptionPreferenceController;
import com.android.settings.network.telephony.cdma.CdmaSystemSelectPreferenceController;
import com.android.settings.network.telephony.gsm.AutoSelectPreferenceController;
import com.android.settings.network.telephony.gsm.OpenNetworkSelectPagePreferenceController;
import com.android.settings.network.telephony.gsm.SelectNetworkPreferenceController;
import com.android.settings.network.telephony.satellite.SatelliteSettingPreferenceController;
import com.android.settings.network.telephony.wificalling.CrossSimCallingViewModel;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.utils.SubIdBundleUtils;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.metadata.ValidatedKeyParameters;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;

// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import com.qti.extphone.CiwlanConfig;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import com.qti.extphone.ExtTelephonyManager;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import com.qti.extphone.RadioIcon;
import com.qti.extphone.RadioIconType;
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import com.qti.extphone.ServiceCallback;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import com.qti.extphone.Status;
import com.qti.extphone.Token;

// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import java.lang.Runnable;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs

import kotlin.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// LINT.IfChange
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MobileNetworkSettings extends AbstractMobileNetworkSettings implements
        MobileNetworkRepository.MobileNetworkCallback {

    private static final String LOG_TAG = "NetworkSettings";
    public static final int REQUEST_CODE_EXIT_ECM = 17;
    public static final int REQUEST_CODE_DELETE_SUBSCRIPTION = 18;
    @VisibleForTesting
    static final String KEY_CLICKED_PREF = "key_clicked_pref";

// QTI_BEGIN: 2023-01-30: Telephony: Fix for "Data preference" issue
    private static final String KEY_DATA_PREF = "data_preference";
// QTI_END: 2023-01-30: Telephony: Fix for "Data preference" issue
// QTI_BEGIN: 2024-09-19: Telephony: Use java implementations for Roaming preference
    private static final String KEY_ROAMING_PREF = "button_roaming_key";
// QTI_END: 2024-09-19: Telephony: Use java implementations for Roaming preference
    private static final String KEY_CALLS_PREF = "calls_preference";
    private static final String KEY_SMS_PREF = "sms_preference";
    private static final String KEY_MOBILE_DATA_PREF = "mobile_data_enable";
    private static final String KEY_CONVERT_TO_ESIM_PREF = "convert_to_esim";
    private static final String KEY_EID_KEY = "network_mode_eid_info";

// QTI_BEGIN: 2020-06-10: Telephony: Update SIM options according to SIM status
    // UICC provisioning status
    public static final int CARD_NOT_PROVISIONED = 0;
    public static final int CARD_PROVISIONED = 1;

// QTI_END: 2020-06-10: Telephony: Update SIM options according to SIM status
    //String keys for preference lookup
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "cdma_subscription_key";

// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria

// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    private static TelephonyManager mTelephonyManager;
    private static int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs

    private CdmaSystemSelectPreferenceController mCdmaSystemSelectPreferenceController;
    private CdmaSubscriptionPreferenceController mCdmaSubscriptionPreferenceController;

    private UserManager mUserManager;
    private String mClickedPrefKey;

    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();
    @Nullable
    private SubscriptionInfoEntity mSubscriptionInfoEntity;
    private MobileNetworkInfoEntity mMobileNetworkInfoEntity;

// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static ImsManager sImsMgr;
    private static String sPackageName;
    private static SparseArray<CiwlanConfig> sCiwlanConfig = new SparseArray();
    private static boolean sExtTelServiceConnected = false;
    private static Client sClient;
    private static ExtTelephonyManager sExtTelephonyManager;
    private static SubscriptionManager sSubscriptionManager;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
    private static boolean sIsMsimCiwlanSupported = false;
    private static int sInstanceCounter = 0;
// QTI_END: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
    private static RadioIcon sCurrentRadioIcon = null;
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static final ServiceCallback mExtTelServiceCallback = new ServiceCallback() {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        @Override
        public void onConnected() {
            Log.d(LOG_TAG, "ExtTelephony service connected");
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
            sExtTelServiceConnected = true;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
            int[] events = new int[] {
                    EVENT_ON_CIWLAN_CONFIG_CHANGE,
                    EVENT_GET_RADIO_ICON_RESPONSE,
                    EVENT_ON_RADIO_ICON_CHANGE};
            // Since sClient is static, it is shared between the sub-specific instances of this
            // class. Calling registerCallbackWithEvents with the same package name will return
            // null. As such, make sure to call it only if it hasn't been called yet.
            if (sClient == null) {
                sClient = sExtTelephonyManager.registerCallbackWithEvents(sPackageName,
                        mExtPhoneCallbackListener, events);
            }
// QTI_BEGIN: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
            sIsMsimCiwlanSupported = sExtTelephonyManager.isFeatureSupported(
                    ExtTelephonyManager.FEATURE_CIWLAN_MODE_PREFERENCE);
// QTI_END: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
            Log.d(LOG_TAG, "Client = " + sClient);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            getCiwlanConfig();
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            getCurrentRadioIcon();
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        }

        @Override
        public void onDisconnected() {
            Log.d(LOG_TAG, "ExtTelephony service disconnected");
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
            sCurrentRadioIcon = null;
// QTI_BEGIN: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
            sExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
// QTI_END: 2024-07-22: Telephony: Unregister to ExtPhoneCallback
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
            sExtTelServiceConnected = false;
            sClient = null;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        }
    };

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static ExtPhoneCallbackListener mExtPhoneCallbackListener =
            new ExtPhoneCallbackListener() {
        @Override
        public void onCiwlanConfigChange(int slotId, CiwlanConfig ciwlanConfig) {
           Log.d(LOG_TAG, "onCiwlanConfigChange: slotId = " + slotId + ", config = " +
                   ciwlanConfig);
           int subId = SubscriptionManager.getSubscriptionId(slotId);
           sCiwlanConfig.put(subId, ciwlanConfig);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        }
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs

        @Override
        public void onRadioIconResponse(int slotId, Token token, Status status, RadioIcon icon)
                throws RemoteException {
            if (status.get() == Status.SUCCESS && icon != null) {
                Log.d(LOG_TAG, "onRadioIconResponse slotId = " + slotId + ", icon = "
                        + icon.getType());
                sCurrentRadioIcon = icon;
            } else {
                Log.d(LOG_TAG, "onRadioIconResponse: Failed or null icon, status = "
                        + status.get());
            }
        }

        @Override
        public void onRadioIconChange(int slotId, RadioIcon icon)
                throws RemoteException {
            if (icon != null) {
                Log.d(LOG_TAG, "onRadioIconChange slotId = " + slotId + ", icon = "
                        + icon.getType());
                sCurrentRadioIcon = icon;
            } else {
                Log.d(LOG_TAG, "onRadioIconChange: Null icon received");
            }
        }
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    };

// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED
                    .equals(intent.getAction())) {
                ThreadUtils.postOnMainThread(() -> {
                    redrawPreferenceControllers();
                });
            }
        }
    };

// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
    private static void getCurrentRadioIcon() {
        mExecutor.execute(() -> {
            final int slotId = SubscriptionManager.getPhoneId(mSubId);
            if (slotId == SubscriptionManager.INVALID_PHONE_INDEX) {
                Log.e(LOG_TAG, "queryRadioIcon - invalid phone ID");
                return;
            }
            try {
                sExtTelephonyManager.queryRadioIcon(slotId, sClient);
                Log.d(LOG_TAG, "Queried initial radio icon state for NB-TN detection");
            } catch (Exception ex) {
                Log.e(LOG_TAG, "queryRadioIcon failed: " + ex);
            }
        });
    }

// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static CiwlanConfig getCiwlanConfig(int... subscriptionId) {
        // If subscriptionId is passed in, return the config belonging to that subId. Otherwise,
        // query the config for all active subscriptions.
        if (subscriptionId.length != 0 && sCiwlanConfig != null) {
            return sCiwlanConfig.get(subscriptionId[0]);
        }

// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        mExecutor.execute(new Runnable() {
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
            @Override
            public void run() {
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                // Query the C_IWLAN config of all active subscriptions
                int[] activeSubIdList = sSubscriptionManager.getActiveSubscriptionIdList();
                for (int i = 0; i < activeSubIdList.length; i++) {
                    try {
                        int subId = activeSubIdList[i];
                        sCiwlanConfig.put(subId, sExtTelephonyManager.getCiwlanConfig(
                                SubscriptionManager.getSlotIndex(subId)));
                    } catch (RemoteException ex) {
                        Log.e(LOG_TAG, "getCiwlanConfig exception", ex);
                    }
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
                }
            }
        });
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
        return null;
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static boolean isCiwlanEnabled(int subId) {
        ImsMmTelManager imsMmTelMgr = getImsMmTelManager(subId);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        if (imsMmTelMgr == null) {
            return false;
        }
        try {
            return imsMmTelMgr.isCrossSimCallingEnabled();
        } catch (ImsException exception) {
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            Log.e(LOG_TAG, "Failed to get C_IWLAN toggle status", exception);
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        }
        return false;
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private static ImsMmTelManager getImsMmTelManager(int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            Log.d(LOG_TAG, "getImsMmTelManager: subId unusable");
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
            return null;
        }
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        if (sImsMgr == null) {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            Log.d(LOG_TAG, "getImsMmTelManager: ImsManager null");
            return null;
        }
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        return sImsMgr.getImsMmTelManager(subId);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static boolean isInCiwlanOnlyMode(int subId) {
        if (sCiwlanConfig == null) {
            Log.d(LOG_TAG, "isInCiwlanOnlyMode: C_IWLAN config map null");
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
            return false;
        }
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        CiwlanConfig config = sCiwlanConfig.get(subId);
        if (config != null) {
            if (isRoaming(subId)) {
                return config.isCiwlanOnlyInRoam();
            }
            return config.isCiwlanOnlyInHome();
        } else {
            Log.d(LOG_TAG, "isInCiwlanOnlyMode: C_IWLAN config null for subId " + subId);
            return false;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        }
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static boolean isCiwlanModeSupported(int subId) {
        if (sCiwlanConfig == null) {
            Log.d(LOG_TAG, "isCiwlanModeSupported: C_IWLAN config map null");
            return false;
        }
        CiwlanConfig config = sCiwlanConfig.get(subId);
        if (config != null) {
            return config.isCiwlanModeSupported();
        } else {
            Log.d(LOG_TAG, "isCiwlanModeSupported: C_IWLAN config null for subId " + subId);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
            return false;
        }
    }

// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static boolean isImsRegisteredOnCiwlan(int subId) {
        if (mTelephonyManager == null) {
            Log.d(LOG_TAG, "isImsRegisteredOnCiwlan: TelephonyManager null");
            return false;
        }
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        IImsRegistration imsRegistrationImpl = tm.getImsRegistration(
                SubscriptionManager.getSlotIndex(subId), FEATURE_MMTEL);
        if (imsRegistrationImpl != null) {
            try {
                return imsRegistrationImpl.getRegistrationTechnology() ==
                        REGISTRATION_TECH_CROSS_SIM;
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "getRegistrationTechnology failed", ex);
            }
        }
        return false;
    }

    static boolean isMsimCiwlanSupported() {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
        Log.i(LOG_TAG, "isMsimCiwlanSupported = " + sIsMsimCiwlanSupported);
        return sIsMsimCiwlanSupported;
// QTI_END: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    }

    static boolean isRoaming(int subId) {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
        if (mTelephonyManager == null) {
            Log.d(LOG_TAG, "isRoaming: TelephonyManager null");
            return false;
        }
// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
        boolean nriRoaming = false;
// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        ServiceState serviceState = tm.getServiceState();
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Use the new API to check if the device is roaming
        if (serviceState != null) {
            NetworkRegistrationInfo nri =
                    serviceState.getNetworkRegistrationInfo(DOMAIN_PS, TRANSPORT_TYPE_WWAN);
            if (nri != null) {
                nriRoaming = nri.isNetworkRoaming();
            } else {
                Log.d(LOG_TAG, "isRoaming: network registration info null");
            }
        } else {
            Log.d(LOG_TAG, "isRoaming: service state null");
        }
        return nriRoaming;
    }

// QTI_END: 2023-04-03: Telephony: Use the new API to check if the device is roaming
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static int getNonDefaultDataSub() {
        final int DDS = SubscriptionManager.getDefaultDataSubscriptionId();
        int nDDS = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int[] activeSubIdList = sSubscriptionManager.getActiveSubscriptionIdList();
        for (int i = 0; i < activeSubIdList.length; i++) {
            if (activeSubIdList[i] != DDS) {
                nDDS = activeSubIdList[i];
            }
        }
        return nDDS;
    }

// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    static boolean isOnLteNb1(int subId) {
        if (subId != mSubId) {
            Log.d(LOG_TAG, "isOnLteNb1 - passed in sub ID different from this instance's sub ID");
            return false;
        }
        if (sCurrentRadioIcon == null) {
            Log.d(LOG_TAG, "isOnLteNb1 - radio icon is null");
            return false;
        }
        return sCurrentRadioIcon.getType().get() == RadioIconType.TYPE_LTE_NB_IOT;
    }

    private BroadcastReceiver mBrocastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                redrawPreferenceControllers();
            }
        }
    };

    public MobileNetworkSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK;
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (super.onPreferenceTreeClick(preference)) {
            return true;
        }
        final String key = preference.getKey();

// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (mTelephonyManager == null) {
            return false;
        }
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (TextUtils.equals(key, BUTTON_CDMA_SYSTEM_SELECT_KEY)
                || TextUtils.equals(key, BUTTON_CDMA_SUBSCRIPTION_KEY)) {
            if (mTelephonyManager.getEmergencyCallbackMode()) {
                startActivityForResult(
                        new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null)
                                .setPackage(Utils.PHONE_PACKAGE_NAME),
                        REQUEST_CODE_EXIT_ECM);
                mClickedPrefKey = key;
            }
            return true;
        }

        return false;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (!Utils.isMobileDataCapable(context) && !Utils.isVoiceCapable(context)) {
            finish();
            return Arrays.asList();
        }
        if (getArguments() == null) {
            Intent intent = getIntent();
            if (intent != null) {
                mSubId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                        MobileNetworkUtils.getSearchableSubscriptionId(context));
                Log.d(LOG_TAG, "display subId from intent: " + mSubId);
            } else {
                Log.d(LOG_TAG, "intent is null, can not get subId " + mSubId + " from intent.");
            }
        } else {
            mSubId = SubIdBundleUtils.getSubId(
                    getArguments(),
                    Settings.EXTRA_SUB_ID,
                    MobileNetworkUtils.getSearchableSubscriptionId(context)
            );
            Log.d(LOG_TAG, "display subId from getArguments(): " + mSubId);
        }
// QTI_BEGIN: 2022-04-13: Telephony: Remove setScreenState Method.
        Log.i(LOG_TAG, "display subId: " + mSubId);
// QTI_END: 2022-04-13: Telephony: Remove setScreenState Method.

        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mExecutor.execute(() -> {
            mSubscriptionInfoEntity = mMobileNetworkRepository.getSubInfoById(
                    String.valueOf(mSubId));
            mMobileNetworkInfoEntity =
                    mMobileNetworkRepository.queryMobileNetworkInfoBySubId(
                            String.valueOf(mSubId));
        });

        MobileNetworkEidPreferenceController eid = new MobileNetworkEidPreferenceController(context,
                KEY_EID_KEY);
        eid.init(this, mSubId);

        return Arrays.asList(
                new DataUsageSummaryPreferenceController(context, mSubId),
// QTI_BEGIN: 2023-01-30: Telephony: Fix for "Data preference" issue
                new DataDefaultSubscriptionController(context, KEY_DATA_PREF,
                        getSettingsLifecycle(), this),
// QTI_END: 2023-01-30: Telephony: Fix for "Data preference" issue
                new CallsDefaultSubscriptionController(context, KEY_CALLS_PREF,
                        getSettingsLifecycle(), this),
                new SmsDefaultSubscriptionController(context, KEY_SMS_PREF, getSettingsLifecycle(),
                        this),
                new MobileDataPreferenceController(context, KEY_MOBILE_DATA_PREF,
                        getSettingsLifecycle(), this, mSubId),
                new ConvertToEsimPreferenceController(context, KEY_CONVERT_TO_ESIM_PREF,
                        getSettingsLifecycle(), this, mSubId), eid);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(LOG_TAG, "Invalid subId, get the default subscription to show.");
            SubscriptionInfo info = SubscriptionUtil.getSubscriptionOrDefault(context, mSubId);
            if (info == null) {
                Log.d(LOG_TAG, "Invalid subId request " + mSubId);
                return;
            }
            mSubId = info.getSubscriptionId();
            Log.d(LOG_TAG, "Show NetworkSettings fragment for subId" + mSubId);
        }

// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        sImsMgr = context.getSystemService(ImsManager.class);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets

        // Connect TelephonyUtils to ExtTelephonyService
        TelephonyUtils.connectExtTelephonyService(context);
// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets

        Intent intent = getIntent();
        if (intent != null) {
            int updateSubscriptionIndex = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            // If the user selects the 'Settings' action button from the 2G network protection
            // notification, the user will land on the screen, and the below code will dismiss the
            // notification.
            int notificationId = intent.getIntExtra(
                    NetworkChangeNotification.NETWORK_PROTECTION_2G_NOTIFICATION_ID_KEY, -1);
            if (notificationId != -1) {
                NotificationManager notificationManager =
                        context.getSystemService(NotificationManager.class);
                notificationManager.cancel(notificationId);
            }
            if (updateSubscriptionIndex != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                int oldSubId = mSubId;
                mSubId = updateSubscriptionIndex;
                // If the subscription has changed or the new intent does not contain the opt in
                // action,
                // remove the old discovery dialog. If the activity is being recreated, we will see
                // onCreate -> onNewIntent, so the dialog will first be recreated for the old
                // subscription
                // and then removed.
                if (updateSubscriptionIndex != oldSubId
                        || !MobileNetworkActivity.doesIntentContainOptInAction(intent)) {
                    removeContactDiscoveryDialog(oldSubId);
                }

                // evaluate showing the new discovery dialog if this intent contains an action to
                // show the
                // opt-in.
                if (MobileNetworkActivity.doesIntentContainOptInAction(intent)) {
                    showContactDiscoveryDialog();
                }
            }

        }

        if (!isCatalystEnabled()) {
            use(MobileNetworkSwitchController.class).init(mSubId);
        }
        use(CarrierSettingsVersionPreferenceController.class).init(mSubId);
        if (!isCatalystEnabled()) {
            use(BillingCyclePreferenceController.class).init(mSubId);
        }
        use(MmsMessagePreferenceController.class).init(mSubId);
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
        use(DataDuringCallsPreferenceController.class).init(mSubId);
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
        // CrossSimCallingViewModel is responsible for maintaining the correct cross sim calling
        // settings (backup calling).
        new ViewModelProvider(this).get(CrossSimCallingViewModel.class);
        use(AutoDataSwitchPreferenceController.class).init(mSubId);
        if (!isCatalystEnabled()) {
            use(DisabledSubscriptionController.class).init(mSubId);
        }
        use(DeleteSimProfilePreferenceController.class).init(mSubId);
        use(DisableSimFooterPreferenceController.class).init(mSubId);
        use(NrDisabledInDsdsFooterPreferenceController.class).init(mSubId);

        if (!isCatalystEnabled()) {
            use(MobileNetworkSpnPreferenceController.class).init(this, mSubId);
            use(MobileNetworkPhoneNumberPreferenceController.class).init(mSubId);
            use(MobileNetworkImeiPreferenceController.class).init(this, mSubId);
            use(ApnPreferenceController.class).init(mSubId);
        }

        final MobileDataPreferenceController mobileDataPreferenceController =
                use(MobileDataPreferenceController.class);
        if (mobileDataPreferenceController != null) {
            mobileDataPreferenceController.init(getFragmentManager(), mSubId,
                    mSubscriptionInfoEntity, mMobileNetworkInfoEntity);
            mobileDataPreferenceController.setWifiPickerTrackerHelper(
                    new WifiPickerTrackerHelper(getSettingsLifecycle(), context,
                            null /* WifiPickerTrackerCallback */));
        }

// QTI_BEGIN: 2024-09-19: Telephony: Use java implementations for Roaming preference
        final RoamingPreferenceController roamingPreferenceController =
                use(RoamingPreferenceController.class);
// QTI_END: 2024-09-19: Telephony: Use java implementations for Roaming preference
        if (roamingPreferenceController != null) {
// QTI_BEGIN: 2024-10-24: Telephony: Use kotlin implementation for roaming preference
            roamingPreferenceController.init(getFragmentManager(), mSubId);
// QTI_END: 2024-10-24: Telephony: Use kotlin implementation for roaming preference
        }

        final SatelliteSettingPreferenceController satelliteSettingPreferenceController = use(
                SatelliteSettingPreferenceController.class);
        if (satelliteSettingPreferenceController != null) {
            satelliteSettingPreferenceController.initialize(mSubId);
        }

        use(ApnPreferenceController.class).init(mSubId);
// QTI_BEGIN: 2020-04-17: Telephony: Redesign the user controlled PLMN feature
        use(UserPLMNPreferenceController.class).init(mSubId);
// QTI_END: 2020-04-17: Telephony: Redesign the user controlled PLMN feature
        use(CarrierPreferenceController.class).init(mSubId);
        if (!isCatalystEnabled()) {
            use(DataUsagePreferenceController.class).init(mSubId);
            use(EnabledNetworkModePreferenceController.class)
                    .init(mSubId, getParentFragmentManager());
        }
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        use(PreferredNetworkModePreferenceController.class).init(getLifecycle(), mSubId);
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        use(DataServiceSetupPreferenceController.class).init(mSubId);
        use(Enable2gPreferenceController.class).init(this, mSubId);
        use(CarrierWifiTogglePreferenceController.class).init(getLifecycle(), mSubId);

        final CallingPreferenceCategoryController callingPreferenceCategoryController =
                use(CallingPreferenceCategoryController.class);
        use(WifiCallingPreferenceController.class)
                .init(mSubId, callingPreferenceCategoryController);

        final OpenNetworkSelectPagePreferenceController openNetworkSelectPagePreferenceController =
                use(OpenNetworkSelectPagePreferenceController.class).init(mSubId);
        final AutoSelectPreferenceController autoSelectPreferenceController =
                use(AutoSelectPreferenceController.class)
                        .init(mSubId)
                        .addListener(openNetworkSelectPagePreferenceController);

// QTI_BEGIN: 2024-03-14: Telephony: CAG and SNPN feature
        final SelectNetworkPreferenceController selectNetworkPreferenceController =
                use(SelectNetworkPreferenceController.class)
                        .init(mSubId)
                        .addListener(autoSelectPreferenceController);
// QTI_END: 2024-03-14: Telephony: CAG and SNPN feature

        use(NetworkPreferenceCategoryController.class).init(mSubId)
                .setChildren(Arrays.asList(autoSelectPreferenceController));
        mCdmaSystemSelectPreferenceController = use(CdmaSystemSelectPreferenceController.class);
        mCdmaSystemSelectPreferenceController.init(getPreferenceManager(), mSubId);
        mCdmaSubscriptionPreferenceController = use(CdmaSubscriptionPreferenceController.class);
        mCdmaSubscriptionPreferenceController.init(getPreferenceManager(), mSubId);

        final VideoCallingPreferenceController videoCallingPreferenceController =
                use(VideoCallingPreferenceController.class)
                        .init(mSubId, callingPreferenceCategoryController);
// QTI_BEGIN: 2024-01-11: Telephony: Restore C_IWLAN UI
        final BackupCallingPreferenceController crossSimCallingPreferenceController =
                use(BackupCallingPreferenceController.class)
                        .init(getFragmentManager(), mSubId, callingPreferenceCategoryController);
        use(Enabled5GPreferenceController.class).init(mSubId);
// QTI_END: 2024-01-11: Telephony: Restore C_IWLAN UI
        use(Enhanced4gLtePreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(Enhanced4gAdvancedCallingPreferenceController.class).init(mSubId)
                .addListener(videoCallingPreferenceController);
        use(ContactDiscoveryPreferenceController.class).init(getParentFragmentManager(), mSubId);
        use(NrAdvancedCallingPreferenceController.class).init(mSubId);
        use(TransferEsimPreferenceController.class).init(mSubId, mSubscriptionInfoEntity);
        use(Smart5gPreferenceController.class).init(mSubId);
        final ConvertToEsimPreferenceController convertToEsimPreferenceController =
                use(ConvertToEsimPreferenceController.class);
        if (convertToEsimPreferenceController != null) {
            convertToEsimPreferenceController.init(mSubId, mSubscriptionInfoEntity);
        }

        List<AbstractPreferenceController> subscriptionPreferenceControllers =
                useGroup(AbstractSubscriptionPreferenceController.class);
        subscriptionPreferenceControllers.forEach(
                controller -> ((AbstractSubscriptionPreferenceController) controller).init(mSubId));
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate:+");

        final TelephonyStatusControlSession session =
                setTelephonyAvailabilityStatus(getPreferenceControllersAsList());

        super.onCreate(icicle);
        if (isUiRestricted()) {
            Log.d(LOG_TAG, "Mobile network page is disallowed.");
            finish();
            return;
        }
// QTI_BEGIN: 2024-05-10: Telephony: IMS: Fix NPE during SIM removal re-insert.
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.i(LOG_TAG, "onCreate: invalid subId. finish");
            session.close();
            finish();
            return;
        }
// QTI_END: 2024-05-10: Telephony: IMS: Fix NPE during SIM removal re-insert.
        final Context context = getContext();
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        sPackageName = this.getClass().getPackage().toString();
        sSubscriptionManager = context.getSystemService(SubscriptionManager.class);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
// QTI_BEGIN: 2024-03-25: Android_UI: Change ext telephony service connection location.
        sExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        sExtTelephonyManager.connectService(mExtTelServiceCallback);
        sInstanceCounter++;
// QTI_END: 2024-03-25: Android_UI: Change ext telephony service connection location.

        session.close();

        onRestoreInstance(icicle);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        new SubscriptionRepository(requireContext())
                .collectSubscriptionVisible(mSubId, viewLifecycleOwner, (isVisible) -> {
                    if (!isVisible) {
                        Log.d(LOG_TAG, "Due to subscription not visible, closes page");
                        finishFragment();
                    }
                    return Unit.INSTANCE;
                });
        new AirplaneModeRepository(requireContext()).collectAirplaneModeChanged(viewLifecycleOwner,
                (isAirplaneModeOn) -> {
                    notifyAirplaneModeForPreferences(isAirplaneModeOn);
                    return Unit.INSTANCE;
                });
    }

// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    @Override
    public void onResume() {
        Log.i(LOG_TAG, "onResume:+");
        super.onResume();
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        mMobileNetworkRepository.addRegister(this, this, mSubId);
        mMobileNetworkRepository.updateEntity();
        // TODO: remove log after fixing b/182326102
        Log.d(LOG_TAG, "onResume() subId=" + mSubId);
        redrawPreferenceControllers();
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
        getActivity().registerReceiver(mBroadcastReceiver,
                new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        getContext().registerReceiver(mBrocastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    }

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    private void onSubscriptionDetailChanged() {
        final SubscriptionInfoEntity subscriptionInfoEntity = mSubscriptionInfoEntity;
        if (subscriptionInfoEntity == null) {
            return;
        }
        ThreadUtils.postOnMainThread(() -> {
            if (getActivity() instanceof SettingsActivity activity && !activity.isFinishing()) {
                // Update the title when SIM stats got changed
                activity.setTitle(subscriptionInfoEntity.uniqueName);
            }
            redrawPreferenceControllers();
        });
    }

    @Override
    public void onPause() {
        mMobileNetworkRepository.removeRegister(this);
        getContext().unregisterReceiver(mBrocastReceiver);
        super.onPause();
// QTI_BEGIN: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
        getActivity().unregisterReceiver(mBroadcastReceiver);
// QTI_END: 2024-06-07: Telephony: Fix mobile network setting preferences display issue
    }

// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    @Override
    public void onDestroy() {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        sCurrentRadioIcon = null;
// QTI_BEGIN: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
        Log.i(LOG_TAG, "onDestroy: sExtTelServiceConnected = " + sExtTelServiceConnected
                + " , sInstanceCounter = " + sInstanceCounter);
        if (sInstanceCounter > 0) {
            sInstanceCounter--;
        }
        if ((sInstanceCounter == 0) && (sExtTelephonyManager != null) && sExtTelServiceConnected) {
            Log.i(LOG_TAG, "onDestroy");
// QTI_END: 2024-02-29: Telephony: Fix for Settings app crash when toggle Mobile Data
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
            sExtTelephonyManager.disconnectService(mExtTelServiceCallback);
            sExtTelephonyManager = null;
        }
        super.onDestroy();
    }

// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    @VisibleForTesting
    void onRestoreInstance(Bundle icicle) {
        if (icicle != null) {
            mClickedPrefKey = icicle.getString(KEY_CLICKED_PREF);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.mobile_network_settings;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CLICKED_PREF, mClickedPrefKey);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_EXIT_ECM:
                if (resultCode != Activity.RESULT_CANCELED) {
                    // If the phone exits from ECM mode, show the CDMA
                    final Preference preference = getPreferenceScreen()
                            .findPreference(mClickedPrefKey);
                    if (preference != null) {
                        preference.performClick();
                    }
                }
                break;

            case REQUEST_CODE_DELETE_SUBSCRIPTION:
                if (resultCode != Activity.RESULT_CANCELED) {
                    final Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        activity.finish();
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            final MenuItem item = menu.add(Menu.NONE, R.id.edit_sim_name, Menu.NONE,
                    R.string.mobile_network_sim_label_color_title);
            item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (menuItem.getItemId() == R.id.edit_sim_name) {
                RenameMobileNetworkDialogFragment.newInstance(mSubId).show(
                        getFragmentManager(), RenameMobileNetworkDialogFragment.TAG);
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mobile_network_settings) {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    return super.getXmlResourcesToIndex(context, enabled);
                }
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return MobileNetworkSettingsSearchIndex
                            .isMobileNetworkSettingsSearchable(context);
                }
            };

    private ContactDiscoveryDialogFragment getContactDiscoveryFragment(int subId) {
        // In the case that we are rebuilding this activity after it has been destroyed and
        // recreated, look up the dialog in the fragment manager.
        return (ContactDiscoveryDialogFragment) getChildFragmentManager()
                .findFragmentByTag(ContactDiscoveryDialogFragment.getFragmentTag(subId));
    }


    private void removeContactDiscoveryDialog(int subId) {
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(subId);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showContactDiscoveryDialog() {
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(mSubId);

        if (mSubscriptionInfoEntity == null) {
            Log.d(LOG_TAG, "showContactDiscoveryDialog, Invalid subId request " + mSubId);
            onDestroy();
            return;
        }

        if (fragment == null) {
            fragment = ContactDiscoveryDialogFragment.newInstance(mSubId,
                    mSubscriptionInfoEntity.uniqueName);
        }
        // Only try to show the dialog if it has not already been added, otherwise we may
        // accidentally add it multiple times, causing multiple dialogs.
        if (!fragment.isAdded()) {
            fragment.show(getChildFragmentManager(),
                    ContactDiscoveryDialogFragment.getFragmentTag(mSubId));
        }
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        mSubInfoEntityList = subInfoEntityList;
        SubscriptionInfoEntity[] entityArray = mSubInfoEntityList.toArray(
                new SubscriptionInfoEntity[0]);
        mSubscriptionInfoEntity = null;
        for (SubscriptionInfoEntity entity : entityArray) {
            int subId = Integer.parseInt(entity.subId);
            if (subId == mSubId) {
                mSubscriptionInfoEntity = entity;
                Log.d(LOG_TAG, "Set subInfo for subId " + mSubId);
                break;
            } else if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    && entity.isDefaultSubscriptionSelection) {
                mSubscriptionInfoEntity = entity;
                Log.d(LOG_TAG, "Set subInfo to default subInfo.");
            }
        }
        onSubscriptionDetailChanged();
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return MobileNetworkScreen.KEY;
    }

    @Deprecated(since = "This method will be removed once the catalyst framework stops passing the"
            + " arguments as a bundle. Use getPreferenceScreenBindingKeyParameters instead.")
    @Override
    public @Nullable Bundle getPreferenceScreenBindingArgs(@NonNull Context context) {
        final Bundle bundle = new Bundle();
        SubIdBundleUtils.putSubId(bundle, Settings.EXTRA_SUB_ID, getSubId());
        return bundle;
    }

    @Override
    @Nullable
    public ValidatedKeyParameters getPreferenceScreenBindingKeyParameters(
            @NonNull Context context
    ) {
        return MobileNetworkScreen.Companion.getParametersSchema().prepare(
            Map.of(
                Settings.EXTRA_SUB_ID,
                String.valueOf(getSubId())
            )
        );
    }

    @VisibleForTesting
    void notifyAirplaneModeForPreferences(boolean isAirplaneModeOn) {
        // notify preferences' airplaneModeCallback
        List<AbstractPreferenceController> allPreferencesList =
                getPreferenceControllersAsList();
        Log.d(LOG_TAG, "notifyAirplaneModeForPreferences");

        for (AbstractPreferenceController subPreference : allPreferencesList) {
            if (subPreference instanceof AirplaneModeChangedCallback) {
                ((AirplaneModeChangedCallback) subPreference)
                        .notifyAirplaneModeChanged(isAirplaneModeOn);
            }
        }
        updatePreferenceStates();
    }

    private int getSubId() {
        int retSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (getArguments() == null) {
            Intent intent = getIntent();
            if (intent != null) {
                retSubId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                        MobileNetworkUtils.getSearchableSubscriptionId(getContext()));
            } else {
                Log.d(LOG_TAG, "getSubId: intent is null, can not get subId " + retSubId
                        + " from intent.");
            }
        } else {
            retSubId = SubIdBundleUtils.getSubId(
                    getArguments(),
                    Settings.EXTRA_SUB_ID,
                    MobileNetworkUtils.getSearchableSubscriptionId(getContext())
            );
        }
        if (retSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(LOG_TAG, "getSubId: Invalid subId, get the default subscription to show.");
            SubscriptionInfo info = SubscriptionUtil.getSubscriptionOrDefault(getContext(),
                    retSubId);
            if (info == null) {
                Log.d(LOG_TAG, "getSubId: Invalid subId request " + retSubId);
            } else {
                retSubId = info.getSubscriptionId();
            }
        }
        Log.d(LOG_TAG, "getSubId: Result subId : " + retSubId);
        return retSubId;
    }
}
// LINT.ThenChange(MobileNetworkScreen.kt)
