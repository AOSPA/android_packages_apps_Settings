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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.R;
import com.android.settings.connecteddevice.display.ConnectedDisplayInjector;
import com.android.settings.connecteddevice.display.DisplayDevice;
import com.android.settings.connecteddevice.display.DisplayIsEnabled;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDesktopSettingsUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        ShadowDesktopSettingsUtils.class
})
public class TopLevelDisplayPreferenceControllerTest {
    private Context mContext;
    private TopLevelDisplayPreferenceController mController;
    private PackageManager mPackageManager;
    private ConnectedDisplayInjector mConnectedDisplayInjector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mConnectedDisplayInjector = mock(ConnectedDisplayInjector.class);
        when(mConnectedDisplayInjector.getDisplays()).thenReturn(new ArrayList<>());

        mController = new TopLevelDisplayPreferenceController(mContext, "test_key");
        ReflectionHelpers.setField(mController, "mConnectedDisplayInjector", mConnectedDisplayInjector);
    }

    private void setupController() {
        mController = new TopLevelDisplayPreferenceController(mContext, "test_key");
        ReflectionHelpers.setField(mController, "mConnectedDisplayInjector", mConnectedDisplayInjector);
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_unsupportedWhenSet() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_topLevelDeviceCategoryOff_shouldReturnDefaultSummary() {
        ShadowDesktopSettingsUtils.setShouldShow(false);
        setupController();

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.display_dashboard_summary));
    }

    @Test
    public void getSummary_topLevelDeviceCategory_shouldUseDeviceDisplaySummary() {
        ShadowDesktopSettingsUtils.setShouldShow(true);
        setupController();
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(false);
        when(mConnectedDisplayInjector.getDisplays()).thenReturn(new ArrayList<>());

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.device_display_dashboard_summary));
    }

    @Test
    public void getSummary_topLevelDeviceCategoryHasTouch_shouldIncludeTouch() {
        ShadowDesktopSettingsUtils.setShouldShow(true);
        setupController();
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true);
        when(mConnectedDisplayInjector.getDisplays()).thenReturn(new ArrayList<>());

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.device_display_dashboard_summary_with_touch));
    }

    @Test
    public void getSummary_topLevelDeviceCategoryHasExternalDisplay_shouldIncludeConnectedDisplay() {
        ShadowDesktopSettingsUtils.setShouldShow(true);
        setupController();
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(false);
        when(mConnectedDisplayInjector.getDisplays()).thenReturn(createExternalDisplays());

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.device_display_dashboard_summary_with_external));
    }

    @Test
    public void getSummary_topLevelDeviceCategoryHasBoth_shouldIncludeBoth() {
        ShadowDesktopSettingsUtils.setShouldShow(true);
        setupController();
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true);
        when(mConnectedDisplayInjector.getDisplays()).thenReturn(createExternalDisplays());

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.device_display_dashboard_summary_with_touch_external));
    }

    private List<DisplayDevice> createExternalDisplays() {
        List<DisplayDevice> displays = new ArrayList<>();
        DisplayDevice externalDisplay = new DisplayDevice(
                0, "test_id", "test_name", null, new ArrayList<>(),
                DisplayIsEnabled.YES, true, 0, false);
        displays.add(externalDisplay);
        return displays;
    }
}
