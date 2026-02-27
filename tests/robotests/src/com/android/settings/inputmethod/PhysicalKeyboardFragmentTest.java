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

package com.android.settings.inputmethod;

import static com.android.settings.flags.Flags.FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.InputDevice;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowInputDevice;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        ShadowUserManager.class,
        ShadowInputDevice.class
})
public class PhysicalKeyboardFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private PhysicalKeyboardFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new PhysicalKeyboardFragment();
    }

    @Test
    @EnableFlags(FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE)
    public void isPageSearchEnabled_inDemoMode_returnsFalse() {
        // Put the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        SettingsShadowResources.overrideResource(
                R.bool.config_disable_keyboard_settings_in_demo_mode, true);

        assertThat(isPageSearchEnabled()).isFalse();

        // Clean up to the default value.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }

    @Test
    public void isPageSearchEnabled_notInDemoMode_returnsTrue() {
        InputDevice device = ShadowInputDevice.makeFullKeyboardInputDevicebyId(1);
        ShadowInputDevice.addDevice(device.getId(), device);

        assertThat(isPageSearchEnabled()).isTrue();

        ShadowInputDevice.reset();
    }

    @Test
    @EnableFlags(FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE)
    public void onStart_inDemoMode_disablesPreferenceScreen() {
        // Put the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        SettingsShadowResources.overrideResource(
                R.bool.config_disable_keyboard_settings_in_demo_mode, true);

        PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        PreferenceManager preferenceManager = mock(PreferenceManager.class);
        when(preferenceManager.getContext()).thenReturn(mContext);
        when(preferenceManager.getPreferenceScreen()).thenReturn(preferenceScreen);
        ReflectionHelpers.setField(mFragment, "mPreferenceManager", preferenceManager);
        DashboardFeatureProvider dashboardFeatureProvider = mock(DashboardFeatureProvider.class);
        ReflectionHelpers.setField(mFragment,
                "mDashboardFeatureProvider", dashboardFeatureProvider);

        mFragment.onStart();

        verify(preferenceScreen).setEnabled(false);

        // Clean up to the default value.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }

    @Test
    public void onStart_notInDemoMode_enablePreferenceScreen() {
        PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        PreferenceManager preferenceManager = mock(PreferenceManager.class);
        when(preferenceManager.getContext()).thenReturn(mContext);
        when(preferenceManager.getPreferenceScreen()).thenReturn(preferenceScreen);
        ReflectionHelpers.setField(mFragment, "mPreferenceManager", preferenceManager);
        DashboardFeatureProvider dashboardFeatureProvider = mock(DashboardFeatureProvider.class);
        ReflectionHelpers.setField(mFragment,
                "mDashboardFeatureProvider", dashboardFeatureProvider);

        mFragment.onStart();

        verify(preferenceScreen, never()).setEnabled(false);
    }

    private boolean isPageSearchEnabled() {
        return ReflectionHelpers.callInstanceMethod(
                PhysicalKeyboardFragment.SEARCH_INDEX_DATA_PROVIDER,
                "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
    }
}
