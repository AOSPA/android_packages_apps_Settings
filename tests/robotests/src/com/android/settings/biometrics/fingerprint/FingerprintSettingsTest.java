/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowUtils.class,
        com.android.settings.testutils.shadow.ShadowLockPatternUtils.class})
public class FingerprintSettingsTest {

    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private UserManager mUserManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void getIntent_shouldIncludeFingerprintSettingsFragment() {
        try (ActivityScenario<FingerprintSettings> scenario = ActivityScenario.launch(
                FingerprintSettings.class)) {
            scenario.onActivity(activity -> {
                Intent intent = activity.getIntent();

                assertThat(intent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                        .isEqualTo(FingerprintSettings.FingerprintSettingsFragment.class.getName());
            });
        }
    }

    @Test
    public void isValidFragment_onlyFingerprintSettingsFragment_shouldBeValid() {
        try (ActivityScenario<FingerprintSettings> scenario = ActivityScenario.launch(
                FingerprintSettings.class)) {
            scenario.onActivity(activity -> {
                assertThat(activity.isValidFragment(
                        FingerprintSettings.FingerprintSettingsFragment.class.getName()))
                        .isTrue();
                assertThat(activity.isValidFragment("com.android.settings.SomeOtherFragment"))
                        .isFalse();
            });
        }
    }

    @Test
    public void onCreate_regularUser_shouldSetRegularTitle() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, FingerprintSettings.class);
        intent.putExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        try (ActivityScenario<FingerprintSettings> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertThat(activity.getTitle().toString())
                        .isEqualTo(
                                context.getText(
                                        R.string.security_settings_fingerprint_preference_title)
                                        .toString());
            });
        }
    }

    @Test
    public void isFingerprintHardwareDetected_managerNull_shouldReturnFalse() {
        ShadowUtils.setFingerprintManager(null);

        assertThat(FingerprintSettings.isFingerprintHardwareDetected(mContext)).isFalse();
    }

    @Test
    public void isFingerprintHardwareDetected_hardwareNotDetected_shouldReturnFalse() {
        doReturn(false).when(mFingerprintManager).isHardwareDetected();

        assertThat(FingerprintSettings.isFingerprintHardwareDetected(mContext)).isFalse();
    }

    @Test
    public void isFingerprintHardwareDetected_hardwareDetected_shouldReturnTrue() {
        doReturn(true).when(mFingerprintManager).isHardwareDetected();

        assertThat(FingerprintSettings.isFingerprintHardwareDetected(mContext)).isTrue();
    }
}
