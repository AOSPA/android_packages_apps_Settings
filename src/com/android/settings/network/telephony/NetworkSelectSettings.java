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
// QTI_BEGIN: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
// QTI_END: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
// QTI_BEGIN: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f
 * Copyright (c) 2022-2025 Qualcomm Innovation Center, Inc. All rights reserved.
// QTI_END: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.annotations.Initializer;
import com.android.internal.telephony.OperatorInfo;
import com.android.settings.R;
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
import com.android.settings.Utils;
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
import com.android.settings.dashboard.DashboardFragment;
// QTI_BEGIN: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
import com.android.settings.network.SubscriptionsChangeListener;
// QTI_END: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
import com.android.settings.network.telephony.scan.NetworkScanRepository;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.ZeroStatePreference;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import kotlinx.coroutines.Job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
import java.util.Set;
import java.util.HashSet;
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * "Choose network" settings UI for the Settings app.
 */
@Keep
// QTI_BEGIN: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
public class NetworkSelectSettings extends DashboardFragment implements
         SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
// QTI_END: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed

    private static final String TAG = "NetworkSelectSettings";

    private static final int EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE = 1;

    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";
// QTI_BEGIN: 2023-12-06: Telephony: Separate error status message to display
    private static final String PREF_KEY_ERROR_MSG = "error_msg_preference";
// QTI_END: 2023-12-06: Telephony: Separate error status message to display

    private PreferenceCategory mPreferenceCategory;
// QTI_BEGIN: 2023-12-06: Telephony: Separate error status message to display
    private PreferenceCategory mErrorMsgCategory;
// QTI_END: 2023-12-06: Telephony: Separate error status message to display
    @VisibleForTesting
    NetworkOperatorPreference mSelectedPreference;
// QTI_BEGIN: 2024-01-28: Telephony: Allow user to change network selection mode
    NetworkOperatorPreference mConnectedPreference;
// QTI_END: 2024-01-28: Telephony: Allow user to change network selection mode
    private View mProgressHeader;
    private ZeroStatePreference mStatusMessagePreference;
// QTI_BEGIN: 2023-12-06: Telephony: Separate error status message to display
    private Preference mErrorMsgPreference;
// QTI_END: 2023-12-06: Telephony: Separate error status message to display
    @VisibleForTesting
    @NonNull
    List<CellInfo> mCellInfoList = ImmutableList.of();
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private TelephonyManager mTelephonyManager;
// QTI_BEGIN: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
    SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionsChangeListener;
// QTI_END: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
    private SatelliteManager mSatelliteManager;
    private CarrierConfigManager mCarrierConfigManager;
    private List<String> mForbiddenPlmns;
    private boolean mShow4GForLTE = false;
    private final ExecutorService mNetworkScanExecutor = Executors.newFixedThreadPool(1);
    private MetricsFeatureProvider mMetricsFeatureProvider;
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
    private boolean mIsAdvancedScanSupported;
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
    private CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener;
    private AtomicBoolean mShouldFilterOutSatellitePlmn = new AtomicBoolean();

    private NetworkScanRepository mNetworkScanRepository;
    @Nullable
    private Job mNetworkScanJob = null;

    private NetworkSelectRepository mNetworkSelectRepository;
// QTI_BEGIN: 2025-01-01: Telephony: Skip connected preference in case of error
    private NetworkScanRepository.NetworkScanState mState =
            NetworkScanRepository.NetworkScanState.ACTIVE;
