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

// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
/*
// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
 * Changes from Qualcomm Technologies, Inc. are provided under the following license:
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
// QTI_BEGIN: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

// QTI_END: 2023-04-27: Telephony: Restore backward compatibility for C_IWLAN dialogs
package com.android.settings.network.telephony;

// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.NR;

import android.app.AlertDialog;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import static com.android.settings.network.telephony.EnabledNetworkModePreferenceControllerHelperKt.getNetworkModePreferenceType;

import android.content.Context;
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import android.content.DialogInterface;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import android.os.PersistableBundle;
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import android.os.RemoteException;
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import android.telephony.PhoneStateListener;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteModemStateCallback;
import android.telephony.satellite.SelectedNbIotSatelliteSubscriptionCallback;
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import android.util.Log;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock

import androidx.annotation.NonNull;
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import androidx.annotation.VisibleForTesting;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
import androidx.lifecycle.DefaultLifecycleObserver;
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import androidx.lifecycle.Lifecycle;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
import androidx.preference.PreferenceScreen;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock

import com.android.settings.R;
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
import com.android.settings.network.AllowedNetworkTypesListener;
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.telephony.mode.NetworkModes;

// QTI_BEGIN: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// QTI_END: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
/**
 * Preference controller for "Preferred network mode"
 */
public class PreferredNetworkModePreferenceController extends BasePreferenceController
        implements ListPreference.OnPreferenceChangeListener, DefaultLifecycleObserver,
        AirplaneModeChangedCallback {
    private static final String TAG = "PrefNetworkModeCtrl";

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private CarrierConfigCache mCarrierConfigCache;
    private TelephonyManager mTelephonyManager;
    private boolean mIsGlobalCdma;
    private SatelliteManager mSatelliteManager;
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    private Preference mPreference;
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    private boolean mIsSatelliteSessionStarted = false;
    private boolean mIsCurrentSubscriptionForSatellite = false;
    protected boolean mIsAirplaneModeOn = false;
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    private PhoneCallStateListener mPhoneStateListener;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    @VisibleForTesting
    Integer mCallState;
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.

    @VisibleForTesting
    final SelectedNbIotSatelliteSubscriptionCallback mSelectedNbIotSatelliteSubscriptionCallback =
            new SelectedNbIotSatelliteSubscriptionCallback() {
                @Override
                public void onSelectedNbIotSatelliteSubscriptionChanged(int selectedSubId) {
                    mIsCurrentSubscriptionForSatellite = selectedSubId == mSubId;
                    updateState(mPreference);
                }
            };

    @VisibleForTesting
    final SatelliteModemStateCallback mSatelliteModemStateCallback =
            new SatelliteModemStateCallback() {
                @Override
                public void onSatelliteModemStateChanged(int state) {
                    switch (state) {
                        case SatelliteManager.SATELLITE_MODEM_STATE_OFF:
                        case SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE:
                        case SatelliteManager.SATELLITE_MODEM_STATE_UNKNOWN:
                            if (mIsSatelliteSessionStarted) {
                                mIsSatelliteSessionStarted = false;
                                updateState(mPreference);
                            }
                            break;
                        default:
                            if (!mIsSatelliteSessionStarted) {
                                mIsSatelliteSessionStarted = true;
                                updateState(mPreference);
                            }
                            break;
                    }
                }
            };

    public PreferredNetworkModePreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        mSatelliteManager = context.getSystemService(SatelliteManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return getNetworkModePreferenceType(mContext, mSubId)
                == NetworkModePreferenceType.PreferredNetworkMode
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
// QTI_BEGIN: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
        // Remove entries containing CDMA and TDSCDMA choices if unsupported
        removeCdmaAndTdscdmaChoices();
// QTI_END: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    }

// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    @Override
    public void updateState(Preference preference) {
        if (mTelephonyManager == null) {
            return;
        }
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        preference.setEnabled(!(mIsCurrentSubscriptionForSatellite && mIsSatelliteSessionStarted)
                && !mIsAirplaneModeOn);
        final ListPreference listPreference = (ListPreference) preference;
        final int networkMode = getPreferredNetworkMode();
        listPreference.setValue(Integer.toString(networkMode));
        listPreference.setSummary(getPreferredNetworkModeSummaryResId(networkMode));
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        listPreference.setEnabled(isCallStateIdle());
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
// QTI_BEGIN: 2024-05-05: Telephony: Add null checks to avoid NPE
        if (mTelephonyManager == null) {
            return false;
        }
// QTI_END: 2024-05-05: Telephony: Add null checks to avoid NPE
        final int newPreferredNetworkMode = Integer.parseInt((String) object);
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        final int DDS = SubscriptionManager.getDefaultDataSubscriptionId();
        final int nDDS = MobileNetworkSettings.getNonDefaultDataSub();
        final boolean isDDS = mSubId == DDS;
        // Check UE's C_IWLAN configuration and user's current network mode selection. If C_IWLAN is
        // enabled, and the selection does not contain LTE or NR, show a dialog to disable C_IWLAN.
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        boolean isCiwlanIncompatibleNetworkSelected = isCiwlanIncompatibleNetworkSelected(
                newPreferredNetworkMode);
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        boolean isMsimCiwlanSupported = MobileNetworkSettings.isMsimCiwlanSupported();
        boolean currentSubCiwlanEnabled = MobileNetworkSettings.isCiwlanEnabled(mSubId);
        boolean otherSubCiwlanEnabled = isDDS ? MobileNetworkSettings.isCiwlanEnabled(nDDS) :
                MobileNetworkSettings.isCiwlanEnabled(DDS);
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        Log.d(TAG, "isDDS = " + isDDS +
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                ", currentSubCiwlanEnabled = " + currentSubCiwlanEnabled +
                ", otherSubCiwlanEnabled = " + otherSubCiwlanEnabled +
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
                ", isCiwlanIncompatibleNetworkSelected = " + isCiwlanIncompatibleNetworkSelected);
// QTI_END: 2023-04-03: Telephony: Fix warning dialog not showing for data toggle
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        if (isMsimCiwlanSupported) {
            if (isCiwlanIncompatibleNetworkSelected) {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
                if (isDDS) {
                    if (otherSubCiwlanEnabled && currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_dds_with_ciwlan_ui_on_both);
                        return false;
                    } else if (otherSubCiwlanEnabled && !currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_dds_with_ciwlan_ui_on_ndds);
                        return false;
                    } else if (!otherSubCiwlanEnabled && currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_dds_with_ciwlan_ui_on_dds);
                        return false;
                    } else {
                        // No warning
                    }
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                } else {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
                    if (otherSubCiwlanEnabled && currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_ndds_with_ciwlan_ui_on_both);
                    } else if (otherSubCiwlanEnabled && !currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_ndds_with_ciwlan_ui_on_dds);
                    } else if (!otherSubCiwlanEnabled && currentSubCiwlanEnabled) {
                        showCiwlanWarningDialog(
                                R.string.incompatible_pref_nw_for_ndds_with_ciwlan_ui_on_ndds);
                    } else {
                        // No warning
                    }
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                }
            }
        } else {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-04-18: Telephony: Fix C_IWLAN warning dialog showing for nDDS
            if (isDDS && currentSubCiwlanEnabled && isCiwlanIncompatibleNetworkSelected) {
// QTI_END: 2024-04-18: Telephony: Fix C_IWLAN warning dialog showing for nDDS
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                showCiwlanWarningDialog(
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
                        R.string.incompatible_pref_nw_for_dds_with_ciwlan_ui_on_dds);
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
                return false;
            }
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        }
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs

        mTelephonyManager.setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                RadioAccessFamily.getRafFromNetworkType(newPreferredNetworkMode));

        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setSummary(getPreferredNetworkModeSummaryResId(newPreferredNetworkMode));
        return true;
    }

