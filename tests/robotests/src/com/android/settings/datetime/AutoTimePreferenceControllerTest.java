/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static android.os.UserManager.DISALLOW_CONFIG_DATE_TIME;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.app.admin.DpcAuthority;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.flags.Flags;
import android.app.admin.EnforcingAdmin;
import android.app.time.Capabilities;
import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.admin.PolicyIdentifier;
import android.app.time.TimeManager;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import com.android.settings.testutils.DevicePolicyUtils;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.util.List;
import android.platform.test.flag.junit.SetFlagsRule;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.RestrictedPreferenceHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestParameterInjector.class)
public class AutoTimePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;
    private Context mContext;
    private AutoTimePreferenceController mController;
    private RestrictedSwitchPreference mPreference;
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private DevicePolicyResourcesManager mResources;
    @Mock
    private RestrictedPreferenceHelper mHelper;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = spy(new RestrictedSwitchPreference(mContext));
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);
        when(mDpm.getResources()).thenReturn(mResources);

        mController = new AutoTimePreferenceController(mContext, "test_key");
        mController.setDateAndTimeCallback(mCallback);
    }

    @Test
    public void autoTimeNotSupported_notAvailable() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void autoTimeNotSupported_notEnable() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsFalse_shouldReadFromTimeManagerConfig() {
        // Disabled
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsTrue_shouldReadFromTimeManagerConfig() {
        // Enabled
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.onPreferenceChange(mPreference, true)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeConfiguration timeConfiguration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeConfiguration(timeConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(
                capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.onPreferenceChange(mPreference, false)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeConfiguration timeConfiguration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .build();
        verify(mTimeManager).updateTimeConfiguration(timeConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(
                capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void getSummary() {
        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.date_time_auto_summary));
    }

    @Test
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_DISABLED +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_ENABLED +"}")
    @EnableFlags(Flags.FLAG_POLICY_STREAMLINING_AUTO_TIME)
    public void updateState_autoTimePolicyForcedValues_shouldDisableSetting(
            int autoTimePolicyValue) {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        EnforcingAdmin admin = setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        assertTrue(mPreference.isDisabledByAdmin());
    }

    @Test
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_DISABLED +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_ENABLED +"}")
    @DisableFlags(Flags.FLAG_POLICY_STREAMLINING_AUTO_TIME)
    public void updateState_autoTimePolicyForcedValues_shouldNotDisableSetting_flagDisabled(
            int autoTimePolicyValue) {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        EnforcingAdmin admin = setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        assertFalse(mPreference.isDisabledByAdmin());
    }

    @Test
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_USER_CHOICE +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_DISABLED_UNENFORCED +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_ENABLED_UNENFORCED +"}")
    @EnableFlags(Flags.FLAG_POLICY_STREAMLINING_AUTO_TIME)
    public void updateState_autoTimeUnenforced_settingDisabled_userRestrictionChecked(
            int autoTimePolicyValue) {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mPreference.getRestrictedPreferenceHelper()).thenReturn(mHelper);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        verify(mHelper).setDisabledByEnforcingAdmin((EnforcingAdmin) null);
        verify(mHelper).checkRestrictionAndSetDisabled(
                DISALLOW_CONFIG_DATE_TIME, UserHandle.myUserId());
    }

    @Test
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_USER_CHOICE +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_DISABLED_UNENFORCED +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_ENABLED_UNENFORCED +"}")
    @DisableFlags(Flags.FLAG_POLICY_STREAMLINING_AUTO_TIME)
    public void updateState_autoTimeUnenforced_settingDisabled_userRestrictionSkipped_flagDisabled(
            int autoTimePolicyValue) {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mPreference.getRestrictedPreferenceHelper()).thenReturn(mHelper);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        verify(mHelper, never()).setDisabledByEnforcingAdmin((EnforcingAdmin) null);
        verify(mHelper, never()).checkRestrictionAndSetDisabled(
                DISALLOW_CONFIG_DATE_TIME, UserHandle.myUserId());
    }

    private static TimeCapabilitiesAndConfig createCapabilitiesAndConfig(boolean autoSupported,
            boolean autoEnabled) {
        int configureAutoDetectionEnabledCapability =
                autoSupported ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeCapabilities capabilities = new TimeCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(configureAutoDetectionEnabledCapability)
                .setSetManualTimeCapability(Capabilities.CAPABILITY_POSSESSED)
                .build();
        TimeConfiguration config = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(autoEnabled)
                .build();
        return new TimeCapabilitiesAndConfig(capabilities, config);
    }

    private EnforcingAdmin setupEnforcingAdminForPolicy() {
        EnforcingAdmin admin = DevicePolicyUtils.DPC_ADMIN;
        when(mDpm.getEnforcingAdminsForPolicy(
                DevicePolicyIdentifiers.AUTO_TIME_POLICY,
                UserHandle.myUserId())).thenReturn(new PolicyEnforcementInfo(List.of(admin)));
        return admin;
    }
}
