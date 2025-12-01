/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.admin.EnforcingAdmin;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.UnknownAuthority;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.List;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LockUnificationPreferenceControllerTest {

    private static final int FAKE_PROFILE_USER_ID = 1234;
    private static final int MY_USER_ID = UserHandle.myUserId();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUm;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SecuritySettings mHost;
    @Mock private RestrictedSwitchPreference mMockRestrictedSwitchPreference;

    private Context mContext;
    private LockUnificationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.USER_SERVICE, mUm);

        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {FAKE_PROFILE_USER_ID});
        when(mUm.isManagedProfile(FAKE_PROFILE_USER_ID)).thenReturn(true);

        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
    }

    private void init() {
        mController = new LockUnificationPreferenceController(mContext, mHost);
        RestrictedSwitchPreference switchPreference = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(switchPreference);
    }

    @Test
    public void isAvailable_noProfile_false() {
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[0]);
        init();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void getPreferenceKey_byDefault_returnsDefaultValue_enforcedAdmin() {
        init();

        assertThat(mController.getPreferenceKey()).isEqualTo("unification");
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void getPreferenceKey_byDefault_returnsDefaultValue_enforcingAdmin() {
        init();

        assertThat(mController.getPreferenceKey()).isEqualTo("unification");
    }

    @Test
    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void getPreferenceKey_whenGivenValue_returnsGivenValue() {
        mController = new LockUnificationPreferenceController(mContext, mHost, "key");

        assertThat(mController.getPreferenceKey()).isEqualTo("key");
    }

    @Test
    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void updateState_disallowedByAdmin_preferenceIsDisabled() {
        setupForUpdateState();

        mController.updateState(mMockRestrictedSwitchPreference);

        doNothing().when(mMockRestrictedSwitchPreference)
                .checkRestrictionAndSetDisabled(
                        UserManager.DISALLOW_UNIFIED_PASSWORD, FAKE_PROFILE_USER_ID);

        verify(mMockRestrictedSwitchPreference)
                .checkRestrictionAndSetDisabled(
                        UserManager.DISALLOW_UNIFIED_PASSWORD, FAKE_PROFILE_USER_ID);
    }

     private void setupForDisplayPreference() {
         mController = new LockUnificationPreferenceController(mContext, mHost, "key");
         when(mScreen.findPreference(any())).thenReturn(mMockRestrictedSwitchPreference);
         when(mMockRestrictedSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
     }

    private void setupGetProfiles() {
        UserInfo managedProfile = new UserInfo(FAKE_PROFILE_USER_ID, "user1a", 0);
        managedProfile.profileGroupId = MY_USER_ID;
        managedProfile.userType = UserManager.USER_TYPE_PROFILE_MANAGED;
        when(mUm.getProfiles(anyInt())).thenReturn(Arrays.asList(managedProfile));
    }

     private void setupForUpdateState() {
         setupGetProfiles();
         setupForDisplayPreference();
         mController.displayPreference(mScreen);
         when(mLockPatternUtils.isSeparateProfileChallengeEnabled(anyInt())).thenReturn(true);
     }
}
