/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.premiumsms;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PremiumSmsControllerTest {
    private static final int TEST_SUB_ID_1 = 1;
    private static final int TEST_SUB_ID_2 = 2;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mTelephonyManager2;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private Context mContext;
    private Resources mResources;
    private PremiumSmsController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mTelephonyManager.createForSubscriptionId(TEST_SUB_ID_1)).thenReturn(
                mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(TEST_SUB_ID_2)).thenReturn(
                mTelephonyManager2);
        when(mSubscriptionManager.getActiveSubscriptionIdList(true)).thenReturn(
                new int[]{TEST_SUB_ID_1});
        when(mTelephonyManager.getSimCountryIso()).thenReturn("us");
        mController = new PremiumSmsController(mContext, "key");
    }

    @Test
    public void getAvailability_byDefault_shouldBeShown() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailability_disabledByCarrier_returnUnavailable() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailability_disabled_returnUnavailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_simIsJpOnly_returnUnavailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        when(mTelephonyManager.getSimCountryIso()).thenReturn("jp");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailability_dualSimsButOnly1simIsJp_returnAvailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        when(mSubscriptionManager.getActiveSubscriptionIdList(true)).thenReturn(
                new int[]{TEST_SUB_ID_1, TEST_SUB_ID_2});
        when(mTelephonyManager.getSimCountryIso()).thenReturn("jp");
        when(mTelephonyManager2.getSimCountryIso()).thenReturn("us");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_dualSimsAndNoJpSim_returnAvailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        when(mSubscriptionManager.getActiveSubscriptionIdList(true)).thenReturn(
                new int[]{TEST_SUB_ID_1, TEST_SUB_ID_2});
        when(mTelephonyManager.getSimCountryIso()).thenReturn("tw");
        when(mTelephonyManager2.getSimCountryIso()).thenReturn("us");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_noSims_returnAvailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        when(mSubscriptionManager.getActiveSubscriptionIdList(true)).thenReturn(
                new int[]{});

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
