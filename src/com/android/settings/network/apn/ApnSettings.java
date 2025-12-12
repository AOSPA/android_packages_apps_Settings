/*
 * Copyright (C) 2008 The Android Open Source Project
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

// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
package com.android.settings.network.apn;

import static com.android.settings.network.apn.ApnEditPageProviderKt.INSERT_URL;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
import android.content.BroadcastReceiver;
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
import android.content.Context;
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
import android.content.Intent;
import android.content.IntentFilter;
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
import android.content.res.Resources.NotFoundException;
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
import android.telephony.ServiceState;
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
import android.telephony.SubscriptionInfo;
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
import com.android.ims.ImsManager;
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.network.telephony.SubscriptionRepository;
import com.android.settings.spa.SpaActivity;
import com.android.settings.utils.SubIdBundleUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settings.Utils;
import com.android.settingslib.metadata.ValidatedKeyParameters;

import kotlin.Unit;

import java.util.ArrayList;
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display


/** Handle each different apn setting. */
// LINT.IfChange
public class ApnSettings extends RestrictedDashboardFragment
        implements Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String APN_ID = "apn_id";
    public static final String APN_LIST = "apn_list";
    public static final String SUB_ID = "sub_id";

    private static final String[] CARRIERS_PROJECTION = new String[] {
            Telephony.Carriers._ID,
            Telephony.Carriers.NAME,
            Telephony.Carriers.APN,
            Telephony.Carriers.TYPE,
            Telephony.Carriers.EDITED_STATUS,
            Telephony.Carriers.BEARER,
            Telephony.Carriers.BEARER_BITMASK,
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int EDITED_INDEX = 4;
// QTI_BEGIN: 2024-12-05: Android_UI: Settings: Fix IllegalStateException when access bearer
    private static final int BEARER_INDEX = 5;
    private static final int BEARER_BITMASK_INDEX = 6;
// QTI_END: 2024-12-05: Android_UI: Settings: Fix IllegalStateException when access bearer

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private int mSubId;
    private PreferredApnRepository mPreferredApnRepository;
    @Nullable
    private String mPreferredApnKey;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    private boolean mHidePresetApnDetails;

// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
    private String[] mHideApnsWithRule;
    private String[] mHideApnsWithIccidRule;
    private PersistableBundle mHideApnsGroupByIccid;
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
    private SubscriptionManager mSubscriptionManager;
    private IntentFilter mIntentFilter;
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
    private final static String INCLUDE_COMMON_RULES = "include_common_rules";
    private final static String APN_HIDE_RULE_STRINGS_ARRAY= "apn_hide_rule_strings_array";
    private final static String APN_HIDE_RULE_STRINGS_WITH_ICCIDS_ARRAY = "apn_hide_rule_strings_with_iccids_array";

// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)) {
                final int subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                if (!SubscriptionManager.isUsableSubscriptionId(subId) || subId == mSubId) {
                    loadCarrierConfig();
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    }
                }
            }
        }
    };

// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
    private void loadCarrierConfig() {
// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        final CarrierConfigManager configManager = getSystemService(CarrierConfigManager.class);
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
        final PersistableBundle b = configManager.getConfigForSubId(mSubId);
        if (b == null) {
            return;
        }
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
        mHideApnsWithRule = b.getStringArray(APN_HIDE_RULE_STRINGS_ARRAY);
        mHideApnsWithIccidRule = b.getStringArray(APN_HIDE_RULE_STRINGS_WITH_ICCIDS_ARRAY);
        if (mAllowAddingApns) {
// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue
            final List<String> readOnlyApnTypes = ApnTypes.getReadOnlyApnTypes(b);
// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
            // if no apn type can be edited, do not allow adding APNs
// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue
            if (ApnTypes.hasAllApnTypes(readOnlyApnTypes)) {
// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
                Log.d(TAG, "not allowing adding APN because all APN types are read only");
                mAllowAddingApns = false;
            }
        }
        mHidePresetApnDetails = b.getBoolean(CarrierConfigManager.KEY_HIDE_PRESET_APN_DETAILS_BOOL);
// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        mHideApnsGroupByIccid = b.getPersistableBundle(getIccid());
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
   }

// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue
    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        mSubId = activity.getIntent().getIntExtra(SUB_ID, getSubIdFromBindingArgs());
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        mIntentFilter = new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        mPreferredApnRepository = new PreferredApnRepository(activity, mSubId);
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        mSubscriptionManager =  getSystemService(SubscriptionManager.class);
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge

        setIfOnlyAvailableForAdmins(true);

// QTI_BEGIN: 2024-03-20: Telephony: Fix APN preference updating issue
        loadCarrierConfig();
