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
package com.android.settings.users;

import static android.os.Flags.FLAG_DISALLOW_ADD_GUEST;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, SettingsShadowResources.class})
public class AddGuestFromSignInPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private ShadowUserManager mUserManager;
    private AddGuestFromSignInPreferenceController mController;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScreen = mock(PreferenceScreen.class);
        mUserManager = ShadowUserManager.getShadow();
        mController = new AddGuestFromSignInPreferenceController(mContext, "fake_key");
        mUserManager.setSupportsMultipleUsers(true);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen, true);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
        SettingsShadowResources.reset();
    }

    @Test
    @EnableFlags(FLAG_DISALLOW_ADD_GUEST)
    public void displayPref_admin_shouldDisplayPreference() {
        mUserManager.setIsAdminUser(true);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);

        mController.updateState(preference);
        mController.displayPreference(mScreen);

        verify(preference).setVisible(true);
    }

    @Test
    @EnableFlags(FLAG_DISALLOW_ADD_GUEST)
    public void displayPref_notAdmin_shouldNotDisplayPreference() {
        mUserManager.setIsAdminUser(false);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);

        mController.updateState(preference);
        mController.displayPreference(mScreen);

        verify(preference).setVisible(false);
    }

    @Test
    @EnableFlags(FLAG_DISALLOW_ADD_GUEST)
    public void displayPref_admin_noLoginScreen_shouldNotDisplayPreference() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen, false);
        mUserManager.setIsAdminUser(false);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);

        mController.updateState(preference);
        mController.displayPreference(mScreen);

        verify(preference).setVisible(false);
    }

    @Test
    @DisableFlags(FLAG_DISALLOW_ADD_GUEST)
    public void displayPref_admin_flagDisabled_shouldNotDisplayPreference() {
        mUserManager.setIsAdminUser(true);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);

        mController.updateState(preference);
        mController.displayPreference(mScreen);

        verify(preference).setVisible(false);
    }

    @Test
    public void updateState_preferenceSetCheckedWithNoGuestRestriction() {
        mUserManager.setUserRestriction(UserHandle.SYSTEM, UserManager.DISALLOW_ADD_GUEST, false);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWithGuestRestriction() {
        mUserManager.setUserRestriction(UserHandle.SYSTEM, UserManager.DISALLOW_ADD_GUEST, true);
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);

        mController.updateState(preference);

        verify(preference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_noGuestRestrictionWhenPreferenceChecked() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        mController.onPreferenceChange(preference, true);

        final UserManager userManager = mContext.getSystemService(UserManager.class);
        assertThat(
            userManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_ADD_GUEST, UserHandle.SYSTEM)).isFalse();
    }

    @Test
    public void onPreferenceChange_hasGuestRestrictionWhenPreferenceNotChecked() {
        final RestrictedSwitchPreference preference = mock(RestrictedSwitchPreference.class);
        mController.onPreferenceChange(preference, false);

        final UserManager userManager = mContext.getSystemService(UserManager.class);
        assertThat(
            userManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_ADD_GUEST, UserHandle.SYSTEM)).isTrue();
    }
}
