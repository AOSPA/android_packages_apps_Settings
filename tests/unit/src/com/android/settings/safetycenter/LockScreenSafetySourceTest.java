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

package com.android.settings.safetycenter;

import static android.app.admin.DpcAuthority.DPC_AUTHORITY;
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.EnforcingAdmin;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.flags.Flags;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

import java.util.List;

@RunWith(ParameterizedAndroidJunit4.class)
public class LockScreenSafetySourceTest {

    private static final String SUMMARY = "summary";
    private static final String FAKE_ACTION_OPEN_SUB_SETTING = "open_sub_setting";
    private static final String EXTRA_DESTINATION = "destination";
    private static final String FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT = "choose_lock_generic";
    private static final String FAKE_SCREEN_LOCK_SETTINGS = "screen_lock_settings";
    private static final SafetyEvent EVENT_SOURCE_STATE_CHANGED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build();

    private final EnforcingAdmin mEnforcingAdmin = new EnforcingAdmin("package", DPC_AUTHORITY,
            UserHandle.of(UserHandle.myUserId()));
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mApplicationContext;

    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Mock private ScreenLockPreferenceDetailsUtils mScreenLockPreferenceDetailsUtils;

    @Mock private LockPatternUtils mLockPatternUtils;

    private Resources mResources;

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(
                android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED);
    }

    public LockScreenSafetySourceTest(FlagsParameterization flags) {
        mSetFlagsRule.setFlagsParameterization(flags);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mApplicationContext.getResources());
        when(mApplicationContext.getResources()).thenReturn(mResources);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mApplicationContext))
                .thenReturn(mLockPatternUtils);
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    public void setSafetySourceData_whenScreenLockEnabled_safetyCenterDisabled_doesNotSetData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    public void setSafetySourceData_whenScreenLockIsDisabled_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(false);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), eq(null), any());
    }

    @Test
    public void setSafetySourceData_setsDataForLockscreenSafetySource() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    public void setSafetySourceData_setsDataWithCorrectSafetyEvent() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), any(), eq(EVENT_SOURCE_STATE_CHANGED));
    }

    @Test
    public void setSafetySourceData_whenScreenLockIsEnabled_setData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "unlock_set_unlock_launch_picker_title"));
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(SUMMARY);
        assertThat(safetySourceStatus.getPendingIntent().getIntent()).isNotNull();
        assertThat(safetySourceStatus.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_OPEN_SUB_SETTING);
        assertThat(
                        safetySourceStatus
                                .getPendingIntent()
                                .getIntent()
                                .getStringExtra(EXTRA_DESTINATION))
                .isEqualTo(FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsSec_setStatusLevelInfo() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);


        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsNotSec_setStatusLevelRec() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsSec_setStatusLevelUnsp() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(mEnforcingAdmin);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsNotSec_setStatusLevelUnsp() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(mEnforcingAdmin);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsNotMan_whenLockPattIsNotSec_setsIssue() {
        whenScreenLockIsEnabled();
        setConfigHidePatternSecurityOptionEnabled(false);
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).hasSize(1);
        SafetySourceIssue issue = safetySourceData.getIssues().get(0);
        assertThat(issue.getId()).isEqualTo(LockScreenSafetySource.NO_SCREEN_LOCK_ISSUE_ID);
        assertThat(issue.getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_title"));
        assertThat(issue.getSummary().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_summary"));
        assertThat(issue.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION);
        assertThat(issue.getIssueTypeId())
                .isEqualTo(LockScreenSafetySource.NO_SCREEN_LOCK_ISSUE_TYPE_ID);
        assertThat(issue.getIssueCategory()).isEqualTo(SafetySourceIssue.ISSUE_CATEGORY_DEVICE);
        assertThat(issue.getCustomNotification().getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_notification_title"));
        assertThat(issue.getCustomNotification().getText().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_notification_text"));
        assertThat(issue.getActions()).hasSize(1);
        SafetySourceIssue.Action action = issue.getActions().get(0);
        assertThat(action.getId()).isEqualTo(LockScreenSafetySource.SET_SCREEN_LOCK_ACTION_ID);
        assertThat(action.getLabel().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_action_label"));
        assertThat(action.getPendingIntent().getIntent().getAction())
                .isEqualTo(FAKE_ACTION_OPEN_SUB_SETTING);
        assertThat(action.getPendingIntent().getIntent().getStringExtra(EXTRA_DESTINATION))
                .isEqualTo(FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
    }

    @Test
    public void setSafetySourceData_whensLockPattIsNotSec_patternUnsupported_setsIssue() {
        whenScreenLockIsEnabled();
        setConfigHidePatternSecurityOptionEnabled(true);
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).hasSize(1);
        SafetySourceIssue issue = safetySourceData.getIssues().get(0);
        assertThat(issue.getId()).isEqualTo(LockScreenSafetySource.NO_SCREEN_LOCK_ISSUE_ID);
        assertThat(issue.getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_title"));
        assertThat(issue.getSummary().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext,
                                "no_screen_lock_issue_summary_without_pattern"));
        assertThat(issue.getCustomNotification().getTitle().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "no_screen_lock_issue_notification_title"));
        assertThat(issue.getCustomNotification().getText().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext,
                                "no_screen_lock_issue_notification_text_without_pattern"));
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(mEnforcingAdmin);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), captor.capture(), any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPwdQualIsMan_whenLockPattIsNotSec_doesNotSetIssues() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(mEnforcingAdmin);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();

        assertThat(safetySourceData.getIssues()).isEmpty();
    }

    @Test
    public void setSafetySourceData_whenPasswordQualityIsManaged_setDisabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(mEnforcingAdmin);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isFalse();
        assertThat(safetySourceStatus.getPendingIntent()).isNull();
        assertThat(safetySourceStatus.getIconAction()).isNull();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);
        assertThat(safetySourceStatus.getSummary().toString())
                .isEqualTo(
                        ResourcesUtils.getResourcesString(
                                mApplicationContext, "disabled_by_policy_title"));
    }

    @Test
    public void setSafetySourceData_whenPasswordQualityIsNotManaged_setEnabled() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.isPasswordQualityManaged(anyInt(), any()))
                .thenReturn(false);
        when(mScreenLockPreferenceDetailsUtils.getPasswordQualityManagedEnforcingAdmin(
                anyInt())).thenReturn(null);
        when(mScreenLockPreferenceDetailsUtils.isLockPatternSecure()).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.isEnabled()).isTrue();
        assertThat(safetySourceStatus.getPendingIntent()).isNotNull();
        assertThat(safetySourceStatus.getIconAction()).isNull();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(SUMMARY);
    }

    @Test
    public void setSafetySourceData_doesNotSetGearMenuActionIcon() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.setSafetySourceData(
                mApplicationContext, mScreenLockPreferenceDetailsUtils, EVENT_SOURCE_STATE_CHANGED);

        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(LockScreenSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getIconAction()).isNull();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void
            onLockScreenChange_whenSafetyCenterEnabled_flagOff_setsLockscreenAndBiometricData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), any(), any());
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(BiometricsSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void onLockScreenChange_whenSafetyCenterEnabled_flagOn_setsLockscreenAndBiometricData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(LockScreenSafetySource.SAFETY_SOURCE_ID), any(), any());
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), eq(FaceSafetySource.SAFETY_SOURCE_ID), any(), any());
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(FingerprintSafetySource.SAFETY_SOURCE_ID), any(), any());
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(WearSafetySource.SAFETY_SOURCE_ID), any(), any());
    }

    @Test
    public void onLockScreenChange_whenSafetyCenterDisabled_doesNotSetData() {
        whenScreenLockIsEnabled();
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        LockScreenSafetySource.onLockScreenChange(mApplicationContext);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    private void setConfigHidePatternSecurityOptionEnabled(boolean enabled) {
        final int resId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_hide_pattern_security_option");
        doReturn(enabled).when(mResources).getBoolean(resId);
    }

    private void whenScreenLockIsEnabled() {
        when(mScreenLockPreferenceDetailsUtils.isAvailable()).thenReturn(true);
        when(mScreenLockPreferenceDetailsUtils.getSummary(anyInt())).thenReturn(SUMMARY);

        Intent launchChooseLockGenericFragment = new Intent(FAKE_ACTION_OPEN_SUB_SETTING);
        launchChooseLockGenericFragment.putExtra(
                EXTRA_DESTINATION, FAKE_CHOOSE_LOCK_GENERIC_FRAGMENT);
        when(mScreenLockPreferenceDetailsUtils.getLaunchChooseLockGenericFragmentIntent(anyInt()))
                .thenReturn(launchChooseLockGenericFragment);

        Intent launchScreenLockSettings = new Intent(FAKE_ACTION_OPEN_SUB_SETTING);
        launchScreenLockSettings.putExtra(EXTRA_DESTINATION, FAKE_SCREEN_LOCK_SETTINGS);
    }
}
