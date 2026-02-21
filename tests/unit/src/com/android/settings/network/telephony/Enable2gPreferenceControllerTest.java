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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.CarrierConfigManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class Enable2gPreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final String PREFERENCE_KEY = "TEST_2G_PREFERENCE";
    private static final String ENABLE_2G_SUMMARY = "Avoids 2G networks, which are less secure."
            + " This may limit connectivity in some places. 2G is always allowed for emergency"
            + " calls, but calls may not connect if you're roaming internationally.";
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private Fragment mFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private RestrictedSwitchPreference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private Enable2gPreferenceController mController;
    private Context mContext;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private NotificationManager mNotificationManager;
    private PersistableBundle mCarrierConfig;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                mNotificationManager);
        when(mContext.getSystemService(NotificationManager.class)).thenReturn(mNotificationManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(
                mCarrierConfigManager);
        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);
        Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getString(R.string.enable_2g_summary)).thenReturn(ENABLE_2G_SUMMARY);
        when(mContext.getString(R.string.enable_2g_summary)).thenReturn(ENABLE_2G_SUMMARY);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        doReturn((long) RadioAccessFamily.getRafFromNetworkType(
                    TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA)) // 2G+3G+4G
                .when(mTelephonyManager)
                .getSupportedRadioAccessFamily();

        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(true).when(mInvalidTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        doReturn((long) RadioAccessFamily.getRafFromNetworkType(
                    TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA)) // 2G+3G+4G
                .when(mInvalidTelephonyManager)
                .getSupportedRadioAccessFamily();

        mController = new Enable2gPreferenceController(mContext, PREFERENCE_KEY);

        mPreference = spy(new RestrictedSwitchPreference(mContext));
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
        mController.init(mFragment, SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnavailable() {
        mController.init(mFragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_capabilityNotSupported_returnUnsupported() {
        doReturn(false).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_2gUnsupported_returnUnsupported() {
        doReturn((long) RadioAccessFamily.getRafFromNetworkType(
                    TelephonyManager.NETWORK_MODE_NR_LTE_WCDMA)) // 3G+4G+5G (No 2G)
                .when(mTelephonyManager)
                .getSupportedRadioAccessFamily();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                Enable2gPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        // Use defaults
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedTrue_returnFalse() {
        mController.init(mFragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedFalse_returnFalse() {
        mController.init(mFragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_KEY_CARRIER_2G_TOGGLE)
    public void setCheckedWithFalse_carrierDisabled2gNetwork() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_CARRIER_DEFAULT_2G_PROTECTION_ENABLED_BOOL, true);

        assertThat(mController.setChecked(false)).isTrue();
        verify(mPreference, never()).isDisabledByAdmin();
    }

    @Test
    @EnableFlags(Flags.FLAG_KEY_CARRIER_2G_TOGGLE)
    public void setCheckedWithTrue_carrierDisabled2gNetwork() {
        when2gIsDisabledByAdmin(false);
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_CARRIER_DEFAULT_2G_PROTECTION_ENABLED_BOOL, true);

        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_disabledByAdmin_returnFalse() {
        when2gIsDisabledByAdmin(true);

        assertThat(mController.setChecked(false)).isFalse();
        verify(mPreference).useAdminDisabledSummary(true);
        verify(mPreference).getRestrictedPreferenceHelper();
    }

    @Test
    public void setChecked_disable2G() {
        when2gIsEnabledForReasonEnable2g();

        // Disable 2G
        boolean changed = mController.setChecked(true);
        assertThat(changed).isEqualTo(true);

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                TelephonyManager.NETWORK_TYPE_BITMASK_LTE);
    }

    @Test
    public void disabledByAdmin_toggleUnchecked() {
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_invalidSubId_returnsFalse() {
        mController.init(mFragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        when2gIsDisabledByAdmin(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_corruptedNetworkType_resetsAndReturnsCorrectState() {
        // When allowed network types for 2G is corrupted (i.e. 0), it should be reset to the
        // default network types. The default includes 2G, so isChecked() should return false.
        when2gIsDisabledByAdmin(false);
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(0L);

        // Action
        boolean isChecked = mController.isChecked();

        // Verify that the allowed network types are reset to default.
        long defaultNetworkTypes = RadioAccessFamily.getRafFromNetworkType(
                RILConstants.PREFERRED_NETWORK_MODE);
        verify(mTelephonyManager).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G, defaultNetworkTypes);

        // The default network mode includes 2G, so 2G is enabled.
        // The "disable 2G" toggle should be OFF (isChecked() == false).
        assertThat(isChecked).isFalse();
    }

    @Test
    public void userRestrictionInactivated_userToggleMaintainsState() {
        // Initially, 2g is enabled
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isFalse();

        // When we disable the preference by an admin, the preference should be unchecked
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isTrue();

        // If the preference is re-enabled by an admin, former state should hold
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void updateState_isDisabledByAdmin() {
        when2gIsDisabledByAdmin(true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_preferenceIsNull() {
        when2gIsDisabledByAdmin(false);

        mController.updateState(null);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_notUsableSubscriptionId() {
        mController.init(mFragment, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        when2gIsDisabledByAdmin(false);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_withEnable2gSummary() {
        when2gIsDisabledByAdmin(false);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.enable_2g_summary));
    }

    @Test
    public void onDialogResult_positiveButtonEvent() {
        when2gIsDisabledByAdmin(false);
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(0L);
        Bundle bundle = new Bundle();
        bundle.putInt(Enable2gPreferenceController.REQUEST_KEY, DialogInterface.BUTTON_POSITIVE);
        mPreference.setChecked(true);

        mController.onDialogResult(bundle);

        assertFalse(mPreference.isChecked());
        verify(mNotificationManager).cancel(anyInt());
        verify(mTelephonyManager).setAllowedNetworkTypesForReason(anyInt(), anyLong());
    }
    @Test
    public void onDialogResult_NegariveButtonEvent() {
        mController.displayPreference(mPreferenceScreen);
        Bundle bundle = new Bundle();
        bundle.putInt(Enable2gPreferenceController.REQUEST_KEY, DialogInterface.BUTTON_NEGATIVE);
        mPreference.setChecked(false);

        mController.onDialogResult(bundle);

        assertTrue(mPreference.isChecked());
    }

    @Test
    public void updateState_simNameAsSummary() {
        when2gIsDisabledByAdmin(false);
        String simName = "SIM1";
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo.Builder()
                .setId(SUB_ID)
                .setDisplayName(simName)
                .build();
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(subscriptionInfo);
        when(mSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(subInfos);
        mController.init(mFragment, SUB_ID, true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(simName);
    }

    @Test
    public void updateState_simNameAsSummary_2gPreferenceDisableByAdmin() {
        when2gIsDisabledByAdmin(true);
        String simName = "SIM1";
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo.Builder()
                .setId(SUB_ID)
                .setDisplayName(simName)
                .build();
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(subscriptionInfo);
        when(mSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(subInfos);
        mController.init(mFragment, SUB_ID, true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(simName);
    }

    @Test
    public void updateState_preferenceEnable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState((Preference) mPreference);

        assertTrue(mPreference.isEnabled());
    }

    @Test
    public void updateState_isAirplaneModeOn_preferenceDisable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
        mController.notifyAirplaneModeChanged(true);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    @Test
    public void updateState_preferenceDisable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_GSM_ONLY);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    @Test
    public void updateState_isDisabledByAdmin_networkTypeLte_preferenceDisable() {
        when2gIsDisabledByAdmin(true);
        mPreference.setEnabled(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    private void mockAllowedNetworkTypes(long allowedNetworkType) {
        doReturn(allowedNetworkType).when(mTelephonyManager).getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
    }

    private void when2gIsEnabledForReasonEnable2g() {
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                        | TelephonyManager.NETWORK_TYPE_BITMASK_LTE));
    }

    private void when2gIsDisabledByAdmin(boolean is2gDisabledByAdmin) {
        // Our controller depends on state being initialized when the associated preference is
        // displayed because the admin disablement functionality flows from the association of a
        // Preference with the PreferenceScreen
        when(mPreference.isDisabledByAdmin()).thenReturn(is2gDisabledByAdmin);
        mController.displayPreference(mPreferenceScreen);
    }
}
