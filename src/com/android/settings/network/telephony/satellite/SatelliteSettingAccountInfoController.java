/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.telephony.satellite;

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;

/** A controller to control content of "Your mobile plan". */
public class SatelliteSettingAccountInfoController extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteSettingAccountInfoController";
    @VisibleForTesting
    static final String PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN =
            "key_category_your_satellite_plan";
    @VisibleForTesting
    static final String PREF_KEY_YOUR_SATELLITE_PLAN = "key_your_satellite_plan";
    @VisibleForTesting
    static final String PREF_KEY_YOUR_SATELLITE_DATA_PLAN = "key_your_satellite_data_plan";

    private PreferenceScreen mScreen;
    private String mSimOperatorName;
    private boolean mIsSmsAvailable;
    private boolean mIsDataAvailable;
    private boolean mIsSatelliteEligible;
    private int mDataMode = SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;

    public SatelliteSettingAccountInfoController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Initialize the UI settings. */
    public void init(int subId) {
        mSubId = subId;
        mSimOperatorName = mContext.getSystemService(TelephonyManager.class).getSimOperatorName(
                mSubId);
    }

    void setCarrierRoamingNtnAvailability(boolean isSmsAvailable, boolean isDataAvailable,
            int dataMode) {
        mIsSmsAvailable = isSmsAvailable;
        mIsDataAvailable = isDataAvailable;
        mDataMode = dataMode;
        mIsSatelliteEligible = isSatelliteEligible();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        mScreen = screen;
        super.displayPreference(screen);
        updatePreferences();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updatePreferences();
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        SatelliteSettingsRepository repository =
                FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                        .getSatelliteSettingsRepository();
        if (repository.getSatelliteNtnConnectType(mSubId)
                == CARRIER_ROAMING_NTN_CONNECT_MANUAL
                || (Flags.vzwAstSkyloFallback()
                && repository.getSatelliteNtnConnectType(mSubId)
                == CARRIER_ROAMING_NTN_CONNECT_HYBRID)) {
            return AVAILABLE_UNSEARCHABLE;
        }
        return repository.isSatelliteEntitlementSupported(mSubId)
                ? AVAILABLE_UNSEARCHABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    private void updatePreferences() {
        if (mScreen == null) {
            return;
        }
        PreferenceCategory prefCategory = mScreen.findPreference(
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        // Your mobile plan
        prefCategory.setTitle(mContext.getString(R.string.category_title_your_satellite_plan,
                mSimOperatorName));

        if (mIsSatelliteEligible) {
            handleEligibleUI();
            return;
        }
        handleIneligibleUI();
    }

    private void handleEligibleUI() {
        Preference messagingPreference = mScreen.findPreference(PREF_KEY_YOUR_SATELLITE_PLAN);
        Drawable icon = mContext.getDrawable(R.drawable.ic_check_circle_24px);
        /* In case satellite is allowed by carrier's entitlement server, the page will show
               the check icon with guidance that satellite is included in user's mobile plan */
        messagingPreference.setTitle(R.string.title_have_satellite_plan);
        messagingPreference.setSummary(null);
        messagingPreference.setOnPreferenceClickListener(null);
        if (mIsDataAvailable && mDataMode > SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED) {
            Preference connectivityPreference = mScreen.findPreference(
                    PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
            connectivityPreference.setIcon(icon);
            connectivityPreference.setVisible(true);
            connectivityPreference
                    .setTitle(mDataMode == SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED
                            ? R.string.title_have_satellite_constrained_data_plan
                            : R.string.title_have_satellite_unconstrained_data_plan);
        }

        icon.setTintList(Utils.getColorAttr(mContext, android.R.attr.textColorPrimary));
        messagingPreference.setIcon(icon);
    }

    private void handleIneligibleUI() {
        Preference messagingPreference = mScreen.findPreference(PREF_KEY_YOUR_SATELLITE_PLAN);
        /* Or, it will show the blocked icon with the guidance that satellite is not included
               in user's mobile plan */
        messagingPreference.setTitle(R.string.title_no_satellite_plan);
        String url = FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                .getCarrierConfigRepository().getString(mSubId,
                        KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING);
        if (url != null && !url.isEmpty()) {
            /* And, the link url provides more information via web page will be shown */
            SpannableString spannable = new SpannableString(
                    mContext.getString(R.string.summary_add_satellite_setting));
            spannable.setSpan(new UnderlineSpan(), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            messagingPreference.setSummary(spannable);
            /* The link will lead users to a guide page */
            messagingPreference.setOnPreferenceClickListener(pref -> {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mContext.startActivity(intent);
                return true;
            });
        }

        Drawable icon = mContext.getDrawable(R.drawable.ic_block_24px);
        icon.setTintList(Utils.getColorAttr(mContext, android.R.attr.textColorPrimary));
        messagingPreference.setIcon(icon);
    }

    @VisibleForTesting
    protected boolean isSatelliteEligible() {
        SatelliteSettingsRepository repository =
                FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                        .getSatelliteSettingsRepository();
        if (repository.getSatelliteNtnConnectType(mSubId)
                == CARRIER_ROAMING_NTN_CONNECT_MANUAL) {
            return mIsSmsAvailable;
        }

        if (Flags.vzwAstSkyloFallback()
                && repository.getSatelliteNtnConnectType(mSubId)
                == CARRIER_ROAMING_NTN_CONNECT_HYBRID) {
            if (repository.isSatelliteEntitlementSupported(mSubId)) {
                if (SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext, mSubId)) {
                    return true;
                } else {
                    return mIsSmsAvailable;
                }
            }
        }
        return SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext, mSubId);
    }
}
