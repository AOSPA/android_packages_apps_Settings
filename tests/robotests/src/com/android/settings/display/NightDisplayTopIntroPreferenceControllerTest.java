/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.hardware.display.DisplayManagerGlobal;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayTopIntroPreferenceControllerTest {

    private NightDisplayTopIntroPreferenceController mController;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    @Before
    public void setUp() {
        mDisplayInfo.type = Display.TYPE_INTERNAL;
        DisplayAdjustments daj = null;
        Display display = new Display(mock(DisplayManagerGlobal.class),
                Display.DEFAULT_DISPLAY, mDisplayInfo, daj);
        mController = new NightDisplayTopIntroPreferenceController(
                RuntimeEnvironment.application.createDisplayContext(display), "key");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_returnAvailableUnsearchable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void configuredNightDisplayUnavailableAndNotBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(false);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayAvailableAndBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(true);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayUnavailableAndBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_externalDisplay_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        mDisplayInfo.type = Display.TYPE_EXTERNAL;
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