// QTI_END: 2025-01-01: Telephony: Skip connected preference in case of error

    private ListenableFuture mSelectNetworkFuture;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        onCreateInitialization();
    }

    @Keep
    @VisibleForTesting
    @Initializer
    protected void onCreateInitialization() {
        Context context = getContext();

// QTI_BEGIN: 2021-06-20: Telephony: Change for IExtphone implementation
        if (TelephonyUtils.isServiceConnected()) {
            mIsAdvancedScanSupported = TelephonyUtils.isAdvancedPlmnScanSupported(
                    getContext());
        } else {
            Log.d(TAG, "ExtTelephonyService is not connected!!! ");
        }
// QTI_END: 2021-06-20: Telephony: Change for IExtphone implementation
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
        Log.d(TAG, "mIsAdvancedScanSupported: " + mIsAdvancedScanSupported);
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
        mSubId = getSubId();

        mPreferenceCategory = getPreferenceCategory(PREF_KEY_NETWORK_OPERATORS);
// QTI_BEGIN: 2023-12-06: Telephony: Separate error status message to display
        mErrorMsgCategory = getPreferenceCategory(PREF_KEY_ERROR_MSG);
        mErrorMsgPreference = new Preference(getContext());
        mErrorMsgPreference.setSelectable(false);
// QTI_END: 2023-12-06: Telephony: Separate error status message to display
        mSelectedPreference = null;
// QTI_BEGIN: 2024-01-28: Telephony: Allow user to change network selection mode
        mConnectedPreference = null;
// QTI_END: 2024-01-28: Telephony: Allow user to change network selection mode
        mTelephonyManager = getTelephonyManager(context, mSubId);
        mSatelliteManager = getSatelliteManager(context);
        mCarrierConfigManager = getCarrierConfigManager(context);
// QTI_BEGIN: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
        mSubscriptionManager = getContext().getSystemService(SubscriptionManager.class);
        mSubscriptionsChangeListener = new SubscriptionsChangeListener(getContext(), this);
// QTI_END: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
        PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(mSubId,
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL);
        mShow4GForLTE = bundle.getBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                false);
        mShouldFilterOutSatellitePlmn.set(bundle.getBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
                true));

        mMetricsFeatureProvider = getMetricsFeatureProvider(context);

        mCarrierConfigChangeListener =
                (slotIndex, subId, carrierId, specificCarrierId) -> handleCarrierConfigChanged(
                        subId);
        mCarrierConfigManager.registerCarrierConfigChangeListener(mNetworkScanExecutor,
                mCarrierConfigChangeListener);
        mNetworkScanRepository = new NetworkScanRepository(context, mSubId);
        mNetworkSelectRepository = new NetworkSelectRepository(context, mSubId);
// QTI_BEGIN: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
        mSubscriptionsChangeListener.start();
