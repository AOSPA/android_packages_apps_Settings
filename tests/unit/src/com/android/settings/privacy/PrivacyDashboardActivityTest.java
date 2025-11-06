/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.flags.Flags;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.safetycenter.ui.SafetyCenterFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivacyDashboardActivityTest {
    private static final String DEFAULT_FRAGMENT_CLASSNAME = "DefaultFragmentClassname";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;
    private Settings.PrivacyDashboardActivity mActivity;
    private static final String ACTION_PRIVACY_ADVANCED_SETTINGS =
            "android.settings.PRIVACY_ADVANCED_SETTINGS";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    public void onCreate_whenSafetyCenterEnabled_oldUi_redirectsToSafetyCenter() throws Exception {
        startActivityUsingIntent(android.provider.Settings.ACTION_PRIVACY_SETTINGS);
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mActivity.handleSafetyCenterRedirection();
        verify(mActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_SAFETY_CENTER);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    public void onCreate_whenSafetyCenterEnabled_newUi_redirectsToSafetyCenter() throws Exception {
        startActivityUsingIntent(android.provider.Settings.ACTION_PRIVACY_SETTINGS);
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mActivity.handleSafetyCenterRedirection();
        verify(mActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intentCaptor.getValue().getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(SafetyCenterFragment.class.getName());
    }

    @Test
    public void onCreateWithAdvancedIntent_whenSafetyCenterEnabled_doesntRedirectToSafetyCenter()
            throws Exception {
        startActivityUsingIntent(ACTION_PRIVACY_ADVANCED_SETTINGS);
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mActivity.handleSafetyCenterRedirection();
        verify(mActivity, times(0)).startActivity(any());
    }

    @Test
    public void onCreate_whenSafetyCenterDisabled_doesntRedirectToSafetyCenter() throws Exception {
        startActivityUsingIntent(android.provider.Settings.ACTION_PRIVACY_SETTINGS);
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);
        mActivity.handleSafetyCenterRedirection();
        verify(mActivity, times(0)).startActivity(any());
    }

    @Test
    public void onCreateWithAdvancedIntent_whenSafetyCenterDisabled_doesntRedirectToSafetyCenter()
            throws Exception {
        startActivityUsingIntent(ACTION_PRIVACY_ADVANCED_SETTINGS);
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mActivity.handleSafetyCenterRedirection();
        verify(mActivity, times(0)).startActivity(any());
    }

    private void startActivityUsingIntent(String intentAction) throws Exception {
        MockitoAnnotations.initMocks(this);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        final Intent intent = new Intent();
        intent.setAction(intentAction);
        intent.setClass(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                Settings.PrivacyDashboardActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT_CLASSNAME);
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            try {
                                Settings.PrivacyDashboardActivity activity =
                                        (Settings.PrivacyDashboardActivity)
                                                InstrumentationRegistry.getInstrumentation()
                                                        .newActivity(
                                                                getClass().getClassLoader(),
                                                                Settings.PrivacyDashboardActivity
                                                                        .class
                                                                        .getName(),
                                                                intent);
                                activity.setIntent(intent);
                                mActivity = spy(activity);
                            } catch (Exception e) {
                                throw new RuntimeException(e); // nothing to do
                            }
                        });
        doNothing().when(mActivity).startActivity(any(Intent.class));

        PackageManager pm = mock(PackageManager.class);
        doReturn(pm).when(mActivity).getPackageManager();
        doReturn("com.android.permissioncontroller").when(pm).getPermissionControllerPackageName();
        doReturn("com.android.settings").when(mActivity).getPackageName();
    }
}
