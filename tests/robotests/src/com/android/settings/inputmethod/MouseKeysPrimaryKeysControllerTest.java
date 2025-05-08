/*
 * Copyright 2025 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MouseKeysPrimaryKeysController} */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysPrimaryKeysControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "mouse_key_use_primary_key_preference";

    private Context mContext;
    private MouseKeysPrimaryKeysController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new MouseKeysPrimaryKeysController(
                mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}
