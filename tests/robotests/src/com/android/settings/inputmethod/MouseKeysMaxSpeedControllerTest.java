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

package com.android.settings.inputmethod;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MouseKeysMaxSpeedController}. */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysMaxSpeedControllerTest {

    private static final String KEY_CUSTOM_SLIDER = "mouse_keys_max_speed_slider";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private MouseKeysMaxSpeedController mController;
    Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        mController = new MouseKeysMaxSpeedController(mContext, KEY_CUSTOM_SLIDER);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_available_whenFlagOn() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_unavailable_whenFlagOff() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void setSliderPosition_maxspeedValue_shouldReturnTrue() {
        int position = 3;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isTrue();
        assertThat(mController.getSliderPosition())
                .isEqualTo(position);
    }

    @Test
    public void setSliderPosition_maxspeedValueOverMaxValue_shouldReturnFalse() {
        int position = mController.getMax() + 1;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition())
                .isEqualTo(InputSettings.DEFAULT_MOUSE_KEYS_MAX_SPEED);
    }

    @Test
    public void setSliderPosition_maxspeedValueBelowMinValue_shouldReturnFalse() {
        int position = mController.getMin() - 1;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition())
                .isEqualTo(InputSettings.DEFAULT_MOUSE_KEYS_MAX_SPEED);
    }
}
