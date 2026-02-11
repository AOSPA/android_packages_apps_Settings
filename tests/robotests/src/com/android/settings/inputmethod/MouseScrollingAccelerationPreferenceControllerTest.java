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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputSettings;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.hardware.input.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MouseScrollingAccelerationPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class MouseScrollingAccelerationPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "mouse_scrolling_acceleration";
    private static final String SETTING_KEY = Settings.System.MOUSE_SCROLLING_ACCELERATION;

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private FeatureFactory mFeatureFactory;

    private Context mContext;
    private MouseScrollingAccelerationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        when(mFeatureFactory.getMetricsFeatureProvider()).thenReturn(mMetricsFeatureProvider);
        FeatureFactory.setFactory(mContext, mFeatureFactory);
        mController =
                new MouseScrollingAccelerationPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void setChecked_false_enablesAccelerationAndLogsMetric() {
        mController.setChecked(false);

        boolean isEnabled = InputSettings.isMouseScrollingAccelerationEnabled(mContext);
        assertThat(isEnabled).isTrue();
        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_MOUSE_SCROLLING_ACCELERATION_ENABLED);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void setChecked_true_disablesAccelerationAndLogsMetric() {
        mController.setChecked(true);

        boolean isEnabled = InputSettings.isMouseScrollingAccelerationEnabled(mContext);
        assertThat(isEnabled).isFalse();
        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_MOUSE_SCROLLING_ACCELERATION_DISABLED);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void isChecked_providerPutInt1_returnFalse() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                1,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_SCROLLING_ACCELERATION)
    public void isChecked_providerPutInt0_returnTrue() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isTrue();
    }

    @Test
    public void getSliceHighlightMenuRes_returnsCorrectMenuKey() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.menu_key_system);
    }
}
