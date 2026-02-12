/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.display;


import static android.app.admin.DpcAuthority.DPC_AUTHORITY;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.flags.Flags;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link AutoBrightnessPreferenceControllerForSetupWizard}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class, ShadowDevicePolicyManager.class,
        ShadowRestrictedLockUtilsInternal.class})
public class AutoBrightnessPreferenceControllerForSetupWizardTest {

    private static final String PREFERENCE_KEY = "auto_brightness";

    private static final EnforcingAdmin ENFORCING_ADMIN = new EnforcingAdmin("test", DPC_AUTHORITY,
            UserHandle.CURRENT, new ComponentName("test", "test.class"));

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(
                Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED);
    }

    public AutoBrightnessPreferenceControllerForSetupWizardTest(FlagsParameterization flags) {
        mSetFlagsRule.setFlagsParameterization(flags);
    }

    private Context mContext;
    private AutoBrightnessPreferenceControllerForSetupWizard mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController =
                new AutoBrightnessPreferenceControllerForSetupWizard(mContext, PREFERENCE_KEY);
    }

    @Test
    public void displayPreference_preferenceVisibleTrue() {
        Preference preference =
                displayPreference(/* configAvailable= */ true, /* restricted= */ false);

        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_restricted_preferenceVisibleFalse() {
        Preference preference =
                displayPreference(/* configAvailable= */ true, /* restricted= */ true);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_configTrue_availableUnsearchable() {
        displayPreference(/* configAvailable= */ true, /* restricted= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_configTrueAndRestricted_conditionallyUnavailable() {
        displayPreference(/* configAvailable= */ true, /* restricted= */ true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configFalse_unsupportedOnDevice() {
        displayPreference(/* configAvailable= */ false, /* restricted= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_configFalseAndRestricted_conditionallyUnavailable() {
        displayPreference(/* configAvailable= */ false, /* restricted= */ true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    private RestrictedSwitchPreference displayPreference(
            boolean configAvailable, boolean restricted) {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, configAvailable);
        setConfigBrightnessPolicyEnforcementInfo(restricted);
        setConfigBrightnessOnRestrictedLockUtils(restricted);

        final PreferenceManager manager = new PreferenceManager(mContext);
        final PreferenceScreen screen = manager.createPreferenceScreen(mContext);
        final RestrictedSwitchPreference preference = new RestrictedSwitchPreference(mContext);
        // Normally user restriction is set on the preference XML attribute {@link R
        // .styleable#RestrictedPreference_userRestriction}. For test environment, we set it
        // explicitly here.
        preference.getRestrictedPreferenceHelper().setUserRestriction(
                UserManager.DISALLOW_CONFIG_BRIGHTNESS);
        preference.setKey(mController.getPreferenceKey());
        screen.addPreference(preference);

        mController.displayPreference(screen);
        assertThat(preference.isDisabledByAdmin()).isEqualTo(restricted);
        return preference;
    }

    private void setConfigBrightnessPolicyEnforcementInfo(boolean restricted) {
        PolicyEnforcementInfo policyEnforcementInfo = restricted ? new PolicyEnforcementInfo(
                List.of(ENFORCING_ADMIN)) : new PolicyEnforcementInfo(
                Collections.emptyList());
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                UserManager.DISALLOW_CONFIG_BRIGHTNESS, policyEnforcementInfo);
    }

    private void setConfigBrightnessOnRestrictedLockUtils(boolean restricted) {
        ShadowRestrictedLockUtilsInternal.setRestrictedByAdmin(restricted);
    }
}

