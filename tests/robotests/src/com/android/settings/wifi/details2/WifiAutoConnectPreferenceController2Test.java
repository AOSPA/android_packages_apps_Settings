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

package com.android.settings.wifi.details2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Process;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.security.advancedprotection.AdvancedProtectionManager;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.flags.Flags;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiAutoConnectPreferenceController2Test {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int USER_ID_CURRENT = Process.myUserHandle().getIdentifier();
    private static final int USER_ID_OTHER = USER_ID_CURRENT + 1;

    private WifiAutoConnectPreferenceController2 mController;

    private Context mContext;

    @Mock
    private UserManager mUserManager;
    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private WifiConfiguration mWifiConfiguration;
    @Mock
    private PreferenceScreen mScreen;

    @Mock
    private SwitchPreferenceCompat mPreference;

    @Mock
    private AdvancedProtectionManager mAdvancedProtectionManager;
    @Mock
    private Fragment mFragment;
    @Mock
    private WifiAutoConnectPreferenceController2.PreferenceRefreshCallback mCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getSystemService(AdvancedProtectionManager.class))
                .thenReturn(mAdvancedProtectionManager);

        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);

        WifiAutoConnectPreferenceController2 realController =
                new WifiAutoConnectPreferenceController2(mContext);

        mController = spy(realController);

        mController.setWifiEntry(mWifiEntry);
        mController.setParentFragment(mFragment);
        mController.setRefreshCallback(mCallback);
    }

    @Test
    public void getAvailabilityStatus_shouldFollowCanSetAutoJoinEnabled() {
        // Test able to set auto join.
        when(mWifiEntry.canSetAutoJoinEnabled()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Test not able to set auto join.
        when(mWifiEntry.canSetAutoJoinEnabled()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_shouldFollowIsAutoJoinEnabled() {
        // Test auto join enabled.
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();

        // Test auto join disabled.
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_shouldSetAutoJoinEnabled() {
        // Test checked.
        mController.setChecked(true);

        verify(mWifiEntry).setAutoJoinEnabled(true);

        // Test unchecked.
        mController.setChecked(false);

        verify(mWifiEntry).setAutoJoinEnabled(false);
    }

    @Test
    @DisableFlags({com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER,
            com.android.settings.connectivity.Flags.FLAG_WIFI_MULTIUSER})
    public void displayPreference_FlagDisabled() {
        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void displayPreference_NetworkOwned() {
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);

        // Stub getCreatorUserId to return the CURRENT user
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void displayPreference_NetworkNotOwned() {
        when(mUserManager.getUserCount()).thenReturn(3);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);

        // Stub getCreatorUserId to return a DIFFERENT user
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void displayPreference_NetworkNotOwned_SingleUser() {
        when(mUserManager.getUserCount()).thenReturn(1);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);

        // Stub getCreatorUserId to return a DIFFERENT user
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void onPreferenceClick_aapmActiveInsecureNetwork_showsDialogAndDoesNotToggle() {
        when(mAdvancedProtectionManager.isAdvancedProtectionEnabled()).thenReturn(true);
        when(mWifiConfiguration.isAutoJoinInAdvancedProtectionModeEnabled()).thenReturn(false);
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(false);

        doReturn(new Intent("mock.action"))
                .when(mController).getAapmSupportIntent();

        boolean handled = mController.onPreferenceClick(mPreference);

        assertThat(handled).isTrue();
        verify(mFragment).startActivityForResult(any(Intent.class),
                eq(WifiAutoConnectPreferenceController2.REQUEST_CODE_AUTOCONNECT_OVERRIDE));

        verify(mWifiEntry, never()).setAutoJoinEnabled(true);
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void handleDialogResult_resultCanceled_remainsOff() {
        mController.handleDialogResult(
                WifiAutoConnectPreferenceController2.REQUEST_CODE_AUTOCONNECT_OVERRIDE,
                Activity.RESULT_CANCELED);

        verify(mWifiConfiguration, never()).setAutoJoinInAdvancedProtectionModeEnabled(true);
        verify(mWifiEntry, never()).setAutoJoinEnabled(true);
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void handleDialogResult_resultOk_turnsOnAndSavesOverride() {
        when(mAdvancedProtectionManager.isAdvancedProtectionEnabled()).thenReturn(true);
        when(mWifiConfiguration.isAutoJoinInAdvancedProtectionModeEnabled()).thenReturn(false);
        mController.updateState(mPreference);

        mController.handleDialogResult(
                WifiAutoConnectPreferenceController2.REQUEST_CODE_AUTOCONNECT_OVERRIDE,
                Activity.RESULT_OK);

        verify(mWifiConfiguration).setAutoJoinInAdvancedProtectionModeEnabled(true);

        verify(mWifiEntry).setAutoJoinEnabled(true);

        verify(mCallback).onPreferenceStateChange(mPreference);
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void onPreferenceClick_aapmActive_overrideAlreadyOn_turnsOffImmediately() {
        // AAPM is active and we are on an insecure network
        when(mAdvancedProtectionManager.isAdvancedProtectionEnabled()).thenReturn(true);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);

        // The override is currently enabled (User previously selected "Connect anyway")
        when(mWifiConfiguration.isAutoJoinInAdvancedProtectionModeEnabled()).thenReturn(true);

        // Preference is clicked
        boolean handled = mController.onPreferenceClick(mPreference);
        assertThat(handled).isTrue();

        // Should NOT show the dialog
        verify(mFragment, never()).startActivityForResult(any(Intent.class), anyInt());

        // Disable the override and save
        verify(mWifiConfiguration).setAutoJoinInAdvancedProtectionModeEnabled(false);
        verify(mWifiEntry).setAutoJoinEnabled(false);
        verify(mCallback).onPreferenceStateChange(any());
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void isChecked_aapmActive_readsFromConfigOverride() {
        // AAPM is active on insecure network
        when(mAdvancedProtectionManager.isAdvancedProtectionEnabled()).thenReturn(true);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);

        // Config override says true (user chose to connect)
        when(mWifiEntry.isAutoJoinEnabled()).thenReturn(false);
        when(mWifiConfiguration.isAutoJoinInAdvancedProtectionModeEnabled()).thenReturn(true);

        // isChecked should return true (reflecting the override)
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    @EnableFlags(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
    public void onPreferenceClick_aapmActive_secureNetwork_standardBehavior() {
        // AAPM is active on Secure network
        when(mAdvancedProtectionManager.isAdvancedProtectionEnabled()).thenReturn(true);
        when(mWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);

        // Preference is clicked
        boolean handled = mController.onPreferenceClick(mPreference);

        // Return false (allowing standard TogglePreferenceController behavior)
        assertThat(handled).isFalse();

        // AAPM configuration changes should not occur
        verify(mWifiConfiguration, never())
                .setAutoJoinInAdvancedProtectionModeEnabled(anyBoolean());
    }
}
