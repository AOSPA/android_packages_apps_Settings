/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayActivationPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    private MainSwitchPreference mPreference;
    private Context mContext;
    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayActivationPreferenceController mPreferenceController;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDisplayInfo.type = Display.TYPE_INTERNAL;
        DisplayAdjustments daj = null;
        Display display = new Display(mock(DisplayManagerGlobal.class),
                Display.DEFAULT_DISPLAY, mDisplayInfo, daj);
        mContext = RuntimeEnvironment.application.createDisplayContext(display);
        mColorDisplayManager = mContext.getSystemService(ColorDisplayManager.class);
        mPreference = new MainSwitchPreference(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mPreferenceController = new NightDisplayActivationPreferenceController(mContext,
                "night_display_activation");
        mPreferenceController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_isAvailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void configuredNightDisplayUnavailableAndNotBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(false);
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayAvailableAndBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(true);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayUnavailableAndBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_externalDisplay_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        mDisplayInfo.type = Display.TYPE_EXTERNAL;
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext, "night_display_activated");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mPreferenceController.isPublicSlice()).isTrue();
    }

    @Test
    public void onClick_activates() {
        mColorDisplayManager.setNightDisplayActivated(false);

        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext, "night_display_activated");
        controller.setChecked(true);

        assertThat(mColorDisplayManager.isNightDisplayActivated()).isEqualTo(true);
    }

    @Test
    public void onClick_deactivates() {
        mColorDisplayManager.setNightDisplayActivated(true);

        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext, "night_display_activated");
        controller.setChecked(false);

        assertThat(mColorDisplayManager.isNightDisplayActivated()).isEqualTo(false);
    }
}
// LINT.ThenChange(NightDisplayApiScreenTest.kt)
