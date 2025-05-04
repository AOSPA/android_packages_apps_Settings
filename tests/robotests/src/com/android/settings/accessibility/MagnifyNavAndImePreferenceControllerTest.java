/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class MagnifyNavAndImePreferenceControllerTest {
    private static final String KEY_MAGNIFY_NAV_AND_IME =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME;

    private Context mContext;
    private ShadowContentResolver mShadowContentResolver;
    private SwitchPreference mSwitchPreference;
    private MagnifyNavAndImePreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule =
            new SetFlagsRule(SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference = spy(new SwitchPreference(mContext));
        mSwitchPreference.setKey(MagnifyNavAndImePreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);

        mController = new MagnifyNavAndImePreferenceController(mContext,
                MagnifyNavAndImePreferenceController.PREF_KEY);
        mController.displayPreference(screen);
        mController.updateState(mSwitchPreference);

        reset(mSwitchPreference);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    public void getAvailabilityStatus_defaultState_disabled() {
        int status = mController.getAvailabilityStatus();

        assertThat(status).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    public void getAvailabilityStatus_featureFlagEnabled_enabled() {
        int status = mController.getAvailabilityStatus();

        assertThat(status).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_settingsEnabled_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, ON);
        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_settingsDisabled_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, OFF);
        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void performClick_switchDisabled_shouldReturnEnable() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, OFF);
        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(true);
        //assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void performClick_switchEnabled_shouldReturnDisable() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_MAGNIFY_NAV_AND_IME, ON);
        mController.updateState(mSwitchPreference);

        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(false);
        //assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onResume_verifyRegisterCapabilityObserver() {
        mController.onResume();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .hasSize(1);
    }

    @Test
    public void onPause_verifyUnregisterCapabilityObserver() {
        mController.onResume();
        mController.onPause();
        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY)))
                .isEmpty();
    }
    @Test
    public void updateState_windowModeOnly_preferenceBecomesUnavailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_fullscreenModeOnly_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.FULLSCREEN);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_switchMode_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    @Config(shadows = SettingsShadowResources.class)
    public void isChecked_defaultValueOn() {
        Settings.Secure.clearProviderForTest();
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_magnification_magnify_keyboard_default,
                true);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    @Config(shadows = SettingsShadowResources.class)
    public void isChecked_defaultValueOff() {
        Settings.Secure.clearProviderForTest();
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_magnification_magnify_keyboard_default,
                false);

        mController.updateState(mSwitchPreference);
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }
}