// QTI_END: 2024-03-20: Telephony: Fix APN preference updating issue

        mUserManager = UserManager.get(activity);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.apn_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(com.android.settingslib.R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        new SubscriptionRepository(requireContext())
                .collectSubscriptionEnabled(mSubId, viewLifecycleOwner, (isEnabled) -> {
                    if (!isEnabled) {
                        Log.d(TAG, "Due to subscription not enabled, closes APN settings page");
                        finish();
                    }
                    return Unit.INSTANCE;
                });

        mPreferredApnRepository.collectPreferredApn(viewLifecycleOwner, (preferredApn) -> {
            mPreferredApnKey = preferredApn;
            final PreferenceGroup apnPreferenceList = getPreferenceScreen();
            for (int i = 0; i < apnPreferenceList.getPreferenceCount(); i++) {
                ApnPreference apnPreference = (ApnPreference) apnPreferenceList.getPreference(i);
                apnPreference.setIsChecked(apnPreference.getKey().equals(preferredApn));
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        getActivity().registerReceiver(mReceiver, mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);

// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
        if (!mRestoreDefaultApnMode) {
            fillList();
        }
    }

// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mReceiver);
    }

// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getProcessUserId());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ApnSettingsScreen.KEY;
    }

    private int getSubIdFromBindingArgs() {
        if (com.android.settingslib.catalyst.flags.Flags.catalystUseKeyParameters()) {
            final ValidatedKeyParameters parameters =
                    getPreferenceScreenBindingKeyParameters(requireContext());
            if (parameters == null) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }

            try {
                return Integer.parseInt(parameters.get(SUB_ID));
            } catch (NumberFormatException e) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
        } else {
            final Bundle args = getPreferenceScreenBindingArgs(requireContext());
            if (args == null) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }

            return SubIdBundleUtils.getSubId(
                    args,
                    SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
            );
        }
    }

    private void fillList() {
        final Uri simApnUri = Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI,
                String.valueOf(mSubId));
        final StringBuilder where =
                new StringBuilder("NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND "
                + "user_visible!=0");
        // Remove Emergency type, users should not mess with that
        where.append(" AND NOT (type='emergency')");

// QTI_BEGIN: 2024-05-13: Telephony: Fix for APN list filter issue by radio tech
        int phoneId = SubscriptionManager.getPhoneId(mSubId);
// QTI_END: 2024-05-13: Telephony: Fix for APN list filter issue by radio tech
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
        Context appContext = getActivity().getApplicationContext();
        boolean isVoLTEEnabled = ImsManager.getInstance(appContext, phoneId)
                .isEnhanced4gLteModeSettingEnabledByUser();
        if (mHideImsApn || (Utils.isSupportCTPA(appContext) && !isVoLTEEnabled)) {
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
            where.append(" AND NOT (type='ims')");
        }

// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
        appendFilter(where);

        Log.d(TAG, "where = " + where.toString());

// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
        final Cursor cursor = getContentResolver().query(simApnUri,
                CARRIERS_PROJECTION, where.toString(), null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            final PreferenceGroup apnPrefList = getPreferenceScreen();
            apnPrefList.removeAll();

            final ArrayList<ApnPreference> apnList = new ArrayList<ApnPreference>();
            final ArrayList<ApnPreference> mmsApnList = new ArrayList<ApnPreference>();

            cursor.moveToFirst();
// QTI_BEGIN: 2024-05-13: Telephony: Fix for APN list filter issue by radio tech
            final int radioTech = networkTypeToRilRidioTechnology(TelephonyManager.getDefault()
                    .getDataNetworkType(mSubId));
// QTI_END: 2024-05-13: Telephony: Fix for APN list filter issue by radio tech
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                final String apn = cursor.getString(APN_INDEX);
                final String key = cursor.getString(ID_INDEX);
                final String type = cursor.getString(TYPES_INDEX);
                final int edited = cursor.getInt(EDITED_INDEX);

// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
                //Special requirement of some operators, need change APN name follow language.
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
                String localizedName = Utils.getLocalizedName(getActivity(), cursor.getString(NAME_INDEX));
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display

                if (!TextUtils.isEmpty(localizedName)) {
                    name = localizedName;
                }
                int bearer = cursor.getInt(BEARER_INDEX);
                int bearerBitMask = cursor.getInt(BEARER_BITMASK_INDEX);
                int fullBearer = ServiceState.getBitmaskForTech(bearer) | bearerBitMask;
                if (!ServiceState.bitmaskHasTech(fullBearer, radioTech)
                        && (bearer != 0 || bearerBitMask != 0)) {
                    // In OOS, show APN with bearer as default
                    if ((radioTech != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) || (bearer == 0
                            && radioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                        cursor.moveToNext();
                        continue;
                    }
                }
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
                final ApnPreference pref = new ApnPreference(getPrefContext());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                pref.setSubId(mSubId);
                if (mHidePresetApnDetails && edited == Telephony.Carriers.UNEDITED) {
                    pref.setHideDetails();
                } else {
                    pref.setSummary(apn);
                }

                boolean defaultSelectable =
                        ((type == null) || type.contains(ApnSetting.TYPE_DEFAULT_STRING));
                if (isVoLTEEnabled && defaultSelectable && Utils.isSupportCTPA(appContext)) {
                    defaultSelectable = ((type == null) || !type.equals("ims"));
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
                }
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
                pref.setDefaultSelectable(defaultSelectable);
                if (defaultSelectable) {
                    pref.setIsChecked(key.equals(mPreferredApnKey));
                    apnList.add(pref);
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }
            cursor.close();

            for (Preference preference : apnList) {
                apnPrefList.addPreference(preference);
            }
            for (Preference preference : mmsApnList) {
                apnPrefList.addPreference(preference);
            }
        }
    }

// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
    private void appendFilter(StringBuilder where){
        boolean includeCommon = true;
        if(mHideApnsGroupByIccid != null && !mHideApnsGroupByIccid.isEmpty()){
           // APN hidden rules according to the specified iccid,
           // it should be configured in CarrierConfig as below.
           // <map name="12345">
           //    <string name="type">fota</string>
           //    <boolean name="include_common_rules" value="true"/>
           // </map>
           includeCommon = mHideApnsGroupByIccid.getBoolean(INCLUDE_COMMON_RULES, true);
           Log.d(TAG, "apn hidden rules specified iccid, include common rule: " + includeCommon);
           Set<String> keys = mHideApnsGroupByIccid.keySet();
           for(String key : keys){
              if(Utils.carrierTableFieldValidate(key)){
                 String value = mHideApnsGroupByIccid.getString(key);
                 if(value != null){
                    where.append(" AND " + key + " <> \"" + value + "\"");
                 }
              }
           }
        }

        // Some operator have special APN hidden rules group by iccids,
        // it should be configured in CarrierConfig as below,
        // it maybe overwrite some rules defined in common rules.
        // <string-array name="apn_hide_rule_strings_with_iccids_array" num="6">
        //    <item value="iccid"/>
        //    <item value="1111,2222"/>
        //    <item value="type"/>
        //    <item value="ims,emergency"/>
        //    <item value="include_common_rules"/>
        //    <item value="true"/>
        // </string-array>
        if(mHideApnsWithIccidRule != null){
            HashMap<String, String> ruleWithIccid = getApnRuleMap(mHideApnsWithIccidRule);
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
            final String iccid = getIccid();
// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
            if(isOperatorIccid(ruleWithIccid, iccid)){
                String s = ruleWithIccid.get(INCLUDE_COMMON_RULES);
                includeCommon = !(s != null && s.equalsIgnoreCase(String.valueOf(false)));
                Log.d(TAG, "apn hidden rules in iccids, include common rule: " + includeCommon);
                filterWithKey(ruleWithIccid, where);
            }
        }

        if(includeCommon){
            // Common APN hidden rules,
            // it should be configured in CarrierConfig as below.
            // <string-array name="apn_default_values_strings_array" num="2">
            //    <item value="type"/>
            //    <item value="fota"/>
            // </string-array>
            if(mHideApnsWithRule != null){
               HashMap<String, String> rule = getApnRuleMap(mHideApnsWithRule);
               filterWithKey(rule, where);
            }
        }
    }

// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
// QTI_BEGIN: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
    private String getIccid() {
        if (mSubscriptionManager == null) {
            Log.e(TAG, "mSubscriptionManager is null");
            return "";
        }
        final SubscriptionInfo subscriptionInfo
                = mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
        return subscriptionInfo == null ? "" : subscriptionInfo.getIccId();
    }

