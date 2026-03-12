/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.shadow.ShadowThreadUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.shadows.ShadowSubscriptionManager;

@RunWith(AndroidJUnit4.class)
@Config(
        shadows = {
            ShadowThreadUtils.class,
            ShadowSubscriptionManager.class,
        })
public class TestingSettingsTest {

    private Context mContext;
    private TestingSettings mFragment;

    @Mock private UserManager mUserManager;
    @Mock private TelephonyManager mTelephonyManager;
    @Mock private CarrierConfigManager mCarrierConfigManager;
    @Mock private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.getApplication());
        mFragment = spy(new TestingSettings());

        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void isRadioInfoVisible_notAdmin_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mFragment.isRadioInfoVisible(mContext)).isFalse();
    }

    @Test
    public void isRadioInfoVisible_mobileNetworkRestricted_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
                .thenReturn(true);

        assertThat(mFragment.isRadioInfoVisible(mContext)).isFalse();
    }

    @Test
    public void isRadioInfoVisible_adminAndNotRestricted_shouldReturnTrue() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
                .thenReturn(false);
        ShadowBuild.setType("userdebug");

        assertThat(mFragment.isRadioInfoVisible(mContext)).isTrue();
    }

    @Test
    public void isRadioInfoVisible_carrierConfigRestricted_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
                .thenReturn(false);
        ShadowBuild.setType("user");

        when(mTelephonyManager.getActiveModemCount()).thenReturn(1);

        SubscriptionInfo subInfo =
                ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
                        .setId(1)
                        .setSimSlotIndex(0)
                        .buildSubscriptionInfo();

        ShadowSubscriptionManager shadowSubMgr =
                org.robolectric.Shadows.shadowOf(
                        mContext.getSystemService(SubscriptionManager.class));
        shadowSubMgr.setActiveSubscriptionInfos(subInfo);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_RADIO_INFO_ON_USER_BUILD_BOOL, true);
        when(mCarrierConfigManager.getConfigForSubId(1)).thenReturn(bundle);

        assertThat(mFragment.isRadioInfoVisible(mContext)).isFalse();
    }

    @Test
    public void removePhoneInfoOptionsFromHiddenMenu_shouldRemovePreferences() {
        Preference radioInfoPref = mock(Preference.class);
        Preference phoneInfoPref = mock(Preference.class);
        doReturn(radioInfoPref).when(mFragment).findPreference("radio_info_settings");
        doReturn(phoneInfoPref).when(mFragment).findPreference("phone_information_v2");

        mFragment.removePhoneInfoOptionsFromHiddenMenu();

        verify(mPreferenceScreen).removePreference(radioInfoPref);
        verify(mPreferenceScreen).removePreference(phoneInfoPref);
    }
}