// QTI_END: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
    }

    @Keep
    @VisibleForTesting
    protected PreferenceCategory getPreferenceCategory(String preferenceKey) {
        return findPreference(preferenceKey);
    }

    @Keep
    @VisibleForTesting
    protected TelephonyManager getTelephonyManager(Context context, int subscriptionId) {
        return context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(subscriptionId);
    }

    @Keep
    @VisibleForTesting
    protected CarrierConfigManager getCarrierConfigManager(Context context) {
        return context.getSystemService(CarrierConfigManager.class);
    }

    @Keep
    @VisibleForTesting
    protected MetricsFeatureProvider getMetricsFeatureProvider(Context context) {
        return FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Keep
    @VisibleForTesting
    @Nullable
    protected SatelliteManager getSatelliteManager(Context context) {
        return context.getSystemService(SatelliteManager.class);
    }

    @Keep
    @VisibleForTesting
    protected boolean isPreferenceScreenEnabled() {
        return getPreferenceScreen().isEnabled();
    }

    @Keep
    @VisibleForTesting
    protected void enablePreferenceScreen(boolean enable) {
        getPreferenceScreen().setEnabled(enable);
    }

    @Keep
    @VisibleForTesting
    protected int getSubId() {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        return subId;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressHeader = setPinnedHeaderView(
                com.android.settingslib.widget.progressbar.R.layout.progress_header
        ).findViewById(com.android.settingslib.widget.progressbar.R.id.progress_bar_animation);
        mNetworkSelectRepository.launchUpdateNetworkRegistrationInfo(
                getViewLifecycleOwner(),
                (info) -> {
                    forceUpdateConnectedPreferenceCategory(info);
                    return Unit.INSTANCE;
                });
        launchNetworkScan();
    }

    private void launchNetworkScan() {
        setProgressBarVisible(true);
        mNetworkScanJob = mNetworkScanRepository.launchNetworkScan(getViewLifecycleOwner(),
                (networkScanResult) -> {
                    if (isPreferenceScreenEnabled() && !isFinishingOrDestroyed()) {
                        scanResultHandler(networkScanResult);
                    }

                    return Unit.INSTANCE;
                });
    }

    /**
     * Update forbidden PLMNs from the USIM App
     */
    @Keep
    @VisibleForTesting
    protected void updateForbiddenPlmns() {
        final String[] forbiddenPlmns = mTelephonyManager.getForbiddenPlmns();
        mForbiddenPlmns = forbiddenPlmns != null
                ? Arrays.asList(forbiddenPlmns)
                : new ArrayList<>();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSelectedPreference) {
            Log.d(TAG, "onPreferenceTreeClick: preference is mSelectedPreference. Do nothing.");
            return true;
        }
        if (!(preference instanceof NetworkOperatorPreference)) {
            Log.d(TAG, "onPreferenceTreeClick: preference is not the NetworkOperatorPreference.");
            return false;
        }

        // Need stop network scan before manual select network.
        if (mNetworkScanJob != null) {
            mNetworkScanJob.cancel(null);
            mNetworkScanJob = null;
        }

        // Refresh the last selected item in case users reselect network.
        clearPreferenceSummary();
        if (mSelectedPreference != null) {
// QTI_BEGIN: 2024-01-28: Telephony: Allow user to change network selection mode
            // Set summary as "Disconnected" to the previously selected network
// QTI_END: 2024-01-28: Telephony: Allow user to change network selection mode
            mSelectedPreference.setSummary(R.string.network_disconnected);
// QTI_BEGIN: 2024-01-28: Telephony: Allow user to change network selection mode
        } else if (mConnectedPreference != null) {
            // Set summary as "Disconnected" to the previously connected network
            mConnectedPreference.setSummary(R.string.network_disconnected);
// QTI_END: 2024-01-28: Telephony: Allow user to change network selection mode
        }

        mSelectedPreference = (NetworkOperatorPreference) preference;
        mSelectedPreference.setSummary(R.string.network_connecting);
// QTI_BEGIN: 2024-01-28: Telephony: Allow user to change network selection mode
        mConnectedPreference = mSelectedPreference;
// QTI_END: 2024-01-28: Telephony: Allow user to change network selection mode

        mMetricsFeatureProvider.action(getContext(),
                SettingsEnums.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK);

        setProgressBarVisible(true);
        // Disable the screen until network is manually set
        enablePreferenceScreen(false);

        final OperatorInfo operator = mSelectedPreference.getOperatorInfo();
        mSelectNetworkFuture = ThreadUtils.postOnBackgroundThread(() -> {
            final Message msg = mHandler.obtainMessage(
                    EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE);
            msg.obj = mTelephonyManager.setNetworkSelectionModeManual(
                    operator, true /* persistSelection */);
            msg.sendToTarget();
        });

        return true;
    }

// QTI_BEGIN: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        boolean isActiveSubscriptionId = mSubscriptionManager.isActiveSubscriptionId(mSubId);
        Log.d(TAG, "onSubscriptionsChanged, mSubId: " + mSubId
                + ", isActive: " + isActiveSubscriptionId);

        if (!isActiveSubscriptionId) {
            // The current subscription is no longer active, possibly because the SIM was removed.
            // Finish the activity.
            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                Log.d(TAG, "Calling finish");
                activity.finish();
            }
        }
    }

// QTI_END: 2021-07-09: Telephony: Finish PLMN search result UI when SIM is removed
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.choose_network;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK_SELECT;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
// QTI_BEGIN: 2020-04-27: Telephony: Add sanity check for slotIndex during scan abort
            Log.d(TAG, "handleMessage, msg.what: " + msg.what);
