/*
 * Copyright (c) 2020-2021, The Linux Foundation. All rights reserved
 * Not a contribution

 * Copyright (C) 2019 The Android Open Source Project

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

package com.android.settings.development;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.network.ims.VolteQueryImsState;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreferVonrSettings extends SettingsPreferenceFragment implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "PreferVonrSettings";
    private static final int VONR_MODE_INVALID = -1;
    private static final int VONR_MODE_OFF = 0;
    private static final int VONR_MODE_ON = 1;
    private static final String VONR_MODE_KEY = "vonr_mode_";
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mTelephonyManager;
    private Map<Integer, RestrictedSwitchPreference> mPreferences;
    private Context mContext;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPreferences;
    private final List<RestrictedSwitchPreference> mPreferenceList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if(getPreferenceScreen() == null) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        }
        mPreferenceScreen = getPreferenceScreen();
        addPreferencesFromResource(R.xml.prefer_vonr_list);
        mContext = getPrefContext();
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mChangeListener = new SubscriptionsChangeListener(mContext, this);
        mPreferences = new ArrayMap<>();
        mSharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(),
                mContext.MODE_PRIVATE);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mChangeListener.start();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        super.onPause();
        mChangeListener.stop();
    }

    private void update() {
        Log.d(TAG, "update");
        // Since we may already have created some preferences previously, we first grab the list of
        // those, then go through the current available subscriptions making sure they are all
        // present in the screen, and finally remove any now-outdated ones.
        final Map<Integer, RestrictedSwitchPreference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();

        for (int slotId = 0; slotId < mTelephonyManager.getActiveModemCount();
                slotId++) {
            SubscriptionInfo info = mSubscriptionManager.
                getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (info != null) {
                final int subId = info.getSubscriptionId();
                RestrictedSwitchPreference pref = existingPreferences.remove(subId);
                if (pref == null) {
                    pref = new RestrictedSwitchPreference(mContext);
                    mPreferenceScreen.addPreference(pref);
                }
                pref.setTitle(info.getDisplayName());
                pref.setOrder(slotId);

                mPreferences.put(subId, pref);
                mPreferenceList.add(pref);
                pref.setChecked(isVoNrSwitchChecked(slotId));
                pref.setEnabled(isVoNrSwitchEnabled(subId, slotId));
                maybeChangeNrCapability(slotId);
                Log.d(TAG, "add preference for slot: " + slotId + " subId: " + subId);
            } else {
                Log.d(TAG, "sub info is null, add null preference for slot: " + slotId);
                mPreferenceList.add(slotId, null);
            }
        }
        for (Preference pref : existingPreferences.values()) {
            mPreferenceScreen.removePreference(pref);
        }
    }

    /**
     * @return {@code true} if VoIMS opt-in has been enabled
     * or VoLTE can be perform on this subscription,
     * or VoNR is not supported by platform,
     * {@code false} otherwise.
     */
    private boolean isVoNrSwitchEnabled(int subId, int slotId) {
        ImsManager imsMgr = ImsManager.getInstance(mContext, slotId);
        boolean isVoNrEnabledByCarrier = imsMgr.isImsOverNrEnabledByPlatform();
        VolteQueryImsState queryImsState = new VolteQueryImsState(mContext, subId);
        return (queryImsState.isVoImsOptInEnabled() || queryImsState.isReadyToVoLte())
            && !isVoNrEnabledByCarrier;
    }

    /**
     * @return {@code false} if VoLTE is off.
     * @return {@code true} if VoLTE is on and supporting SA in carrier config.
     * @return what user choose in this UI if VoLTE is on, but not supporting
     * SA in carrier config
     */
    private boolean isVoNrSwitchChecked(int slotId) {
        ImsManager imsMgr = ImsManager.getInstance(mContext, slotId);
        boolean isVolteEnabled = imsMgr.isEnhanced4gLteModeSettingEnabledByUser();
        if (!isVolteEnabled) {
            return false;
        }
        boolean isVoNrEnabledByCarrier = imsMgr.isImsOverNrEnabledByPlatform();
        int vonrMode = mSharedPreferences.getInt(VONR_MODE_KEY + slotId, VONR_MODE_INVALID);
        boolean isVoNrEnabledByUser = vonrMode == VONR_MODE_ON;
        Log.d(TAG, "enhanced 4g enabled: " + isVolteEnabled + ", vonr enabled by carrier: "
                + isVoNrEnabledByCarrier + ", vonr enabled by user: " + isVoNrEnabledByUser
                + " for slot: " + slotId);
        return isVoNrEnabledByCarrier ? isVolteEnabled : isVoNrEnabledByUser;
    }

    /**
     * This function is to make sure UI align with real capabilities.
     * isVolteEnabled: the user option of "Enhanced 4g", true if enabled.
     * isVoNrEnabledByCarrier: defined by carrier_nr_availabilities_int_array, true if
     * SA is supported.
     * isVoNrEnabledByUser: the user option of this UI, true if enabled.
     * For china operator's sub, need this UI to enable/disable VoNR.
     * For other subs, VoNR capability is been combined with VoLTE through "Enhanced 4g"
     * option. We will not change NR capability here for other subs.
    */
    private void maybeChangeNrCapability(int slotId) {
        ImsManager imsMgr = ImsManager.getInstance(mContext, slotId);
        boolean isVolteEnabled = imsMgr.isEnhanced4gLteModeSettingEnabledByUser();
        boolean isVoNrEnabledByCarrier = imsMgr.isImsOverNrEnabledByPlatform();
        int vonrMode = mSharedPreferences.getInt(VONR_MODE_KEY + slotId, VONR_MODE_INVALID);
        boolean isVoNrEnabledByUser = vonrMode == VONR_MODE_ON;
        // If VoLTE is disabled, need to make sure VoNR is also disabled.
        if (!isVolteEnabled && isVoNrEnabledByUser && !isVoNrEnabledByCarrier) {
            changeNrCapability(imsMgr, false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final int slotId = mPreferenceList.indexOf(preference);
        RestrictedSwitchPreference pref = (RestrictedSwitchPreference)preference;
        Log.d(TAG, "onPreferenceTreeClick, preference isChecked: " + pref.isChecked()
                + " for slot: " + slotId);

        ImsManager imsMgr = ImsManager.getInstance(mContext, slotId);
        boolean isImsEnabled = imsMgr.isEnhanced4gLteModeSettingEnabledByUser();
        if (!isImsEnabled) {
            Log.d(TAG, "onPreferenceTreeClick, ims is disabled, ignore the request");
            return false;
        }
        changeNrCapability(imsMgr, pref.isChecked());
        savePreferenceForSlot(slotId, pref.isChecked());
        return super.onPreferenceTreeClick(preference);
    }

    private void changeNrCapability(ImsManager imsMgr, boolean enabled) {
        try {
            imsMgr.changeMmTelCapability(enabled,
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_NR);
        } catch (ImsException e) {
            Log.e(TAG, "Failed to change vonr mode to " + enabled + " since " + e);
        }
    }

    private void savePreferenceForSlot(int slotId, boolean isChecked) {
        if (mSharedPreferences != null) {
            int vonr_mode = isChecked? VONR_MODE_ON: VONR_MODE_OFF;
            mSharedPreferences.edit().putInt("vonr_mode_" + slotId, vonr_mode).apply();
        }
    }

    @Override
    public void onSubscriptionsChanged() {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mContext);
        for (SubscriptionInfo info : subs) {
            if (info == null) return;

            final int subId = info.getSubscriptionId();
            if (mPreferences.get(subId) == null) {
                Log.d(TAG, "sub changed, will update preference");
                update();
                break;
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }
}
