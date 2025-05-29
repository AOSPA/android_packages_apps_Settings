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

package com.android.settings.display;

import static android.provider.Settings.Secure.HDR_BRIGHTNESS_BOOST_LEVEL;

import static org.junit.Assert.assertEquals;

import android.provider.Settings;
import android.testing.TestableContext;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HdrBrightnessLevelPreferenceControllerTest {
    private HdrBrightnessLevelPreferenceController mPreferenceController;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getContext());

    @Before
    public void setUp() {
        mPreferenceController = new HdrBrightnessLevelPreferenceController(mContext, "test");
    }

    @Test
    public void getSliderPosition() {
        float value = 0.39f;
        Settings.Secure.putFloat(mContext.getContentResolver(), HDR_BRIGHTNESS_BOOST_LEVEL, value);

        assertEquals(Math.round(value * mPreferenceController.getMax()),
                mPreferenceController.getSliderPosition());
    }

    @Test
    public void setSliderPosition() {
        int position = mPreferenceController.getMax() / 3;
        mPreferenceController.setSliderPosition(position);

        assertEquals((float) position / mPreferenceController.getMax(),
                Settings.Secure.getFloat(mContext.getContentResolver(),
                        HDR_BRIGHTNESS_BOOST_LEVEL, /* def= */ 1), /* delta= */ 0);
    }
}
