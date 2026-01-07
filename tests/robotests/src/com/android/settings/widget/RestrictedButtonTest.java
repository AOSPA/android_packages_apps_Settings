/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.widget;

import static com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.admin.PolicyEnforcementInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View.OnClickListener;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class RestrictedButtonTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();


    private RestrictedButton mButton;
    private OnClickListener mOnClickListener;
    private UserHandle mUser;

    @Before
    public void setUp() {
        mButton = new RestrictedButton(Robolectric.setupActivity(FragmentActivity.class));
        mOnClickListener = mock(OnClickListener.class);
        mButton.setOnClickListener(mOnClickListener);

        int userId = UserHandle.myUserId();
        mUser = UserHandle.of(userId);
    }

    @Test
    public void performClick_buttonIsNotInited_shouldCallListener() {
        mButton.performClick();

        verify(mOnClickListener).onClick(eq(mButton));
    }

    @Test
    public void performClick_noRestriction_shouldCallListener() {
        mButton.init(mUser, UserManager.DISALLOW_ADJUST_VOLUME);

        mButton.performClick();

        verify(mOnClickListener).onClick(eq(mButton));
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void performClick_hasRestriction_shouldNotCallListener() {
        mButton.init(mUser, UserManager.DISALLOW_MODIFY_ACCOUNTS);
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                UserManager.DISALLOW_MODIFY_ACCOUNTS,
                new PolicyEnforcementInfo(List.of(DPC_ADMIN)));
        mButton.performClick();

        verify(mOnClickListener, never()).onClick(eq(mButton));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void performClick_hasRestriction_shouldNotCallListener_refactorDisabled() {
        // Setup from previous setUp()
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(UserHandle.myUserId(),
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_MODIFY_ACCOUNTS,
                mUser,
                enforcingUsers);

        mButton.init(mUser, UserManager.DISALLOW_MODIFY_ACCOUNTS);

        mButton.performClick();

        verify(mOnClickListener, never()).onClick(eq(mButton));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void updateState_noRestriction_shoulddisableButton_refactorDisabled() {
        // Setup from previous setUp()
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(UserHandle.myUserId(),
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_MODIFY_ACCOUNTS,
                mUser,
                enforcingUsers);

        mButton.init(mUser, UserManager.DISALLOW_MODIFY_ACCOUNTS);

        mButton.updateState();

        assertThat(mButton.isEnabled()).isFalse();
    }

    @Test
    public void updateState_noRestriction_shouldEnableButton() {
        mButton.init(mUser, UserManager.DISALLOW_ADJUST_VOLUME);

        mButton.updateState();

        assertThat(mButton.isEnabled()).isTrue();
    }
}