// QTI_END: 2020-04-27: Telephony: Add sanity check for slotIndex during scan abort
            switch (msg.what) {
                case EVENT_SET_NETWORK_SELECTION_MANUALLY_DONE:
                    Context context = getContext();
                    if (context == null || isFinishingOrDestroyed()) {
                        Log.d(TAG, "Activity finished or context null, skipping UI update");
                        return;
                    }
                    final boolean isSucceed = (boolean) msg.obj;
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
                    setProgressBarVisible(false);
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
                    enablePreferenceScreen(true);

                    if (mSelectedPreference != null) {
                        mSelectedPreference.setSummary(isSucceed
                                ? R.string.network_connected
                                : R.string.network_could_not_connect);
// QTI_BEGIN: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f

                        if (isSucceed && mSubscriptionManager.isActiveSubscriptionId(mSubId)) {
                            final OperatorInfo operator = mSelectedPreference.getOperatorInfo();
// QTI_END: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f
                            MobileNetworkUtils.setCarrierName(context,
// QTI_BEGIN: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f
                                    operator.getOperatorAlphaLong(), mSubId);
                        }
// QTI_END: 2025-06-23: Telephony: Use selected plmn name as summary of choose network am: 0f4a28c31f am: 0f4a28c31f
                    } else {
                        Log.e(TAG, "No preference to update!");
                    }
                    break;
            }
        }
    };

    /* We do not want to expose carrier satellite plmns to the user when manually scan the
       cellular network. Therefore, it is needed to filter out satellite plmns from current cell
       info list  */
    @VisibleForTesting
    List<CellInfo> filterOutSatellitePlmn(List<CellInfo> cellInfoList) {
        List<String> aggregatedSatellitePlmn = getSatellitePlmnsForCarrierWrapper();
        if (!mShouldFilterOutSatellitePlmn.get() || aggregatedSatellitePlmn.isEmpty()) {
            return cellInfoList;
        }
        return cellInfoList.stream()
                .filter(cellInfo -> !aggregatedSatellitePlmn.contains(
                        CellInfoUtil.getOperatorNumeric(cellInfo.getCellIdentity())))
                .collect(Collectors.toList());
    }

    /**
     * Serves as a wrapper method for {@link SatelliteManager#getSatellitePlmnsForCarrier(int)}.
     * Since SatelliteManager is final, this wrapper enables mocking or spying of
     * {@link SatelliteManager#getSatellitePlmnsForCarrier(int)} for unit testing purposes.
     */
    @VisibleForTesting
    protected List<String> getSatellitePlmnsForCarrierWrapper() {
        if (mSatelliteManager != null) {
            return mSatelliteManager.getSatellitePlmnsForCarrier(mSubId);
        } else {
            Log.e(TAG, "mSatelliteManager is null, return empty list");
            return new ArrayList<>();
        }
    }

    private void handleCarrierConfigChanged(int subId) {
        PersistableBundle config = mCarrierConfigManager.getConfigForSubId(subId,
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL);
        boolean shouldFilterSatellitePlmn = config.getBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL,
                true);
        if (shouldFilterSatellitePlmn != mShouldFilterOutSatellitePlmn.get()) {
            mShouldFilterOutSatellitePlmn.set(shouldFilterSatellitePlmn);
        }
    }

    @VisibleForTesting
    protected void scanResultHandler(NetworkScanRepository.NetworkScanResult results) {
// QTI_BEGIN: 2023-03-27: Android_UI: Settings: Fix force close for updating UI after activity destroyed.
        if (isFinishingOrDestroyed()) {
            Log.d(TAG, "scanResultHandler: activity isFinishingOrDestroyed, directly return");
            return;
        }

// QTI_END: 2023-03-27: Android_UI: Settings: Fix force close for updating UI after activity destroyed.
// QTI_BEGIN: 2025-01-06: Telephony: Remove error message preference
        int errorCount = mErrorMsgCategory.getPreferenceCount();
        if (errorCount > 0) mErrorMsgCategory.removeAll();

// QTI_END: 2025-01-06: Telephony: Remove error message preference
        mCellInfoList = filterOutSatellitePlmn(results.getCellInfos());
        Log.d(TAG, "CellInfoList: " + CellInfoUtil.cellInfoListToString(mCellInfoList));
        updateAllPreferenceCategory();
// QTI_BEGIN: 2025-01-01: Telephony: Skip connected preference in case of error
        mState = results.getState();
        int plmnCount = mPreferenceCategory.getPreferenceCount();
        if (mState == NetworkScanRepository.NetworkScanState.ERROR) {
// QTI_END: 2025-01-01: Telephony: Skip connected preference in case of error
// QTI_BEGIN: 2024-09-17: Telephony: Settings: Do not clean up previous searched plmn list
            if (plmnCount > 0) {
                addErrorMessagePreference(R.string.network_scan_error);
            } else {
                addMessagePreference(R.string.network_query_error);
            }
// QTI_END: 2024-09-17: Telephony: Settings: Do not clean up previous searched plmn list
        } else if (mCellInfoList.isEmpty()) {
            addMessagePreference(R.string.empty_networks_list);
        }
        // keep showing progress bar, it will be stopped when error or completed
// QTI_BEGIN: 2025-01-01: Telephony: Skip connected preference in case of error
        setProgressBarVisible(mState == NetworkScanRepository.NetworkScanState.ACTIVE);
// QTI_END: 2025-01-01: Telephony: Skip connected preference in case of error
    }

    @Keep
    @VisibleForTesting
    protected NetworkOperatorPreference createNetworkOperatorPreference(CellInfo cellInfo) {
        if (mForbiddenPlmns == null) {
            updateForbiddenPlmns();
        }
        NetworkOperatorPreference preference =
            new NetworkOperatorPreference(getPrefContext(),
                mForbiddenPlmns, mShow4GForLTE,
// QTI_BEGIN: 2023-11-20: Telephony: Use a proper context
                MobileNetworkUtils.getAccessMode(getPrefContext().getApplicationContext(),
// QTI_END: 2023-11-20: Telephony: Use a proper context
                        mTelephonyManager.getSlotIndex()));
// QTI_BEGIN: 2024-03-24: Android_UI: Settings: Adapt domestic roaming FR for AOSP change.
        if (!DomesticRoamUtils.isFeatureEnabled(getPrefContext())) {
            preference.updateCell(cellInfo);
        }
// QTI_END: 2024-03-24: Android_UI: Settings: Adapt domestic roaming FR for AOSP change.
        return preference;
    }

    /**
     * Update the content of network operators list.
     */
    private void updateAllPreferenceCategory() {
        int numberOfPreferences = mPreferenceCategory.getPreferenceCount();

        // remove unused preferences
        while (numberOfPreferences > mCellInfoList.size()) {
            numberOfPreferences--;
            mPreferenceCategory.removePreference(
                    mPreferenceCategory.getPreference(numberOfPreferences));
        }

        // update the content of preference
        for (int index = 0; index < mCellInfoList.size(); index++) {
            final CellInfo cellInfo = mCellInfoList.get(index);

            NetworkOperatorPreference pref = null;
            if (index < numberOfPreferences) {
                final Preference rawPref = mPreferenceCategory.getPreference(index);
                if (rawPref instanceof NetworkOperatorPreference) {
                    // replace existing preference
                    pref = (NetworkOperatorPreference) rawPref;
                    pref.updateCell(cellInfo);
                } else {
                    mPreferenceCategory.removePreference(rawPref);
                }
            }
            if (pref == null) {
                // add new preference
                pref = createNetworkOperatorPreference(cellInfo);
                pref.setOrder(index);

// QTI_BEGIN: 2023-11-20: Telephony: Use a proper context
                if (DomesticRoamUtils.isFeatureEnabled(getPrefContext())) {
// QTI_END: 2023-11-20: Telephony: Use a proper context
// QTI_BEGIN: 2022-10-07: Telephony: Merge "Settings: support CU operator ID display in domestic roaming status" into t-keystone-qcom-dev
                    pref.setSubId(mSubId);
                    pref.updateCell(cellInfo);
                }

// QTI_END: 2022-10-07: Telephony: Merge "Settings: support CU operator ID display in domestic roaming status" into t-keystone-qcom-dev
                mPreferenceCategory.addPreference(pref);
            }
            pref.setKey(pref.getOperatorName());

            if (mCellInfoList.get(index).isRegistered()) {
                pref.setSummary(R.string.network_connected);
            } else {
                pref.setSummary(null);
            }
        }
    }

    /**
     * Config the network operator list when the page was created. When user get
     * into this page, the device might or might not have data connection.
     * - If the device has data:
     * 1. use {@code ServiceState#getNetworkRegistrationInfoList()} to get the currently
     * registered cellIdentity, wrap it into a CellInfo;
     * 2. set the signal strength level as strong;
     * 3. get the title of the previously connected network operator, since the CellIdentity
     * got from step 1 only has PLMN.
     * - If the device has no data, we will remove the connected network operators list from the
     * screen.
     */
    private void forceUpdateConnectedPreferenceCategory(
            NetworkSelectRepository.NetworkRegistrationAndForbiddenInfo info) {
// QTI_BEGIN: 2025-01-01: Telephony: Skip connected preference in case of error
        if (mState == NetworkScanRepository.NetworkScanState.ERROR) {
            Log.d(TAG, "forceUpdateConnectedPreferenceCategory: network scan error");
            return;
        }
// QTI_END: 2025-01-01: Telephony: Skip connected preference in case of error
        mPreferenceCategory.removeAll();
        for (NetworkRegistrationInfo regInfo : info.getNetworkList()) {
            final CellIdentity cellIdentity = regInfo.getCellIdentity();
            if (cellIdentity == null) {
                continue;
            }
            final NetworkOperatorPreference pref = new NetworkOperatorPreference(
                    getPrefContext(), info.getForbiddenPlmns(), mShow4GForLTE,
                    MobileNetworkUtils.getAccessMode(getPrefContext().getApplicationContext(),
                            mTelephonyManager.getSlotIndex()));
            pref.updateCell(null, cellIdentity);
            if (pref.isForbiddenNetwork()) {
                continue;
// QTI_BEGIN: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
            }
// QTI_END: 2020-02-05: Telephony: Add support for incremental scan via QCRIL hooks
            pref.setSummary(R.string.network_connected);
            // Update the signal strength icon, since the default signalStrength value
            // would be zero
            // (it would be quite confusing why the connected network has no signal)
            pref.setIcon(SignalStrength.NUM_SIGNAL_STRENGTH_BINS - 1);
            mPreferenceCategory.addPreference(pref);
            break;
        }
    }

    /**
     * Clear all of the preference summary
     */
    private void clearPreferenceSummary() {
        int idxPreference = mPreferenceCategory.getPreferenceCount();
        while (idxPreference > 0) {
            idxPreference--;
            final Preference networkOperator = mPreferenceCategory.getPreference(idxPreference);
            networkOperator.setSummary(null);
        }
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void addMessagePreference(int messageId) {
        if (mStatusMessagePreference == null) {
            mStatusMessagePreference = new ZeroStatePreference(getContext());
            mStatusMessagePreference.setIcon(R.drawable.cell_tower_24px);
            mStatusMessagePreference.setSelectable(false);
        }
        mStatusMessagePreference.setTitle(messageId);
        mPreferenceCategory.removeAll();
        mPreferenceCategory.addPreference(mStatusMessagePreference);
    }

// QTI_BEGIN: 2023-12-06: Telephony: Separate error status message to display
    private void addErrorMessagePreference(int messageId) {
        mErrorMsgPreference.setTitle(messageId);
        mErrorMsgCategory.addPreference(mErrorMsgPreference);
    }

// QTI_END: 2023-12-06: Telephony: Separate error status message to display
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy(): mSelectNetworkFuture = " + mSelectNetworkFuture);
        if (mSelectNetworkFuture != null && !mSelectNetworkFuture.isDone()) {
            Log.d(TAG, "onDestroy(): call future cancel");
            mSelectNetworkFuture.cancel(true);
        }
// QTI_BEGIN: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
        mSubscriptionsChangeListener.stop();
// QTI_END: 2024-06-18: Telephony: Start SubscriptionsChangeListener on PLMN search.
        mNetworkScanExecutor.shutdown();
        super.onDestroy();
    }
}
