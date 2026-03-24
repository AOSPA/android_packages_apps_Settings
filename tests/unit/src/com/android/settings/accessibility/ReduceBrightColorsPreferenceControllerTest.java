/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ReduceBrightColorsPreferenceController} */
@RunWith(AndroidJUnit4.class)
public class ReduceBrightColorsPreferenceControllerTest {
    private static final String PREF_KEY = "rbc_preference";

    private Context mContext;
    private Resources mResources;
    private ReduceBrightColorsPreferenceController mController;

    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mDisplayInfo.type = Display.TYPE_INTERNAL;
        Display display = new Display(new DisplayManagerGlobal(mock(IDisplayManager.class)),
                Display.DEFAULT_DISPLAY, mDisplayInfo, mResources);
        when(mContext.getDisplay()).thenReturn(display);
        mController = new ReduceBrightColorsPreferenceController(mContext,
                PREF_KEY);
    }

    @Test
    public void getSummary_returnSummary() {
        assertThat(mController.getSummary().toString().contains(
                resourceString("reduce_bright_colors_preference_summary"))).isTrue();
    }

    @Ignore("ColorDisplayManager runs in a different thread which results in a race condition")
    @Test
    public void isChecked_reduceBrightColorsIsActivated_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Ignore("ColorDisplayManager runs in a different thread which results in a race condition")
    @Test
    public void isChecked_reduceBrightColorsIsNotActivated_returnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isAvailable_whenEvenDimmerDisabled_RbcAvailable_returnTrue() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_RbcUnavailable_returnFalse() {
        doReturn(false).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenEvenDimmerEnabled_returnFalse() {
        doReturn(true).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_externalDisplay_returnFalse() {
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        mDisplayInfo.type = Display.TYPE_EXTERNAL;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mController.isAvailable()).isFalse();
    }

    private int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    private String resourceString(String name) {
        return mContext.getResources().getString(resourceId("string", name));
    }
}
