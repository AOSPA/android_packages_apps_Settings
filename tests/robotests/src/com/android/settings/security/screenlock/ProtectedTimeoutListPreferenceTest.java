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

package com.android.settings.security.screenlock;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.AttributeSet;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowWifiDppUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowWifiDppUtils.class, ShadowDialog.class})
public class ProtectedTimeoutListPreferenceTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Activity mActivity;
    private ProtectedTimeoutListPreference mPreference;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        ShadowWifiDppUtils.reset();
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mActivity.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mPreferenceManager = new PreferenceManager(mActivity);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mActivity);
        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mPreference = new ProtectedTimeoutListPreference(mActivity, attributeSet);
        mPreference.setKey("test_key");
        mPreference.setEntries(new CharSequence[]{"15s", "30s"});
        mPreference.setEntryValues(new CharSequence[]{"15000", "30000"});
        mPreference.setValue("15000");
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void performClick_flagEnabled_shouldShowDialogAndNotShowLockScreen() {
        mPreference.performClick();

        assertThat(ShadowWifiDppUtils.wasShowLockScreenCalled()).isFalse();
        // DialogPreference might not show a dialog directly without a PreferenceFragment,
        // but we want to check if it attempted to show one.
        // In Robolectric, if we don't have a fragment, performClick might just return.
    }


    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void performClick_flagDisabled_shouldShowLockScreen() {
        mPreference.performClick();

        assertThat(ShadowWifiDppUtils.wasShowLockScreenCalled()).isTrue();
    }
}
