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

import static android.os.UserManager.DISALLOW_CONFIG_DATE_TIME;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import android.app.DatePickerDialog;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.PolicyIdentifier;
import android.app.admin.flags.Flags;
import android.app.time.Capabilities;
import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.app.timedetector.TimeDetector;
import android.app.timedetector.TimeDetectorHelper;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import com.android.settings.testutils.DevicePolicyUtils;
import com.google.testing.junit.testparameterinjector.TestParameters;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedPreferenceHelper;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

@RunWith(RobolectricTestParameterInjector.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class DatePreferenceControllerTest {

    @Mock
    private DatePreferenceController.DatePreferenceHost mHost;
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private TimeDetector mTimeDetector;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private android.app.admin.DevicePolicyResourcesManager mResources;
    @Mock
    private RestrictedPreferenceHelper mHelper;

    private Context mContext;
    private RestrictedPreference mPreference;
    private DatePreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeDetector.class)).thenReturn(mTimeDetector);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);
        when(mDpm.getResources()).thenReturn(mResources);
        mPreference = spy(new RestrictedPreference(mContext));
        mController = new DatePreferenceController(mContext, "test_key");
        mController.setHost(mHost);
    }

    @Test
    public void shouldHandleDateSetCallback() {
        mController.onDateSet(null, 2016, 1, 1);
        verify(mHost).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void updateState_autoTimeEnabled_shouldDisablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin((RestrictedLockUtils.EnforcedAdmin) null);

        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* suggestManualAllowed= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_autoTimeDisabled_shouldEnablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin((RestrictedLockUtils.EnforcedAdmin) null);

        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* suggestManualAllowed= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void clickPreference_showDatePicker() {
        // Click a preference that's not controlled by this controller.
        mPreference.setKey("fake_key");
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();

        // Click a preference controlled by this controller.
        mPreference.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(mPreference);
        // Should show date picker
        verify(mHost).showDatePicker();
    }

    @Test
    public void testBuildDatePicker() {
        TimeDetectorHelper timeDetectorHelper = mock(TimeDetectorHelper.class);
        when(timeDetectorHelper.getManualDateSelectionYearMin()).thenReturn(2015);
        when(timeDetectorHelper.getManualDateSelectionYearMax()).thenReturn(2020);

        Context context = RuntimeEnvironment.application;
        DatePickerDialog dialog = mController.buildDatePicker(context, timeDetectorHelper);

        GregorianCalendar calendar = new GregorianCalendar();

        long minDate = dialog.getDatePicker().getMinDate();
        calendar.setTimeInMillis(minDate);
        assertEquals(2015, calendar.get(Calendar.YEAR));

        long maxDate = dialog.getDatePicker().getMaxDate();
        calendar.setTimeInMillis(maxDate);
        assertEquals(2020, calendar.get(Calendar.YEAR));
    }

    @Test
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_DISABLED +"}")
    @TestParameters("{autoTimePolicyValue: " + PolicyIdentifier.AUTO_TIME_ENABLED +"}")
    @EnableFlags(Flags.FLAG_POLICY_STREAMLINING_AUTO_TIME)
    public void updateState_autoTimePolicyForcedValues_shouldDisableSetting(
            int autoTimePolicyValue) {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* suggestManualAllowed= */true);
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
                /* suggestManualAllowed= */true);
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
                /* suggestManualAllowed= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mPreference.getRestrictedPreferenceHelper()).thenReturn(mHelper);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        verify(mHelper).setDisabledByEnforcingAdmin(null);
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
                /* suggestManualAllowed= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mPreference.getRestrictedPreferenceHelper()).thenReturn(mHelper);
        when(mDpm.getResolvedDeviceWidePolicy(
                PolicyIdentifier.AUTO_TIME)).thenReturn(autoTimePolicyValue);
        setupEnforcingAdminForPolicy();

        mController.updateState(mPreference);

        verify(mHelper, never()).setDisabledByEnforcingAdmin(null);
        verify(mHelper, never()).checkRestrictionAndSetDisabled(
                DISALLOW_CONFIG_DATE_TIME, UserHandle.myUserId());
    }


    static TimeCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean suggestManualAllowed) {
        int suggestManualCapability = suggestManualAllowed ? Capabilities.CAPABILITY_POSSESSED
                : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeCapabilities capabilities = new TimeCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setSetManualTimeCapability(suggestManualCapability)
                .build();
        TimeConfiguration config = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(!suggestManualAllowed)
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
