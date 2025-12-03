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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link BrightnessLevelPreferenceControllerForSetupWizard}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDevicePolicyManager.class})
public class BrightnessLevelPreferenceControllerForSetupWizardTest {
    private static final EnforcingAdmin ENFORCING_ADMIN = new EnforcingAdmin("test", DPC_AUTHORITY,
            UserHandle.CURRENT, new ComponentName("test", "test.class"));

    private Context mContext;
    private BrightnessLevelPreferenceControllerForSetupWizard mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new BrightnessLevelPreferenceControllerForSetupWizard(mContext,
                /* lifecycle= */ null);
    }

    @Test
    public void displayPreference_flagOn_preferenceVisibleTrue() {
        Preference preference = displayPreference(/* restricted= */ false);

        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_restricted_preferenceVisibleFalse() {
        Preference preference = displayPreference(/* restricted= */ true);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_notRestricted_available() {
        displayPreference(/* restricted= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_restricted_conditionallyUnavailable() {
        displayPreference(/* restricted= */ true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    private RestrictedPreference displayPreference(boolean restricted) {
        setConfigBrightnessPolicyEnforcementInfo(restricted);
        final PreferenceManager manager = new PreferenceManager(mContext);
        final PreferenceScreen screen = manager.createPreferenceScreen(mContext);
        final RestrictedPreference preference = new RestrictedPreference(mContext);
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
}
