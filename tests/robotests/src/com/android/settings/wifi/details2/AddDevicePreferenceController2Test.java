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

package com.android.settings.wifi.details2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Process;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowWifiDppUtils;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowWifiDppUtils.class})
public class AddDevicePreferenceController2Test {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final int USER_ID_CURRENT = Process.myUserHandle().getIdentifier();
    private static final int USER_ID_OTHER = USER_ID_CURRENT + 1;

    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private WifiConfiguration mWifiConfiguration;
    @Mock
    private Preference mPreference;
    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private AddDevicePreferenceController2 mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mController = new AddDevicePreferenceController2(mContext);
        mController.setWifiEntry(mWifiEntry);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    @After
    public void tearDown() {
        ShadowWifiDppUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_canEasyConnect_shouldReturnAvailable() {
        when(mWifiEntry.canEasyConnect()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_cannotEasyConnect_shouldReturnConditionallyUnavailable() {
        when(mWifiEntry.canEasyConnect()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_shouldInvokeShowLockScreen() {
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ShadowWifiDppUtils.wasShowLockScreenCalled()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_keyDoesNotMatch_shouldReturnFalse() {
        when(mPreference.getKey()).thenReturn("different_key");

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(ShadowWifiDppUtils.wasShowLockScreenCalled()).isFalse();
    }

    @Test
    @DisableFlags({com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER,
            com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER})
    public void updateState_FlagDisabled() {
        when(mWifiEntry.canShare()).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void updateState_NetworkNotShareable() {
        when(mWifiEntry.canShare()).thenReturn(false);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void updateState_NetworkNotOwned_SingleUser() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.getUserCount()).thenReturn(1);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void updateState_NetworkOwned() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void updateState_NetworkNotOwned() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void updateState_GuestUser() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mUserManager.isGuestUser()).thenReturn(true);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }
}
