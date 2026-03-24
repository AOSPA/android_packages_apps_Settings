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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.location.LocationManager;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayAutoModePreferenceControllerTest {

    private Context mContext;
    private NightDisplayAutoModePreferenceController mController;
    private LocationManager mLocationManager;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    @Before
    public void setUp() {
        mDisplayInfo.type = Display.TYPE_INTERNAL;
        DisplayAdjustments daj = null;
        Display display = new Display(mock(DisplayManagerGlobal.class),
                Display.DEFAULT_DISPLAY, mDisplayInfo, daj);
        mContext = Mockito.spy(RuntimeEnvironment.application.createDisplayContext(display));
        mLocationManager = Mockito.mock(LocationManager.class);
        when(mLocationManager.isLocationEnabled()).thenReturn(true);
        when(mContext.getSystemService(eq(LocationManager.class))).thenReturn(mLocationManager);
        mController = new NightDisplayAutoModePreferenceController(mContext,
            "night_display_auto_mode");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_isAvailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void configuredNightDisplayUnavailableAndNotBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayAvailableAndBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(true);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayUnavailableAndBlocked_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_externalDisplay_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        mDisplayInfo.type = Display.TYPE_EXTERNAL;
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_changesAutoMode() {
        mController.onPreferenceChange(null,
                String.valueOf(ColorDisplayManager.AUTO_MODE_TWILIGHT));
        assertThat(mContext.getSystemService(ColorDisplayManager.class).getNightDisplayAutoMode())
                .isEqualTo(ColorDisplayManager.AUTO_MODE_TWILIGHT);
    }
}
// LINT.ThenChange(NightDisplayApiScreenTest.kt)
