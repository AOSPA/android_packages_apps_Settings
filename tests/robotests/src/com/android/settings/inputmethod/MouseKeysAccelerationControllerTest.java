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

import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MouseKeysAccelerationController}. */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysAccelerationControllerTest {

    private static final String KEY_CUSTOM_SEEKBAR = "mouse_keys_acceleration_seekbar";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private PreferenceScreen mScreen;
    @Mock private LayoutPreference mLayoutPreference;
    @Spy private Context mContext = ApplicationProvider.getApplicationContext();
    private ImageView mSlow;
    private ImageView mFast;
    private SeekBar mSeekBar;
    private MouseKeysAccelerationController mController;

    @Before
    public void setUp() {
        mSlow = new ImageView(mContext);
        mFast = new ImageView(mContext);
        mSeekBar = new SeekBar(mContext);
        mController = new MouseKeysAccelerationController(mContext, KEY_CUSTOM_SEEKBAR);
        doReturn(mLayoutPreference).when(mScreen).findPreference(KEY_CUSTOM_SEEKBAR);
        doReturn(mSeekBar)
                .when(mLayoutPreference)
                .findViewById(R.id.mouse_keys_acceleration_seekbar);
        doReturn(mSlow).when(mLayoutPreference).findViewById(R.id.slow_icon);
        doReturn(mFast).when(mLayoutPreference).findViewById(R.id.fast_icon);
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
    public void displayPreference_initSeekBar() {
        Settings.Secure.putFloat(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                .5f);
        mController.displayPreference(mScreen);

        assertThat(mSeekBar.getProgress()).isEqualTo(5);
    }

    @Test
    public void onSettingsChanged_updateAccelerationValue() {
        Settings.Secure.putFloat(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                .5f);

        mController.displayPreference(mScreen);
        final float actualAccelerationValue =
                Settings.Secure.getFloat(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                        /* def= */ 0.0f);

        assertThat(mSeekBar.getProgress()).isEqualTo(5);
        assertThat(actualAccelerationValue).isEqualTo(.5f);
    }

    @Test
    public void onSeekBarProgressChanged_updateAccelerationValue() {
        Settings.Secure.putFloat(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                .5f);
        mController.displayPreference(mScreen);
        mSeekBar.setProgress(8);
        final float actualAccelerationValue =
                Settings.Secure.getFloat(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                        /* def= */ 0.0f);

        assertThat(mSeekBar.getProgress()).isEqualTo(8);
        assertThat(actualAccelerationValue).isEqualTo(.8f);
    }

    @Test
    public void onDecreaseClicked_updateAccelerationValue() {
        Settings.Secure.putFloat(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                .5f);
        mController.displayPreference(mScreen);
        mSlow.callOnClick();
        final float actualAccelerationValue =
                Settings.Secure.getFloat(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                        /* def= */ 0.0f);

        assertThat(mSeekBar.getProgress()).isEqualTo(4);
        assertThat(actualAccelerationValue).isEqualTo(.4f);
    }

    @Test
    public void onIncreaseClicked_updateAccelerationValue() {
        Settings.Secure.putFloat(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                .5f);

        mController.displayPreference(mScreen);
        mFast.callOnClick();
        final float actualAccelerationValue =
                Settings.Secure.getFloat(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION,
                        /* def= */ 0.0f);

        assertThat(mSeekBar.getProgress()).isEqualTo(6);
        assertThat(actualAccelerationValue).isEqualTo(.6f);
    }
}
