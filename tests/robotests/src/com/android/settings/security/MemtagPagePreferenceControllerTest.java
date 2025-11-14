/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.robolectric.Shadows.shadowOf;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.UnknownAuthority;

import android.content.ComponentName;
import android.content.Context;

import android.os.UserHandle;

import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class,
                   ShadowDevicePolicyManager.class})
public class MemtagPagePreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final String mMemtagSupportedProperty = "ro.arm64.memtag.bootctl_supported";

    private MemtagPagePreferenceController mController;
    private Context mContext;
    private ShadowDevicePolicyManager mShadowDevicePolicyManager;

    private static final String FRAGMENT_TAG = "memtag_page";

    @Before
    public void setUp() {
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");

        mContext = RuntimeEnvironment.application;
        mController = new MemtagPagePreferenceController(mContext, FRAGMENT_TAG);
        mShadowDevicePolicyManager = ShadowDevicePolicyManager.getShadow();
    }

    @Test
    public void displayPreference_disabledByEnforcedAdmin_disablesPreference() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED);
        ShadowRestrictedLockUtilsInternal.setMteIsDisabled(true);
        RestrictedPreference preference = new RestrictedPreference(mContext);
        preference.setKey(mController.getPreferenceKey());
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(preference);

        mController.displayPreference(screen);
        assertThat(preference.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void displayPreference_disabledByEnforcingAdmin_disablesPreference() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED);
        EnforcingAdmin enforcingAdmin = new EnforcingAdmin("test.pkg",
                UnknownAuthority.UNKNOWN_AUTHORITY, UserHandle.of(UserHandle.myUserId()),
                new ComponentName("", ""));
        mShadowDevicePolicyManager.setPolicyEnforcementInfoForPolicy(
                DevicePolicyIdentifiers.MEMORY_TAGGING_POLICY,
                new PolicyEnforcementInfo(Collections.singletonList(enforcingAdmin)));
        RestrictedPreference preference = new RestrictedPreference(mContext);
        preference.setKey(mController.getPreferenceKey());
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preference.isDisabledByAdmin()).isTrue();
    }
}