// QTI_BEGIN: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
    private void removeCdmaAndTdscdmaChoices() {
        final ListPreference listPreference = (ListPreference) mPreference;
        final CharSequence[] entries = listPreference.getEntries();
        final CharSequence[] entryValues = listPreference.getEntryValues();
        final ArrayList<CharSequence> newEntries = new ArrayList<>();
        final ArrayList<CharSequence> newEntryValues = new ArrayList<>();
        final boolean cdmaSupported = MobileNetworkUtils.isCdmaSupported(mContext);
        final boolean tdscdmaSupported = MobileNetworkUtils.isTdscdmaSupported(mContext, mSubId);
        final Pattern pattern = Pattern.compile("(?<!W|TDS)CDMA|EvDo");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i].toString();
            Matcher matcher = pattern.matcher(entry);
            if (cdmaSupported || !matcher.find()) {
                newEntries.add(entries[i]);
                newEntryValues.add(entryValues[i]);
            }
            if (!tdscdmaSupported && entry.contains("TDSCDMA")) {
                newEntries.remove(entries[i]);
                newEntryValues.remove(entryValues[i]);
            }
        }
        listPreference.setEntries(newEntries.toArray(new CharSequence[0]));
        listPreference.setEntryValues(newEntryValues.toArray(new CharSequence[0]));
    }

// QTI_END: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    private boolean isCiwlanIncompatibleNetworkSelected(int networkMode) {
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        long raf = RadioAccessFamily.getRafFromNetworkType(networkMode);
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
        return (LTE & raf) == 0 && (NR & raf) == 0;
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
    private void showCiwlanWarningDialog(int dialogBodyTextId) {
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
// QTI_BEGIN: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
        builder.setTitle(R.string.incompatible_pref_nw_ciwlan_dialog_title)
// QTI_END: 2024-03-29: Telephony: Modify C_IWLAN warning behavior to be non-blocking
// QTI_BEGIN: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
               .setMessage(dialogBodyTextId)
// QTI_END: 2024-02-02: Telephony: Update C_IWLAN warning dialog showing criteria
// QTI_BEGIN: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
               .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                   }
               });
        builder.show();
    }

// QTI_END: 2023-02-13: Telephony: Show C_IWLAN-related warning dialogs
    @Override
    public void notifyAirplaneModeChanged(boolean isAirplaneModeOn) {
        this.mIsAirplaneModeOn = isAirplaneModeOn;
    }

// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    public void init(Lifecycle lifecycle, int subId) {
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        mSubId = subId;
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        if (mPhoneStateListener == null) {
            mPhoneStateListener = new PhoneCallStateListener();
        }
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(mSubId);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);

// QTI_BEGIN: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
        mIsGlobalCdma = MobileNetworkUtils.isCdmaSupported(mContext)
                && mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
// QTI_END: 2024-11-20: Telephony: Deprecate CDMA/TDSCDMA
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);

// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> updatePreference());
        }

// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
// QTI_BEGIN: 2020-01-26: Telephony: Add support for primary card and subsidy lock
        lifecycle.addObserver(this);
// QTI_END: 2020-01-26: Telephony: Add support for primary card and subsidy lock
    }

// QTI_BEGIN: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    private void updatePreference() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

// QTI_END: 2021-05-26: Telephony: Update 5G switch properly after set network mode
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mSatelliteManager != null) {
            try {
                mSatelliteManager.registerForModemStateChanged(
                        mContext.getMainExecutor(), mSatelliteModemStateCallback);
                mSatelliteManager.registerForSelectedNbIotSatelliteSubscriptionChanged(
                        mContext.getMainExecutor(),
                        mSelectedNbIotSatelliteSubscriptionCallback);
            } catch (IllegalStateException e) {
                Log.w(TAG, "IllegalStateException : " + e);
            }
        }
        if (mPhoneStateListener != null) {
            mPhoneStateListener.register(mContext, mSubId);
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mSatelliteManager != null) {
            try {
                mSatelliteManager.unregisterForModemStateChanged(mSatelliteModemStateCallback);
                mSatelliteManager.unregisterForSelectedNbIotSatelliteSubscriptionChanged(
                        mSelectedNbIotSatelliteSubscriptionCallback);
            } catch (IllegalStateException e) {
                Log.w(TAG, "IllegalStateException : " + e);
            }
        }
        if (mPhoneStateListener != null) {
            mPhoneStateListener.unregister();
        }
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    private int getPreferredNetworkMode() {
        if (mTelephonyManager == null) {
            Log.w(TAG, "TelephonyManager is null");
            return NetworkModes.NETWORK_MODE_UNKNOWN;
        }
        long allowedNetworkTypes = NetworkModes.NETWORK_MODE_UNKNOWN;
// QTI_BEGIN: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
        try {
            allowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                    TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
        } catch (Exception ex) {
// QTI_END: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
            Log.e(TAG, "getAllowedNetworkTypesForReason exception", ex);
// QTI_BEGIN: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
        }
// QTI_END: 2024-08-01: Telephony: Handle getAllowedNetworkTypesForReason exceptions
        return RadioAccessFamily.getNetworkTypeFromRaf((int) allowedNetworkTypes);
    }

    private int getPreferredNetworkModeSummaryResId(int networkMode) {
        switch (networkMode) {
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_tdscdma_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_tdscdma_gsm_summary;
            case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                return R.string.preferred_network_mode_gsm_only_summary;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_tdscdma_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_WCDMA_ONLY:
                return R.string.preferred_network_mode_wcdma_only_summary;
            case TelephonyManager.NETWORK_MODE_GSM_UMTS:
            case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                return R.string.preferred_network_mode_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                return mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
                        ? R.string.preferred_network_mode_cdma_summary
                        : R.string.preferred_network_mode_cdma_evdo_summary;
            case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                return R.string.preferred_network_mode_cdma_only_summary;
            case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
                return R.string.preferred_network_mode_evdo_only_summary;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_ONLY:
                return R.string.preferred_network_mode_lte_summary;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_lte_tdscdma_gsm_summary;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                return R.string.preferred_network_mode_lte_cdma_evdo_summary;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                return R.string.preferred_network_mode_tdscdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                        || mIsGlobalCdma
                        || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                    return R.string.preferred_network_mode_lte_cdma_evdo_gsm_wcdma_summary;
                } else {
                    return R.string.preferred_network_mode_lte_summary;
                }
            case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_tdscdma_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_GLOBAL:
                return R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_lte_tdscdma_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_LTE_WCDMA:
                return R.string.preferred_network_mode_lte_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_ONLY:
                return R.string.preferred_network_mode_nr_only_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE:
                return R.string.preferred_network_mode_nr_lte_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO:
                return R.string.preferred_network_mode_nr_lte_cdma_evdo_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_global_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_WCDMA:
                return R.string.preferred_network_mode_nr_lte_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM:
                return R.string.preferred_network_mode_nr_lte_tdscdma_gsm_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_gsm_wcdma_summary;
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                return R.string.preferred_network_mode_nr_lte_tdscdma_cdma_evdo_gsm_wcdma_summary;
            default:
                return R.string.preferred_network_mode_global_summary;
        }
    }

// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        if (mCallState != null && mCallState != TelephonyManager.CALL_STATE_IDLE) {
            callStateIdle = false;
        }
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
        Log.d(TAG, "isCallStateIdle:" + callStateIdle);
// QTI_BEGIN: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
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
// QTI_END: 2020-07-31: Telephony: Set some preferences to disabled when device is in call.
}
