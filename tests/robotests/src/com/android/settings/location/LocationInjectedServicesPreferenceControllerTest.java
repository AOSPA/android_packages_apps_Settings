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
package com.android.settings.location;

import static android.app.admin.DpcAuthority.DPC_AUTHORITY;

import static com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class LocationInjectedServicesPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String KEY_LOCATION_SERVICES = "location_services_screen";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocationSettings mFragment;
    @Mock
    private PreferenceCategory mCategoryPrimary;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AppSettingsInjector mSettingsInjector;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private DevicePolicyResourcesManager mDevicePolicyResourcesManager;

    private Context mContext;
    private LocationInjectedServicesPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(
                new LocationInjectedServicesPreferenceController(mContext, KEY_LOCATION_SERVICES));
        when(mFragment.getSettingsLifecycle()).thenReturn(mLifecycle);
        mController.init(mFragment);
        mController.mInjector = mSettingsInjector;
        final String key = mController.getPreferenceKey();
        when(mScreen.findPreference(key)).thenReturn(mCategoryPrimary);
        when(mCategoryPrimary.getKey()).thenReturn(key);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.getResources()).thenReturn(mDevicePolicyResourcesManager);
    }

    @Test
    public void onResume_shouldRegisterListener() {
        mController.onResume();

        verify(mContext).registerReceiver(eq(mController.mInjectedSettingsReceiver),
                eq(mController.INTENT_FILTER_INJECTED_SETTING_CHANGED),
                anyInt());
    }

    @Test
    public void onPause_shouldUnregisterListener() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(mController.mInjectedSettingsReceiver);
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void workProfileDisallowShareLocationOn_getParentUserLocationServicesOnly() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));
        // Mock the admin restriction on the preference.
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(List.of(DPC_ADMIN)));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void workProfileDisallowShareLocationOff_getAllUserLocationServices() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));
        // Mock restricted preference and make it return empty admins.
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(Collections.emptyList()));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void privateProfileDisallowShareLocationOn_getParentUserLocationServicesOnly() {
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));
        // Mock the admin restriction on the preference.
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(List.of(DPC_ADMIN)));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void privateProfileDisallowShareLocationOff_getAllUserLocationServices() {
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));
        // Mock restricted preference and make it return empty admins.
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(Collections.emptyList()));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    public void onLocationModeChanged_shouldRequestReloadInjectedSettigns() {
        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSettingsInjector).reloadStatusMessages();
    }

    @Test
    @EnableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void withUserRestriction_shouldDisableLocationAccuracy() {
        // Mock the admin restriction on the preference.
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(List.of(DPC_ADMIN)));
        final List<Preference> preferences = new ArrayList<>();
        final RestrictedAppPreference pref = new RestrictedAppPreference(mContext,
                UserManager.DISALLOW_CONFIG_LOCATION);
        pref.setTitle("Location Accuracy");
        preferences.add(pref);
        final Map<Integer, List<Preference>> map = new ArrayMap<>();
        map.put(UserHandle.myUserId(), preferences);
        doReturn(map).when(mSettingsInjector)
                .getInjectedSettings(any(Context.class), any(ArraySet.class));
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{UserHandle.myUserId()});

        mController.displayPreference(mScreen);

        assertThat(pref.isEnabled()).isFalse();
        assertThat(pref.isDisabledByAdmin()).isTrue();
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void workProfileDisallowShareLocationOn_getParentUserLocationServicesOnly_refactorDisabled() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));
        // Mock the admin restriction on the preference.
        List<UserManager.EnforcingUser> enforcers = new ArrayList<>();
        enforcers.add(new UserManager.EnforcingUser(fakeWorkProfileId,
                UserManager.RESTRICTION_SOURCE_PROFILE_OWNER));
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION, UserHandle.of(fakeWorkProfileId), enforcers);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void workProfileDisallowShareLocationOff_getAllUserLocationServices_refactorDisabled() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void privateProfileDisallowShareLocationOn_getParentUserLocationServicesOnly_refactorDisabled() {
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));
        // Mock the admin restriction on the preference.
        List<UserManager.EnforcingUser> enforcers = new ArrayList<>();
        enforcers.add(new UserManager.EnforcingUser(fakePrivateProfileId,
                UserManager.RESTRICTION_SOURCE_PROFILE_OWNER));
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION, UserHandle.of(fakePrivateProfileId),
                enforcers);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void privateProfileDisallowShareLocationOff_getAllUserLocationServices_refactorDisabled() {
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    @DisableFlags(android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void withUserRestriction_shouldDisableLocationAccuracy_refactorDisabled() {
        // Mock the admin restriction on the preference.
        ComponentName componentName = new ComponentName("test", "test");
        EnforcingAdmin enforcingAdmin = new EnforcingAdmin("test", DPC_AUTHORITY,
                UserHandle.of(UserHandle.myUserId()), componentName);
        when(mDevicePolicyManager.getEnforcingAdminsForPolicy(any(), anyInt())).thenReturn(
                new PolicyEnforcementInfo(List.of(enforcingAdmin)));
        final List<Preference> preferences = new ArrayList<>();
        final RestrictedAppPreference pref = new RestrictedAppPreference(mContext,
                UserManager.DISALLOW_CONFIG_LOCATION);
        pref.setTitle("Location Accuracy");
        preferences.add(pref);
        final Map<Integer, List<Preference>> map = new ArrayMap<>();
        map.put(UserHandle.myUserId(), preferences);
        doReturn(map).when(mSettingsInjector)
                .getInjectedSettings(any(Context.class), any(ArraySet.class));
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{UserHandle.myUserId()});

        List<UserManager.EnforcingUser> enforcers = new ArrayList<>();
        enforcers.add(new UserManager.EnforcingUser(UserHandle.myUserId(),
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_CONFIG_LOCATION, UserHandle.of(UserHandle.myUserId()),
                enforcers);

        mController.displayPreference(mScreen);

        assertThat(pref.isEnabled()).isFalse();
        assertThat(pref.isDisabledByAdmin()).isTrue();
    }
}
