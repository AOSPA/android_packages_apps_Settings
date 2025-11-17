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

import static android.view.flags.Flags.enableVectorCursorA11ySettings;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.inputmethod.PointerScaleSliderController.POINTER_SCALE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SliderPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

/** Tests for {@link PointerScaleSliderController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class PointerScaleSliderControllerTest {

    private static final String PREFERENCE_KEY = "pointer_scale";

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceScreen mPreferenceScreen;

    private ShadowContentResolver mShadowContentResolver;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    private Context mContext;
    private SliderPreference mPreference;
    private PointerScaleSliderController mController;
    private FakeFeatureFactory mFeatureFactory;

    private static final int SLIDER_POSITION = 1;
    private static final float SLIDER_SCALE = 1.5f;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPreference = new SliderPreference(mContext);
        mController = new PointerScaleSliderController(mContext, PREFERENCE_KEY);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
        mShadowContentResolver = shadowOf(mContext.getContentResolver());
    }

    @Test
    public void getAvailabilityStatus_flagEnabled() {
        assumeTrue(enableVectorCursorA11ySettings());

        assertEquals(AVAILABLE, mController.getAvailabilityStatus());
    }

    @Test
    public void displayPreference_sliderPositionUpdated() {
        Settings.System.putFloatForUser(
                mContext.getContentResolver(),
                Settings.System.POINTER_SCALE, SLIDER_SCALE,
                UserHandle.USER_CURRENT);

        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);

        assertEquals(SLIDER_POSITION, mController.getSliderPosition(), /* delta= */ 0.001f);
    }

    @Test
    public void setSliderPosition_shouldReturnTrue_shouldUpdateValue() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);

        boolean result = mController.setSliderPosition(SLIDER_POSITION);

        assertThat(result).isTrue();
        assertThat(mController.getSliderPosition()).isEqualTo(SLIDER_POSITION);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(),
                eq(SettingsEnums.ACTION_POINTER_ICON_SCALE_CHANGED),
                eq(Float.toString(SLIDER_SCALE)));
    }

    @Test
    public void onStart_shouldRegisterContentObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mShadowContentResolver.getContentObservers(POINTER_SCALE_URI))
                .hasSize(1);
    }

    @Test
    public void onStop_shouldUnregisterContentObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED)))
                .isEmpty();
    }
}
