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

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;

/**
 * Preference controller for "Preferred network mode"
 */
public class PreferredNetworkModePreferenceController extends TelephonyBasePreferenceController
        implements ListPreference.OnPreferenceChangeListener, LifecycleObserver {

    private static final String LOG_TAG = "PreferredNetworkMode";
    private CarrierConfigCache mCarrierConfigCache;
    private TelephonyManager mTelephonyManager;
    private boolean mIsGlobalCdma;
    private Preference mPreference;
    private PhoneCallStateListener mPhoneStateListener;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    @VisibleForTesting
    Integer mCallState;

    public PreferredNetworkModePreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        boolean visible;
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            visible = false;
        } else if (carrierConfig == null) {
            visible = false;
        } else if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)) {
            visible = false;
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            visible = true;
        } else {
            visible = false;
        }

        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (mPhoneStateListener != null) {
            mPhoneStateListener.register(mContext, mSubId);
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }

    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (mPhoneStateListener != null) {
            mPhoneStateListener.unregister();
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        if (mTelephonyManager == null) {
            return;
        }
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        final int networkMode = getPreferredNetworkMode();
        listPreference.setValue(Integer.toString(networkMode));
        listPreference.setSummary(getPreferredNetworkModeSummaryResId(networkMode));
        listPreference.setEnabled(isCallStateIdle());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        final int newPreferredNetworkMode = Integer.parseInt((String) object);

        mTelephonyManager.setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                MobileNetworkUtils.getRafFromNetworkType(newPreferredNetworkMode));

        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setSummary(getPreferredNetworkModeSummaryResId(newPreferredNetworkMode));
        return true;
    }

    public void init(Lifecycle lifecycle, int subId) {
        mSubId = subId;
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneCallStateListener();
        }
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(mSubId);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);

        mIsGlobalCdma = mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);

        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> updatePreference());
        }

        lifecycle.addObserver(this);
    }

    private void updatePreference() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    private int getPreferredNetworkMode() {
        return MobileNetworkUtils.getNetworkTypeFromRaf(
                (int) mTelephonyManager.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
    }

    private int getPreferredNetworkModeSummaryResId(int NetworkMode) {
        switch (NetworkMode) {
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_tdscdma_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_tdscdma_gsm_summary;
            case TelephonyManagerConstants.NETWORK_MODE_WCDMA_PREF:
                return R.string.preferred_network_mode_wcdma_perf_summary;
            case TelephonyManagerConstants.NETWORK_MODE_GSM_ONLY:
                return R.string.preferred_network_mode_gsm_only_summary;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_tdscdma_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_WCDMA_ONLY:
                return R.string.preferred_network_mode_wcdma_only_summary;
            case TelephonyManagerConstants.NETWORK_MODE_GSM_UMTS:
                return R.string.preferred_network_mode_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO:
                return mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
                        ? R.string.preferred_network_mode_cdma_summary
                        : R.string.preferred_network_mode_cdma_evdo_summary;
            case TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO:
                return R.string.preferred_network_mode_cdma_only_summary;
            case TelephonyManagerConstants.NETWORK_MODE_EVDO_NO_CDMA:
                return R.string.preferred_network_mode_evdo_only_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY:
                return R.string.preferred_network_mode_lte_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_lte_tdscdma_gsm_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                return R.string.preferred_network_mode_lte_cdma_evdo_summary;
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_ONLY:
                return R.string.preferred_network_mode_tdscdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                        || mIsGlobalCdma
                        || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                    return R.string.preferred_network_mode_lte_cdma_evdo_gsm_wcdma_summary;
                } else {
                    return R.string.preferred_network_mode_lte_summary;
                }
            case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_tdscdma_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_GLOBAL:
                return R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA:
                return R.string.preferred_network_mode_lte_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_ONLY:
                return R.string.preferred_network_mode_nr_only_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE:
                return R.string.preferred_network_mode_nr_lte_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO:
                return R.string.preferred_network_mode_nr_lte_cdma_evdo_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_global_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA:
                return R.string.preferred_network_mode_nr_lte_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_nr_lte_tdscdma_gsm_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_gsm_wcdma_summary;
            case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_cdma_evdo_gsm_wcdma_summary;
            default:
                return R.string.preferred_network_mode_global_summary;
        }
    }

    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        if (mCallState != null && mCallState != TelephonyManager.CALL_STATE_IDLE) {
            callStateIdle = false;
        }
        Log.d(LOG_TAG, "isCallStateIdle:" + callStateIdle);
        return callStateIdle;
    }

    private class PhoneCallStateListener extends PhoneStateListener {

        PhoneCallStateListener() {
            super(Looper.getMainLooper());
        }

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);

        }

        public void unregister() {
            mCallState = null;
            mTelephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
        }
    }
}
