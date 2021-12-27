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

package com.android.settings.network.apn;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import com.android.ims.ImsManager;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handle each different apn setting. */
public class ApnSettings extends RestrictedSettingsFragment
        implements Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
            "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
            "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";

    private static final String[] CARRIERS_PROJECTION = new String[] {
            Telephony.Carriers._ID,
            Telephony.Carriers.NAME,
            Telephony.Carriers.APN,
            Telephony.Carriers.TYPE,
            Telephony.Carriers.MVNO_TYPE,
            Telephony.Carriers.MVNO_MATCH_DATA,
            Telephony.Carriers.EDITED_STATUS,
            Telephony.Carriers.BEARER,
            Telephony.Carriers.BEARER_BITMASK,
    };

    /** Copied from {@code com.android.internal.telephony.TelephonyIntents} */
    private static final String ACTION_SIM_STATE_CHANGED =
            "android.intent.action.SIM_STATE_CHANGED";
    /** Copied from {@code com.android.internal.telephony.IccCardConstants} */
    public static final String INTENT_KEY_ICC_STATE = "ss";
    public static final String INTENT_VALUE_ICC_ABSENT = "ABSENT";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    private static final int EDITED_INDEX = 6;
    private static final int BEARER_INDEX = 7;
    private static final int BEARER_BITMASK_INDEX = 8;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private TelephonyManager mTelephonyManager;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private int mSubId;
    private int mPhoneId;
    private String mMvnoType;
    private String mMvnoMatchData;

    private String mSelectedKey;

    private IntentFilter mIntentFilter;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    private boolean mHidePresetApnDetails;

    private String[] mHideApnsWithRule;
    private String[] mHideApnsWithIccidRule;
    private PersistableBundle mHideApnsGroupByIccid;
    private final static String INCLUDE_COMMON_RULES = "include_common_rules";
    private final static String APN_HIDE_RULE_STRINGS_ARRAY= "apn_hide_rule_strings_array";
    private final static String APN_HIDE_RULE_STRINGS_WITH_ICCIDS_ARRAY = "apn_hide_rule_strings_with_iccids_array";

    private final static String ACTION_VOLTE_ENABLED_STATE_CHANGED
            = "org.codeaurora.intent.action.ACTION_ENHANCE_4G_SWITCH";

    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onPreciseDataConnectionStateChanged(
                PreciseDataConnectionState dataConnectionState) {
            if (dataConnectionState.getState() == TelephonyManager.DATA_CONNECTED) {
                if (!mRestoreDefaultApnMode) {
                    fillList();
                } else {
                    showDialog(DIALOG_RESTORE_DEFAULTAPN);
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_SIM_STATE_CHANGED.equals(action)
                    && intent.getStringExtra(INTENT_KEY_ICC_STATE)
                    .equals(INTENT_VALUE_ICC_ABSENT)) {
                final SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
                if (sm != null && !sm.isActiveSubscriptionId(mSubId)) {
                    Log.d(TAG, "Due to SIM absent, closes APN settings page");
                    finish();
                }
            } else if (intent.getAction().equals(
                    TelephonyManager.ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED)) {
                if (mRestoreDefaultApnMode) {
                    return;
                }
                final int extraSubId = intent.getIntExtra(TelephonyManager.EXTRA_SUBSCRIPTION_ID,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                if (SubscriptionManager.isValidSubscriptionId(extraSubId)
                        && mPhoneId == SubscriptionUtil.getPhoneId(context, extraSubId)
                        && extraSubId != mSubId) {
                    // subscription has changed
                    mSubId = extraSubId;
                    mSubscriptionInfo = getSubscriptionInfo(mSubId);
                    restartPhoneStateListener(mSubId);
                }
                fillList();
            } else if (intent.getAction().equals(ACTION_VOLTE_ENABLED_STATE_CHANGED)) {
                if (!mRestoreDefaultApnMode) {
                    fillList();
                } else {
                    showDialog(DIALOG_RESTORE_DEFAULTAPN);
                }
            }
        }
    };

    private void restartPhoneStateListener(int subId) {
        if (mRestoreDefaultApnMode) {
            return;
        }

        final TelephonyManager updatedTelephonyManager =
                mTelephonyManager.createForSubscriptionId(subId);

        // restart monitoring when subscription has been changed
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);

        mTelephonyManager = updatedTelephonyManager;

        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_PRECISE_DATA_CONNECTION_STATE);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        mSubId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mPhoneId = SubscriptionUtil.getPhoneId(activity, mSubId);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyManager.ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED);
        mIntentFilter.addAction(ACTION_SIM_STATE_CHANGED);
        if (Utils.isSupportCTPA(getActivity().getApplicationContext())) {
            mIntentFilter.addAction(ACTION_VOLTE_ENABLED_STATE_CHANGED);
        }

        setIfOnlyAvailableForAdmins(true);

        mSubscriptionInfo = getSubscriptionInfo(mSubId);
        mTelephonyManager = activity.getSystemService(TelephonyManager.class);

        final CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        final PersistableBundle b = configManager.getConfigForSubId(mSubId);
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);

        mHideApnsWithRule = b.getStringArray(APN_HIDE_RULE_STRINGS_ARRAY);
        mHideApnsWithIccidRule = b.getStringArray(APN_HIDE_RULE_STRINGS_WITH_ICCIDS_ARRAY);
        if(mSubscriptionInfo != null){
           String iccid = mSubscriptionInfo.getIccId();
           Log.d(TAG, "iccid: " + iccid);
           mHideApnsGroupByIccid = b.getPersistableBundle(iccid);
        }
        if (mAllowAddingApns) {
            final String[] readOnlyApnTypes = b.getStringArray(
                    CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
            // if no apn type can be edited, do not allow adding APNs
            if (ApnEditor.hasAllApns(readOnlyApnTypes)) {
                Log.d(TAG, "not allowing adding APN because all APN types are read only");
                mAllowAddingApns = false;
            }
        }
        mHidePresetApnDetails = b.getBoolean(CarrierConfigManager.KEY_HIDE_PRESET_APN_DETAILS_BOOL);
        mUserManager = UserManager.get(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
        if (mUnavailable) {
            addPreferencesFromResource(R.xml.placeholder_prefs);
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        getActivity().registerReceiver(mReceiver, mIntentFilter);

        restartPhoneStateListener(mSubId);

        if (!mRestoreDefaultApnMode) {
            fillList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mReceiver);

        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
    }

    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getUserHandle());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    private SubscriptionInfo getSubscriptionInfo(int subId) {
        return SubscriptionManager.from(getActivity()).getActiveSubscriptionInfo(subId);
    }

    private void fillList() {
        final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        final Uri simApnUri = Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI,
                String.valueOf(subId));
        final StringBuilder where =
                new StringBuilder("NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND "
                + "user_visible!=0");

        int phoneId = SubscriptionManager.getPhoneId(subId);
        Context appContext = getActivity().getApplicationContext();
        boolean isVoLTEEnabled = ImsManager.getInstance(appContext, phoneId)
                .isEnhanced4gLteModeSettingEnabledByUser();
        if (mHideImsApn || (Utils.isSupportCTPA(appContext) && !isVoLTEEnabled)) {
            where.append(" AND NOT (type='ims')");
        }

        appendFilter(where);

        Log.d(TAG, "where = " + where.toString());

        final Cursor cursor = getContentResolver().query(simApnUri,
                CARRIERS_PROJECTION, where.toString(), null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            final PreferenceGroup apnPrefList = (PreferenceGroup) findPreference("apn_list");
            apnPrefList.removeAll();

            final ArrayList<ApnPreference> apnList = new ArrayList<ApnPreference>();
            final ArrayList<ApnPreference> mmsApnList = new ArrayList<ApnPreference>();

            mSelectedKey = getSelectedApnKey();

            // ApnPreference.mSelectedKey static variable is shared for MSim case,
            // need be initialized according to preferred apn id per sub
            ApnPreference.setSelectedKey(mSelectedKey);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                final String apn = cursor.getString(APN_INDEX);
                final String key = cursor.getString(ID_INDEX);
                final String type = cursor.getString(TYPES_INDEX);
                final int edited = cursor.getInt(EDITED_INDEX);
                mMvnoType = cursor.getString(MVNO_TYPE_INDEX);
                mMvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);

                //Special requirement of some operators, need change APN name follow language.
                String localizedName = Utils.getLocalizedName(getActivity(), cursor.getString(NAME_INDEX));

                if (!TextUtils.isEmpty(localizedName)) {
                    name = localizedName;
                }
                int bearer = cursor.getInt(BEARER_INDEX);
                int bearerBitMask = cursor.getInt(BEARER_BITMASK_INDEX);
                int fullBearer = ServiceState.getBitmaskForTech(bearer) | bearerBitMask;
                int radioTech = networkTypeToRilRidioTechnology(TelephonyManager.getDefault()
                        .getDataNetworkType(subId));
                if (!ServiceState.bitmaskHasTech(fullBearer, radioTech)
                        && (bearer != 0 || bearerBitMask != 0)) {
                    // In OOS, show APN with bearer as default
                    if ((radioTech != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) || (bearer == 0
                            && radioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                        cursor.moveToNext();
                        continue;
                    }
                }
                final ApnPreference pref = new ApnPreference(getPrefContext());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                pref.setSubId(subId);
                if (mHidePresetApnDetails && edited == Telephony.Carriers.UNEDITED) {
                    pref.setHideDetails();
                } else {
                    pref.setSummary(apn);
                }

                boolean selectable =
                        ((type == null) || type.contains(ApnSetting.TYPE_DEFAULT_STRING));
                if (isVoLTEEnabled && selectable && Utils.isSupportCTPA(appContext)) {
                    selectable = ((type == null) || !type.equals("ims"));
                }
                pref.setSelectable(selectable);
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
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
            final String iccid = mSubscriptionInfo == null ? "" : mSubscriptionInfo.getIccId();
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
            case TelephonyManager.NETWORK_TYPE_NR:
                return ServiceState.RIL_RADIO_TECHNOLOGY_NR;
            default:
                return ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        }
    }

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
        final Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        final ContentResolver resolver = getContentResolver();

        final ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(getUriForCurrSubId(PREFERAPN_URI), values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        final Cursor cursor = getContentResolver().query(getUriForCurrSubId(PREFERAPN_URI),
                new String[] {"_id"}, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null || mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    // Append subId to the Uri
    private Uri getUriForCurrSubId(Uri uri) {
        final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            return Uri.withAppendedPath(uri, "subId/" + String.valueOf(subId));
        } else {
            return uri;
        }
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
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
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    final ContentResolver resolver = getContentResolver();
                    resolver.delete(getUriForCurrSubId(DEFAULTAPN_URI), null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
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
}
