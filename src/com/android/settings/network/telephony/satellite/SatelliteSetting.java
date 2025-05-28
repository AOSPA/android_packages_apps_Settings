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

import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_EMERGENCY_MESSAGING_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import static com.android.settings.network.telephony.satellite.SatelliteCarrierSettingUtils.isSatelliteDataRestricted;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;

/** Handle Satellite Setting Preference Layout. */
public class SatelliteSetting extends RestrictedDashboardFragment {
    private static final String TAG = "SatelliteSetting";

    static final String SUB_ID = "sub_id";
    static final String EXTRA_IS_SERVICE_DATA_TYPE = "is_service_data_type";
    static final String EXTRA_IS_SMS_AVAILABLE_FOR_MANUAL_TYPE = "is_sms_available";

    private Activity mActivity;
    private SatelliteManager mSatelliteManager;
    private PersistableBundle mConfigBundle;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mIsSmsAvailableForManualType = false;

    public SatelliteSetting() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SATELLITE_SETTING;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mSatelliteManager = mActivity.getSystemService(SatelliteManager.class);
        if (mSatelliteManager == null) {
            Log.d(TAG, "SatelliteManager is null, do nothing.");
            finish();
            return;
        }
        mSubId = mActivity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mConfigBundle = fetchCarrierConfigData(mSubId);
        if (!isSatelliteAttachSupported()) {
            Log.d(TAG, "SatelliteSettings: KEY_SATELLITE_ATTACH_SUPPORTED_BOOL is false, "
                    + "do nothing.");
            finish();
        }
        mIsSmsAvailableForManualType = getIntent().getBooleanExtra(
                EXTRA_IS_SMS_AVAILABLE_FOR_MANUAL_TYPE, false);
        boolean isDataAvailableAndNotRestricted = isDataAvailableAndNotRestricted();
        use(SatelliteAppListCategoryController.class).init(mSubId, mConfigBundle,
                mIsSmsAvailableForManualType, isDataAvailableAndNotRestricted);
        use(SatelliteSettingAboutContentController.class).init(mSubId);
        use(SatelliteSettingAccountInfoController.class).init(mSubId, mConfigBundle,
                mIsSmsAvailableForManualType, isDataAvailableAndNotRestricted);
        use(SatelliteSettingFooterController.class).init(mSubId, mConfigBundle);
        use(SatelliteSettingIndicatorController.class).init(mSubId, mConfigBundle);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.satellite_setting;
    }

    private PersistableBundle fetchCarrierConfigData(int subId) {
        CarrierConfigManager carrierConfigManager = mActivity.getSystemService(
                CarrierConfigManager.class);
        PersistableBundle bundle = CarrierConfigManager.getDefaultConfig();
        try {
            bundle = carrierConfigManager.getConfigForSubId(subId,
                    KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                    KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING,
                    KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                    KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL,
                    KEY_EMERGENCY_MESSAGING_SUPPORTED_BOOL);
            if (bundle.isEmpty()) {
                Log.d(TAG, "SatelliteSettings: getDefaultConfig");
                bundle = CarrierConfigManager.getDefaultConfig();
            }
        } catch (IllegalStateException exception) {
            Log.d(TAG, "SatelliteSettings exception : " + exception);
        }
        return bundle;
    }

    private boolean isSatelliteAttachSupported() {
        return mConfigBundle.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, false);
    }

    private boolean isDataAvailableAndNotRestricted() {
        return getIntent().getBooleanExtra(EXTRA_IS_SERVICE_DATA_TYPE, false)
                && !isSatelliteDataRestricted(getContext(), mSubId);
    }
}
