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

package com.android.settings.notification.modes;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.flags.Flags.FLAG_HIDE_MODES_SETTING_IN_DEMO_MODE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class, ShadowUserManager.class})
public class ZenModesLinkPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ZenModesLinkPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new ZenModesLinkPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_notInDemoMode_returnsAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(FLAG_HIDE_MODES_SETTING_IN_DEMO_MODE)
    public void getAvailabilityStatus_inDemoMode_returnsUnsupported() {
        // Put the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);
        SettingsShadowResources.overrideResource(
                com.android.settings.R.bool.config_hide_modes_setting_in_demo_mode, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);

        // Clean up to the default value.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }
}
