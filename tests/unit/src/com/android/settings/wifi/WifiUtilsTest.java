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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.TetheringManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.os.UserManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiUtilsTest {

    static final int USER_ID_CURRENT = Process.myUserHandle().getIdentifier();
    static final int USER_ID_OTHER = USER_ID_CURRENT + 1;
    static final String[] WIFI_REGEXS = {"wifi_regexs"};

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    Resources mResources;
    @Mock
    WifiManager mWifiManager;
    @Mock
    UserManager mUserManager;
    @Mock
    TetheringManager mTetheringManager;
    @Mock
    WifiEntry mWifiEntry;
    @Mock
    WifiConfiguration mWifiConfiguration;

    @Before
    public void setUp() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_wifi_hotspot_settings)).thenReturn(true);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getSystemService(TetheringManager.class)).thenReturn(mTetheringManager);
        when(mTetheringManager.getTetherableWifiRegexs()).thenReturn(WIFI_REGEXS);
        when(mWifiEntry.getWifiConfiguration()).thenReturn(mWifiConfiguration);
    }

    @Test
    public void testSSID() {
        assertThat(WifiUtils.isSSIDTooLong("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooLong("☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎")).isTrue();

        assertThat(WifiUtils.isSSIDTooShort("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooShort("")).isTrue();
    }

    @Test
    public void testPassword() {
        final String longPassword = "123456789012345678901234567890"
                + "1234567890123456789012345678901234567890";
        assertThat(WifiUtils.isHotspotPasswordValid("123",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("12345678",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("1234567890",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid(longPassword,
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("€¥£",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)).isFalse();

        // The WPA3_SAE_TRANSITION password limitation should be same as WPA2_PSK
        assertThat(WifiUtils.isHotspotPasswordValid("123",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("12345678",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("1234567890",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid(longPassword,
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("€¥£",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION)).isFalse();

        // The WA3_SAE password is requested that length > 1 only.
        assertThat(WifiUtils.isHotspotPasswordValid("",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isFalse();
        assertThat(WifiUtils.isHotspotPasswordValid("1",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("123",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("12345678",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("1234567890",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid(longPassword,
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
        assertThat(WifiUtils.isHotspotPasswordValid("€¥£",
                SoftApConfiguration.SECURITY_TYPE_WPA3_SAE)).isTrue();
    }

    @Test
    public void getWifiConfigByWifiEntry_shouldReturnCorrectConfig() {
        final String testSSID = "WifiUtilsTest";
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getSsid()).thenReturn(testSSID);

        final WifiConfiguration config = WifiUtils.getWifiConfig(wifiEntry, null /* scanResult */);

        assertThat(config).isNotNull();
        assertThat(config.SSID).isEqualTo("\"" + testSSID + "\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWifiConfigWithNullInput_ThrowIllegalArgumentException() {
        WifiConfiguration config = WifiUtils.getWifiConfig(null /* wifiEntry */,
                null /* scanResult */);
    }

    @Ignore
    @Test
    public void checkShowWifiHotspot_allReady_returnTrue() {
        assertThat(WifiUtils.checkShowWifiHotspot(mContext)).isTrue();
    }

    @Test
    public void checkShowWifiHotspot_contextIsNull_returnFalse() {
        assertThat(WifiUtils.checkShowWifiHotspot(null)).isFalse();
    }

    @Test
    public void checkShowWifiHotspot_configIsNotShow_returnFalse() {
        when(mResources.getBoolean(R.bool.config_show_wifi_hotspot_settings)).thenReturn(false);

        assertThat(WifiUtils.checkShowWifiHotspot(mContext)).isFalse();
    }

    @Test
    public void checkShowWifiHotspot_wifiManagerIsNull_returnFalse() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(null);

        assertThat(WifiUtils.checkShowWifiHotspot(mContext)).isFalse();
    }

    @Test
    public void checkShowWifiHotspot_tetheringManagerIsNull_returnFalse() {
        when(mContext.getSystemService(TetheringManager.class)).thenReturn(null);

        assertThat(WifiUtils.checkShowWifiHotspot(mContext)).isFalse();
    }

    @Test
    public void checkShowWifiHotspot_wifiRegexsIsEmpty_returnFalse() {
        when(mTetheringManager.getTetherableWifiRegexs()).thenReturn(null);

        assertThat(WifiUtils.checkShowWifiHotspot(mContext)).isFalse();
    }

    @Test
    public void canShowWifiHotspot_cachedIsReady_returnCached() {
        WifiUtils.setCanShowWifiHotspotCached(true);

        assertThat(WifiUtils.canShowWifiHotspot(null)).isTrue();

        WifiUtils.setCanShowWifiHotspotCached(false);

        assertThat(WifiUtils.canShowWifiHotspot(null)).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isCurrentUserNetworkOwner_multiUserDisabled() {
        assertThat(WifiUtils.isCurrentUserNetworkOwner(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isCurrentUserNetworkOwner_nullWifiConfiguration() {
        when(mWifiEntry.getWifiConfiguration()).thenReturn(null);

        assertThat(WifiUtils.isCurrentUserNetworkOwner(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isCurrentUserNetworkOwner_singleUser() {
        when(mUserManager.getUserCount()).thenReturn(1);

        assertThat(WifiUtils.isCurrentUserNetworkOwner(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isCurrentUserNetworkOwner_multipleUsers_notOwnedNetwork() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isCurrentUserNetworkOwner(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isCurrentUserNetworkOwner_multipleUsers_ownedNetwork() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isCurrentUserNetworkOwner(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsDisabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkEditable_multiUserDisabled() {
        assertThat(WifiUtils.isNetworkEditable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkEditable_ownedNetwork() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);
        when(mWifiEntry.isModifiableByOtherUsers()).thenReturn(false);

        assertThat(WifiUtils.isNetworkEditable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkEditable_notOwnedNetwork_networkModifiable() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);
        when(mWifiEntry.isModifiableByOtherUsers()).thenReturn(true);

        assertThat(WifiUtils.isNetworkEditable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkEditable_notOwnedNetwork_networkNotModifiable() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);
        when(mWifiEntry.isModifiableByOtherUsers()).thenReturn(false);

        assertThat(WifiUtils.isNetworkEditable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkEditable_guestUser_notOwnedNetwork_networkModifiable() {
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);
        when(mWifiEntry.isModifiableByOtherUsers()).thenReturn(true);
        when(mUserManager.isGuestUser()).thenReturn(true);

        assertThat(WifiUtils.isNetworkEditable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isSharedFieldEditable_multiuserDisabled() {
        assertThat(WifiUtils.isSharedFieldEditable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    public void isSharedFieldEditable_guestUser() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        assertThat(WifiUtils.isSharedFieldEditable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isSharedFieldEditable_newNetwork() {
        assertThat(WifiUtils.isSharedFieldEditable(null, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isSharedFieldEditable_ownedNetwork() {
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isSharedFieldEditable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isSharedFieldEditable_notOwnedNetwork() {
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isSharedFieldEditable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkShareable_canShare_multiuserDisabled() {
        when(mWifiEntry.canShare()).thenReturn(true);

        assertThat(WifiUtils.isNetworkShareable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    public void isNetworkShareable_cannotShare() {
        when(mWifiEntry.canShare()).thenReturn(false);

        assertThat(WifiUtils.isNetworkShareable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkShareable_canShare_notOwnedNetwork() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isNetworkShareable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkShareable_canShare_ownedNetwork() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isNetworkShareable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkShareable_canShare_ownedNetwork_guestUser() {
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mUserManager.isGuestUser()).thenReturn(true);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isNetworkShareable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkForgettable_canForget_multiuserDisabled() {
        when(mWifiEntry.canForget()).thenReturn(true);

        assertThat(WifiUtils.isNetworkForgettable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    public void isNetworkForgettable_cannotForget() {
        when(mWifiEntry.canForget()).thenReturn(false);

        assertThat(WifiUtils.isNetworkForgettable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkForgettable_canForget_ownedNetwork() {
        when(mWifiEntry.canForget()).thenReturn(true);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_CURRENT);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isNetworkForgettable(mWifiEntry, mContext)).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkForgettable_canForget_notOwnedNetwork() {
        when(mWifiEntry.canForget()).thenReturn(true);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);

        assertThat(WifiUtils.isNetworkForgettable(mWifiEntry, mContext)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void isNetworkForgettable_canForget_notOwnedNetwork_adminUser() {
        when(mWifiEntry.canForget()).thenReturn(true);
        when(mWifiConfiguration.getCreatorUserId()).thenReturn(USER_ID_OTHER);
        when(mUserManager.getUserCount()).thenReturn(2);
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(WifiUtils.isNetworkForgettable(mWifiEntry, mContext)).isTrue();
    }
}
