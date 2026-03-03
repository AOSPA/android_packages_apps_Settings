/*
 * Copyright 2024 The Android Open Source Project
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
import android.hardware.input.InputSettings;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MousePointerAccelerationPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class MousePointerAccelerationPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "mouse_pointer_acceleration";
    private static final String SETTING_KEY = Settings.System.MOUSE_POINTER_ACCELERATION_ENABLED;

    private Context mContext;
    private MousePointerAccelerationPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new MousePointerAccelerationPreferenceController(
                mContext, PREFERENCE_KEY);
    }

    @Test
    public void setChecked_true_shouldReturnTrue() {
        mController.setChecked(true);

        boolean isEnabled = InputSettings.isMousePointerAccelerationEnabled(mContext);
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void setChecked_false_shouldReturnFalse() {
        mController.setChecked(false);

        boolean isEnabled = InputSettings.isMousePointerAccelerationEnabled(mContext);
        assertThat(isEnabled).isFalse();
    }

    @Test
    public void isChecked_providerPutInt1_returnTrue() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                1,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isTrue();
    }

    @Test
    public void isChecked_providerPutInt0_returnFalse() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isFalse();
    }
}
