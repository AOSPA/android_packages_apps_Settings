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

package com.android.settings.security;

import static android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_V2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.EnforcingAdmin;
import android.app.admin.UnknownAuthority;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDevicePolicyManager.class})
public class ContentProtectionWorkSwitchControllerTest {

    private static final UserHandle TEST_USER_HANDLE = UserHandle.of(10);

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private PreferenceScreen mMockPreferenceScreen;

    @Mock private RestrictedSwitchPreference mMockSwitchPreference;

    @Nullable private UserHandle mManagedProfileUserHandle;

    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    @Nullable private EnforcingAdmin mEnforcingAdmin = new EnforcingAdmin(
                    "test.pkg",
                    UnknownAuthority.UNKNOWN_AUTHORITY,
                    UserHandle.of(UserHandle.myUserId()),
                    new ComponentName("", ""));

    private ShadowDevicePolicyManager mShadowDevicePolicyManager;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    private TestContentProtectionWorkSwitchController mController;

    @Before
    public void setUp() {
        mShadowDevicePolicyManager = ShadowDevicePolicyManager.getShadow();
        mController = new TestContentProtectionWorkSwitchController();
    }

    @Test
    public void constructor_fetchesManagedProfile() {
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(0);
    }

    @Test
    public void constructor_withManagedProfile_fetchesPolicy() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(1);
    }

    @Test
    public void getAvailabilityStatus_managedProfile_policyDisabled_available() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_managedProfile_policyEnabled_available() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_managedProfile_policyNotControlled_unavailable() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_noManagedProfile_unavailable() {
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_policyEnabled_true() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_policyDisabled_false() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_policyNotControlled_false() {
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionWorkSwitchController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_alwaysFalse() {
        assertThat(mController.setChecked(true)).isFalse();
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void displayPreference_managedProfile_disabledByEnforcedAdmin() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
    }

    @Test
    public void displayPreference_managedProfile_disabledByEnforcingAdmin() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mManagedProfileUserHandle = TEST_USER_HANDLE;
        setupForDisplayPreferenceWithEnforcingAdmin(true);

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcingAdmin);
    }

    @Test
    public void displayPreference_noManagedProfile_notDisabledByEnforcedAdmin() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                (RestrictedLockUtils.EnforcedAdmin) any());
        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(0);
    }

    @Test
    public void displayPreference_noManagedProfile_notDisabledByEnforcingAdmin() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        setupForDisplayPreferenceWithEnforcingAdmin(false);

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(EnforcingAdmin.class));
    }

    private void setupForDisplayPreference() {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController = new TestContentProtectionWorkSwitchController();
    }

    private void setUpEnforcingAdmin() {
        mShadowDevicePolicyManager.setPolicyEnforcementInfoForPolicy(
                DevicePolicyIdentifiers.CONTENT_PROTECTION_POLICY,
                new PolicyEnforcementInfo(List.of(mEnforcingAdmin)));
        mController = new TestContentProtectionWorkSwitchController();
    }

    private void setupForDisplayPreferenceWithEnforcingAdmin(boolean isSetUpByEnforcingAdmin) {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        if (isSetUpByEnforcingAdmin) {
            setUpEnforcingAdmin();
        }
    }

    private class TestContentProtectionWorkSwitchController
            extends ContentProtectionWorkSwitchController {

        public int mCounterGetManagedProfile;

        public int mCounterGetEnforcedAdmin;

        public int mCounterGetContentProtectionPolicy;

        TestContentProtectionWorkSwitchController() {
            super(ContentProtectionWorkSwitchControllerTest.this.mContext, "key");
        }

        @Override
        @Nullable
        protected UserHandle getManagedProfile() {
            mCounterGetManagedProfile++;
            return mManagedProfileUserHandle;
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(
                @NonNull UserHandle userHandle) {
            mCounterGetEnforcedAdmin++;
            return mEnforcedAdmin;
        }

        @Override
        @DevicePolicyManager.ContentProtectionPolicy
        protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
            mCounterGetContentProtectionPolicy++;
            return mContentProtectionPolicy;
        }
    }
}
