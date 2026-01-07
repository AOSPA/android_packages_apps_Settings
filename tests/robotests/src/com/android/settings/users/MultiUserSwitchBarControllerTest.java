/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN;
import static com.android.settings.testutils.DevicePolicyUtils.SYSTEM_ADMIN;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class MultiUserSwitchBarControllerTest {

    private Context mContext;
    private ShadowUserManager mUserManager;
    private SwitchWidgetController mSwitchWidgetController;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final SetFlagsRule mSetFlagsRule =  new SetFlagsRule();

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mUserManager = ShadowUserManager.getShadow();
        mSwitchWidgetController = mock(SwitchWidgetController.class);
        mUserManager.setSupportsMultipleUsers(true);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void onStart_disallowUserSwitchEnforcedByAdmin_shouldSetDisabledByAdminUnchecked() {
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                UserManager.DISALLOW_USER_SWITCH, new PolicyEnforcementInfo(List.of(DPC_ADMIN)));

        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);
        verify(mSwitchWidgetController).setChecked(false);
        verify(mSwitchWidgetController).setDisabledByAdmin(any(EnforcingAdmin.class));
    }

    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void onStart_disallowUserSwitchEnforcedByAdmin_shouldSetDisabledByAdminUnchecked_refactorDisabled() {
        int userId = UserHandle.myUserId();
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_USER_SWITCH,
                UserHandle.of(userId),
                enforcingUsers);

        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);
        verify(mSwitchWidgetController).setChecked(false);
        verify(mSwitchWidgetController).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void onStart_disallowUserSwitch_userNotMain_shouldSetDisabledUnchecked() {
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                UserManager.DISALLOW_USER_SWITCH, new PolicyEnforcementInfo(List.of(SYSTEM_ADMIN)));
        mUserManager.addUser(10, "Test", UserInfo.FLAG_ADMIN);
        mUserManager.switchUser(10);

        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);

        verify(mSwitchWidgetController).setChecked(false);
        verify(mSwitchWidgetController).setEnabled(false);
        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any(EnforcingAdmin.class));
    }

    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void onStart_disallowUserSwitch_userNotMain_shouldSetDisabledUnchecked_refactorDisabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, true);
        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);

        verify(mSwitchWidgetController).setChecked(false);
        verify(mSwitchWidgetController).setEnabled(false);
        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any(
                RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void onStart_allowUserSwitch_notMainUser_shouldSetDisabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.addUser(10, "Test", UserInfo.FLAG_ADMIN);
        mUserManager.switchUser(10);
        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);

        verify(mSwitchWidgetController).setEnabled(false);
    }

    @Test
    public void onStart_allowUserSwitch_shouldNotSetDisabledByAdmin() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);

        final MultiUserSwitchBarController controller = new MultiUserSwitchBarController(mContext,
                mSwitchWidgetController, null);

        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any(
                RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void onStart_userIsNotMain_shouldNotBeEnabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.addUser(10, "Test", UserInfo.FLAG_ADMIN);
        mUserManager.switchUser(10);
        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);

        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any(
                RestrictedLockUtils.EnforcedAdmin.class));
        verify(mSwitchWidgetController).setEnabled(false);
    }

    @Test
    public void onStart_userIsMain_shouldBeEnabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        new MultiUserSwitchBarController(mContext, mSwitchWidgetController, null);

        verify(mSwitchWidgetController, never()).setDisabledByAdmin(any(
                RestrictedLockUtils.EnforcedAdmin.class));
        verify(mSwitchWidgetController).setEnabled(true);
    }
}
