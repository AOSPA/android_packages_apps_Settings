/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.networkchange.NetworkProtectionDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Preference controller for 'Enable 2G': Created RestrictedSwitchPreference for the 2G toggle
 * view, and applied useAdminDisabledSummary(true) along with the
 * userRestriction(UserManager.DISALLOW_CELLULAR_2G).
 *
 * <p>
 * This preference controller is invoked per subscription id, which means toggling 2g is a per-sim
 * operation. The requested 2g preference is delegated to
 * {@link TelephonyManager#setAllowedNetworkTypesForReason(int reason, long allowedNetworkTypes)}
 * with:
 * <ul>
 *     <li>{@code reason} {@link TelephonyManager#ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G}.</li>
 *     <li>{@code allowedNetworkTypes} with set or cleared 2g-related bits, depending on the
 *     requested preference state. </li>
 * </ul>
 */
public class Enable2gPreferenceController extends TelephonyTogglePreferenceController
        implements DefaultLifecycleObserver {

    private static final String LOG_TAG = "Enable2gPreferenceController";
    public static final String REQUEST_KEY = "request_key";
    private static final long BITMASK_2G = TelephonyManager.NETWORK_TYPE_BITMASK_GSM
            | TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
            | TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
            | TelephonyManager.NETWORK_TYPE_BITMASK_CDMA
            | TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private RestrictedSwitchPreference mRestrictedPreference;
    // This value will be set to true when used with the new Mobile Network Security page
    private boolean mShowSummaryAsSimName;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private Fragment mFragment;

    /**
     * Class constructor of "Enable 2G" toggle.
     *
     * @param context of settings
     * @param key     assigned within UI entry of XML file
     */
    public Enable2gPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mRestrictedPreference = null;
    }

    /**
     * Initialization based on a given subscription id.
     *
     * @param subId is the subscription id
     * @return this instance after initialization
     */
    public @NonNull Enable2gPreferenceController init(@NonNull Fragment host, int subId) {
        mSubId = subId;
        mFragment = host;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener =
                    new AllowedNetworkTypesListener(mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(() -> {
                if (mRestrictedPreference != null) {
                    updateState(mRestrictedPreference);
                }
            });
        }
        return this;
    }

    /**
     * Initialization based on a given subscription id and preference summary.
     *
     * @param host is the current Fragment object
     * @param subId is the subscription id
     * @param showSummaryAsSimName to show summary as a sim name
     * @return this instance after initialization
     */
    public @NonNull Enable2gPreferenceController init(@NonNull Fragment host, int subId,
                                                      boolean showSummaryAsSimName) {
        mShowSummaryAsSimName = showSummaryAsSimName;
        return init(host, subId);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.register(mContext, mSubId);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mAllowedNetworkTypesListener != null) {
            mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mRestrictedPreference = screen.findPreference(getPreferenceKey());
        mRestrictedPreference.useAdminDisabledSummary(true);
        mRestrictedPreference.getRestrictedPreferenceHelper()
                .setUserRestriction(UserManager.DISALLOW_CELLULAR_2G);
        if (isCarrierDisabled2gNetwork()) {
            mRestrictedPreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (!mRestrictedPreference.isChecked()) {
                                showNetworkProtectionDialog(
                                        R.string.network_protection_2g_off_alert_dialog_title,
                                        R.string.network_protection_2g_off_alert_dialog_desc,
                                        R.string.network_protection_2g_alert_dialog_turn_off_btn);
                            }
                            return false;
                        }
                    });
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }

        if (mShowSummaryAsSimName) {
            preference.setSummary(getSimCardName());
        }

        // The device admin decision overrides any carrier preferences
        if (isDisabledByAdmin()) {
            return;
        }

        preference.setEnabled(!is2gPreferredNetworkMode() && !mIsAirplaneModeOn);
        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return;
        }
        if (!mShowSummaryAsSimName) {
            preference.setSummary(mContext.getString(R.string.enable_2g_summary));
        }

        if (isCarrierDisabled2gNetwork()) {
            mFragment.getParentFragmentManager().setFragmentResultListener(REQUEST_KEY, mFragment,
                    (requestKey, result) -> onDialogResult(result));
        }
    }

    private String getSimCardName() {
        SubscriptionInfo subInfo = SubscriptionUtil.getSubById(mSubscriptionManager, mSubId);
        if (subInfo == null) {
            return "";
        }
        // It is the sim card name, and it should be the same name as the sim page.
        CharSequence simCardName = subInfo.getDisplayName();
        return TextUtils.isEmpty(simCardName) ? "" : simCardName.toString();
    }

    /**
     * Get the {@link com.android.settings.core.BasePreferenceController.AvailabilityStatus} for
     * this preference given a {@code subId}.
     * <p>
     * A return value of {@link #AVAILABLE} denotes that the 2G status can be updated for this
     * particular subscription.
     *
     * Otherwise,
     * <ul>
     *     <li>{@link CONDITIONALLY_UNAVAILABLE} is returned if the subscription is invalid
     *     ({@link SubscriptionManager#isUsableSubscriptionId}).</li>
     *     <li>{@link UNSUPPORTED_ON_DEVICE} is returned if the modem doesn't support 2G network
     *     mode or the HAL doesn't support
     *     <a href="https://cs.android.com/android/platform/superproject/+/master:hardware/interfaces/radio/1.6/IRadio.hal">Radio HAL version 1.6</a>
     *      or greater.</li>
     * </ul>
     */
    @Override
    public int getAvailabilityStatus(int subId) {
        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "getAvailabilityStatus: Telephony manager not yet initialized");
            return CONDITIONALLY_UNAVAILABLE;
        }
        if ((mTelephonyManager.getSupportedRadioAccessFamily() & BITMASK_2G) == 0) {
            Log.w(LOG_TAG, "getAvailabilityStatus: 2G unsupported");
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mTelephonyManager.isRadioInterfaceCapabilitySupported(
                    mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK)) {
            Log.w(LOG_TAG, "getAvailabilityStatus: allowed network types bitmask unsupported");
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    /**
     * Return {@code true} if only 3G and higher is currently enabled.
     *
     * <p><b>NOTE:</b> This method returns the active state of the preference controller and is not
     * the parameter passed into {@link #setChecked(boolean)}, which is instead the requested future
     * state.</p>
     */
    @Override
    public boolean isChecked() {
        // If an enterprise admin has disabled 2g, we show the toggle as checked to avoid
        // user confusion of seeing a unchecked toggle, but having 3G and higher actually enable.
        // The RestrictedSwitchPreference will take care of transparently informing the user that
        // the setting was disabled by their admin
        if (isDisabledByAdmin()) {
            return true;
        }

        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return false;
        }

        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "isChecked: Telephony manager not yet initialized");
            return false;
        }
        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);

        // Check for corrupted network types and fix if necessary
        if (currentlyAllowedNetworkTypes == 0) {
            long defaultNetworkTypes = RadioAccessFamily.getRafFromNetworkType(
                    RILConstants.PREFERRED_NETWORK_MODE);
            mTelephonyManager.setAllowedNetworkTypesForReason(
                    mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G, defaultNetworkTypes);
            currentlyAllowedNetworkTypes = defaultNetworkTypes;
        }
        return (currentlyAllowedNetworkTypes & BITMASK_2G) == 0;
    }

    /**
     * Ensure that the modem's allowed network types are configured according to the user's
     * preference.
     * <p>
     * See {@link com.android.settings.core.TogglePreferenceController#setChecked(boolean)} for
     * details.
     *
     * @param isChecked The toggle value that we're being requested to enforce. A value of {@code
     *                  true} denotes that 2g will be disabled by the modem after this function
     *                  completes, if it is not already.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked && isCarrierDisabled2gNetwork()) {
            return true;
        }
        return setAllowedNetworkTypes(isChecked);
    }

    private boolean setAllowedNetworkTypes(boolean isChecked) {
        if (isDisabledByAdmin()) {
            return false;
        }

        if (!SubscriptionManager.isUsableSubscriptionId(mSubId)) {
            return false;
        }

        if (mTelephonyManager == null) {
            Log.w(LOG_TAG, "setChecked: Telephony manager not yet initialized");
            return false;
        }

        long currentlyAllowedNetworkTypes = mTelephonyManager.getAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G);
        boolean enabled = (currentlyAllowedNetworkTypes & BITMASK_2G) != 0;
        if (enabled != isChecked) {
            return false;
        }
        long newAllowedNetworkTypes = currentlyAllowedNetworkTypes;
        if (isChecked) {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes & ~BITMASK_2G;
            Log.i(LOG_TAG, "Disabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        } else {
            newAllowedNetworkTypes = currentlyAllowedNetworkTypes | BITMASK_2G;
            Log.i(LOG_TAG, "Enabling 2g. Allowed network types: " + newAllowedNetworkTypes);
        }
        mTelephonyManager.setAllowedNetworkTypesForReason(
                mTelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G, newAllowedNetworkTypes);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_2G_ENABLED, !isChecked);
        return true;
    }

    private boolean isDisabledByAdmin() {
        return (mRestrictedPreference != null && mRestrictedPreference.isDisabledByAdmin());
    }

    private boolean is2gPreferredNetworkMode() {
        int networkMode = RadioAccessFamily.getNetworkTypeFromRaf(
                (int) mTelephonyManager.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
        Log.i(LOG_TAG, "PreferredNetworkMode: " + networkMode);
        return TelephonyManager.NETWORK_MODE_GSM_ONLY == networkMode;
    }

    private boolean isCarrierDisabled2gNetwork() {
        if (!Flags.keyCarrier2gToggle()) {
            return false;
        }
        final CarrierConfigManager carrierConfigManager =
                mContext.getSystemService(CarrierConfigManager.class);

        final PersistableBundle bundle = carrierConfigManager.getConfigForSubId(mSubId);
        if (bundle == null) {
            return false;
        }
        return bundle.getBoolean(
                CarrierConfigManager.KEY_CARRIER_DEFAULT_2G_PROTECTION_ENABLED_BOOL);
    }


    private void showNetworkProtectionDialog(int titleResId, int descResId,
                                             int positiveBtntxtResId) {
        NetworkProtectionDialogFragment.show(mFragment, mContext.getString(titleResId),
                mContext.getString(descResId), mContext.getString(positiveBtntxtResId));
    }

    @VisibleForTesting
    protected void onDialogResult(@NonNull Bundle result) {
        if (result.getInt(REQUEST_KEY) == DialogInterface.BUTTON_POSITIVE) {
            if (mRestrictedPreference.isChecked()) {
                mRestrictedPreference.setChecked(false);
            }
            setAllowedNetworkTypes(false);
            NotificationManager notificationManager = mContext.getSystemService(
                    NotificationManager.class);
            notificationManager.cancel(
                    NetworkChangeNotification.NETWORK_PROTECTION_2G_NOTIFICATION_ID + mSubId);
        } else {
            if (!mRestrictedPreference.isChecked()) {
                mRestrictedPreference.setChecked(true);
            }
        }
    }
}
