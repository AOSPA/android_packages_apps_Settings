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

/** Tests for {@link MouseKeysMaxSpeedController}. */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysMaxSpeedControllerTest {

    private static final String KEY_CUSTOM_SEEKBAR = "mouse_keys_max_speed_seekbar";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private LayoutPreference mLayoutPreference;
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    private ImageView mShorter;
    private ImageView mLonger;
    private SeekBar mSeekBar;
    private MouseKeysMaxSpeedController mController;

    @Before
    public void setUp() {
        mShorter = new ImageView(mContext);
        mLonger = new ImageView(mContext);
        mSeekBar = new SeekBar(mContext);
        mController = new MouseKeysMaxSpeedController(mContext, KEY_CUSTOM_SEEKBAR);
        doReturn(mLayoutPreference).when(mScreen).findPreference(KEY_CUSTOM_SEEKBAR);
        doReturn(mSeekBar).when(mLayoutPreference).findViewById(R.id.max_speed_seekbar);
        doReturn(mShorter).when(mLayoutPreference).findViewById(R.id.shorter);
        doReturn(mLonger).when(mLayoutPreference).findViewById(R.id.longer);
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
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, 5);
        mController.displayPreference(mScreen);

        assertThat(mSeekBar.getProgress()).isEqualTo(5);
    }

    @Test
    public void onSettingsChanged_updateMaxSpeedValue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, 5);

        mController.displayPreference(mScreen);
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, /* def= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(5);
        assertThat(actualDelayValue).isEqualTo(5);
    }

    @Test
    public void onSeekBarProgressChanged_updateMaxSpeedValue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, 5);
        mController.displayPreference(mScreen);
        // mController.mSeekBarChangeListener.onProgressChanged(mock(SeekBar.class),
        //         /* value= */ 8,
        //         true);
        mSeekBar.setProgress(8);
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, /* def= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(8);
        assertThat(actualDelayValue).isEqualTo(8);
    }

    @Test
    public void onShorterClicked_updateMaxSpeedValue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, 5);
        mController.displayPreference(mScreen);
        mShorter.callOnClick();
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, /* def= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(4);
        assertThat(actualDelayValue).isEqualTo(4);
    }

    @Test
    public void onLongerClicked_updateMaxSpeedValue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, 5);

        mController.displayPreference(mScreen);
        mLonger.callOnClick();
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_MAX_SPEED, /* def= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(6);
        assertThat(actualDelayValue).isEqualTo(6);
    }
}
