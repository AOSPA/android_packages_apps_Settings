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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/** A controller for showing the dynamic disclaimer of Satellite service. */
public class SatelliteSettingFooterController extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteSettingFooterController";
    @VisibleForTesting
    static final String KEY_FOOTER_PREFERENCE = "satellite_setting_extra_info_footer_pref";

    private String mSimOperatorName;
    private boolean mIsSmsAvailable;
    private boolean mIsSatelliteEligible;

    public SatelliteSettingFooterController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    void init(int subId) {
        mSubId = subId;
        mSimOperatorName = mContext.getSystemService(TelephonyManager.class).getSimOperatorName(
                subId);
    }

    void setCarrierRoamingNtnAvailability(boolean isSmsAvailable, boolean isDataAvailable,
            int dataMode) {
        mIsSmsAvailable = isSmsAvailable;
        mIsSatelliteEligible = isSatelliteEligible();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        updateFooterContent(screen);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return AVAILABLE_UNSEARCHABLE;
    }

    private void updateFooterContent(PreferenceScreen screen) {
        // More about satellite messaging
        FooterPreference footerPreference = screen.findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference == null) {
            return;
        }
        footerPreference.setSummary(
                Html.fromHtml(getFooterContent(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM));
        final String link = readSatelliteMoreInfoString();
        if (link == null || link.isEmpty()) {
            return;
        }
        footerPreference.setLearnMoreAction(view -> {
            Intent helpIntent = HelpUtils.getHelpIntent(mContext, link, this.getClass().getName());
            if (helpIntent != null) {
                mContext.startActivityForResult(mContext.getPackageName(),
                        helpIntent, /*requestCode=*/ 0, null);
            }
        });
        footerPreference.setLearnMoreText(
                mContext.getString(R.string.more_about_satellite_connectivity));
    }

    private String getFooterContent() {
        SatelliteSettingsRepository repository =
                FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                        .getSatelliteSettingsRepository();
        boolean isEntitlementSupport = repository.isSatelliteEntitlementSupported(mSubId);
        int ntnType = repository.getSatelliteNtnConnectType(mSubId);

        String result = "";
        result = mContext.getString(R.string.satellite_footer_content_section_0) + "\n\n";
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_1);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_2);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_3);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_4);

        if (isEntitlementSupport) {
            switch (ntnType) {
                case CARRIER_ROAMING_NTN_CONNECT_MANUAL:
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                            mSimOperatorName);
                    break;
                case CARRIER_ROAMING_NTN_CONNECT_HYBRID:
                    if (mIsSatelliteEligible) {
                        result += getHtmlStringCombination(
                                R.string.satellite_footer_content_section_5);
                    }
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                            mSimOperatorName);
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_8);
                    break;
                case CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC:
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_5);
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                            mSimOperatorName);
                    result += getHtmlStringCombination(R.string.satellite_footer_content_section_8);
                    break;
                default:
                    Log.d(TAG, "Illegible type : " + ntnType);
            }
        } else {
            if (ntnType == CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC) {
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_6);
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_5);
                return result;
            }
            result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                    mSimOperatorName);
            result += getHtmlStringCombination(R.string.satellite_footer_content_section_8);
        }
        return result;
    }

    private String getHtmlStringCombination(int resId) {
        String prefix = "<li>&#160;";
        String subfix = "</li>";
        return prefix + mContext.getString(resId) + subfix;
    }

    private String getHtmlStringCombination(int resId, Object... value) {
        String prefix = "<li>&#160;";
        String subfix = "</li>";
        return prefix + mContext.getString(resId, value) + subfix;
    }

    private String readSatelliteMoreInfoString() {
        String url = FeatureFactory.getFeatureFactory().getTelephonyFeatureProvider()
                .getCarrierConfigRepository().getString(mSubId,
                        KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING);
        return (url == null) ? "" : url;
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