// QTI_END: 2024-12-18: Telephony: LKG: Fix for broken APN settings issue due to AP3A.240426.002 merge
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
    private void filterWithKey(Map<String, String> rules, StringBuilder where) {
        Set<String> fields = rules.keySet();
        for(String field : fields){
            if(Utils.carrierTableFieldValidate(field)){
                String value = rules.get(field);
                if(!TextUtils.isEmpty(value)){
                    String[] subValues = value.split(",");
                    for(String subValue : subValues){
                        where.append(" AND " + field + " <> \"" + subValue + "\"");
                    }
                }
            }
        }
    }

    private HashMap<String, String> getApnRuleMap(String[] ruleArray) {
        HashMap<String, String> rules = new HashMap<String, String>();
        if (ruleArray != null) {
            int length = ruleArray.length;
            Log.d(TAG, "ruleArray size = " + length);
            if (length > 0 && (length % 2 == 0)) {
                for (int i = 0; i < length;) {
                    rules.put(ruleArray[i].toLowerCase(), ruleArray[i + 1]);
                    i += 2;
                }
            }
        }
        return rules;
    }

    private boolean isOperatorIccid(HashMap<String, String> ruleMap, String iccid) {
        String valuesOfIccid = ruleMap.get("iccid");
        if (!TextUtils.isEmpty(valuesOfIccid)) {
            String[] iccids = valuesOfIccid.split(",");
            for (String subIccid : iccids) {
                if (iccid.startsWith(subIccid.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int networkTypeToRilRidioTechnology(int nt) {
        switch(nt) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return ServiceState.RIL_RADIO_TECHNOLOGY_GPRS;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return ServiceState.RIL_RADIO_TECHNOLOGY_UMTS;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSPA;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_IS95B;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return ServiceState.RIL_RADIO_TECHNOLOGY_LTE;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP;
            case TelephonyManager.NETWORK_TYPE_GSM:
                return ServiceState.RIL_RADIO_TECHNOLOGY_GSM;
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN;
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                return ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA;
// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
// QTI_BEGIN: 2021-11-10: Telephony: Add NR RAT to filter up APN entries
            case TelephonyManager.NETWORK_TYPE_NR:
                return ServiceState.RIL_RADIO_TECHNOLOGY_NR;
// QTI_END: 2021-11-10: Telephony: Add NR RAT to filter up APN entries
// QTI_BEGIN: 2018-02-01: Android_UI: Settings: UI customization for APN display
            default:
                return ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        }
    }

// QTI_END: 2018-02-01: Android_UI: Settings: UI customization for APN display
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            if (mAllowAddingApns) {
                menu.add(0, MENU_NEW, 0,
                        getResources().getString(R.string.menu_new))
                        .setIcon(R.drawable.ic_add_24dp)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW:
                addNewApn();
                return true;
            case MENU_RESTORE:
                restoreDefaultApn();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        String route = ApnEditPageProvider.INSTANCE.getRoute(
                INSERT_URL, Telephony.Carriers.CONTENT_URI, mSubId);
        SpaActivity.startSpaActivity(getContext(), route);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            mPreferredApnRepository.setPreferredApn((String) newValue);
        }

        return true;
    }

    private void restoreDefaultApn() {
// QTI_BEGIN: 2022-04-26: Android_UI: Settings: Fix restore default apn dialog sometimes not dismissed.
        showRestoreDefaultApnDialog();
// QTI_END: 2022-04-26: Android_UI: Settings: Fix restore default apn dialog sometimes not dismissed.
        mRestoreDefaultApnMode = true;

        mPreferredApnRepository.restorePreferredApn(getViewLifecycleOwner(), () -> {
            onPreferredApnRestored();
            return Unit.INSTANCE;
        });
    }

    private void onPreferredApnRestored() {
        final Activity activity = getActivity();
        if (activity == null) {
            mRestoreDefaultApnMode = false;
            return;
        }
        fillList();
        getPreferenceScreen().setEnabled(true);
        mRestoreDefaultApnMode = false;
        removeDialog(DIALOG_RESTORE_DEFAULTAPN);
        Toast.makeText(
                activity,
                getResources().getString(R.string.restore_default_apn_completed),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            final ProgressDialog dialog = new ProgressDialog(getActivity()) {
                public boolean onTouchEvent(MotionEvent event) {
                    return true;
                }
            };
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_RESTORE_DEFAULTAPN) {
            return SettingsEnums.DIALOG_APN_RESTORE_DEFAULT;
        }
        return 0;
    }
// QTI_BEGIN: 2022-04-26: Android_UI: Settings: Fix restore default apn dialog sometimes not dismissed.

    private void showRestoreDefaultApnDialog() {
        // try to remove the progress dialog firstly to avoid this dialog sometimes not dismissed.
        // such as repeatly and quickly clicking the restore default apn menu item before the
        // progress dialog UI is really shown.
        removeDialog(DIALOG_RESTORE_DEFAULTAPN);
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
    }
// QTI_END: 2022-04-26: Android_UI: Settings: Fix restore default apn dialog sometimes not dismissed.
}
// LINT.ThenChange(ApnSettingsScreen.kt)
