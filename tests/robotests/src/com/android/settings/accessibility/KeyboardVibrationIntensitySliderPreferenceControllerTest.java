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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.os.vibrator.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SliderPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Test for {@link KeyboardVibrationIntensitySliderPreferenceController}. */
// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class, SettingsShadowResources.class})
public class KeyboardVibrationIntensitySliderPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private PreferenceScreen mScreen;
    @Mock private AudioManager mAudioManager;

    private Lifecycle mLifecycle;
    private Context mContext;
    private Vibrator mVibrator;
    private KeyboardVibrationIntensitySliderPreferenceController mController;
    private SliderPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        mVibrator = mContext.getSystemService(Vibrator.class);
        mController =
                new KeyboardVibrationIntensitySliderPreferenceController(
                        mContext, PREFERENCE_KEY, Vibrator.VIBRATION_INTENSITY_HIGH);
        mLifecycle.addObserver(mController);
        mPreference = new SliderPreference(mContext);
        mPreference.setSummary("Test summary");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
        assertThat(mController.getMin()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(mController.getMax()).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);
    }

    @Test
    public void getAvailabilityStatus_bothConfigsAndFlagTrue_isAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported, true);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported,
                true);
        mSetFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagFalse_isUnsupported() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported, true);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported,
                true);
        mSetFlagsRule.disableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_configSupportedFalse_isUnsupported() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported, false);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported,
                true);
        mSetFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_configIntensitySupportedFalse_isUnsupported() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported, true);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported,
                false);
        mSetFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void missingSetting_shouldReturnDefault() {
        Settings.System.putString(
                mContext.getContentResolver(),
                Settings.System.KEYBOARD_VIBRATION_INTENSITY,
                /* value= */ null);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue())
                .isEqualTo(
                        mVibrator.getDefaultVibrationIntensity(
                                VibrationAttributes.USAGE_IME_FEEDBACK));
    }

    @Test
    public void updateState_shouldDisplayIntensityInSliderPosition() {
        updateSetting(
                Settings.System.KEYBOARD_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);

        updateSetting(
                Settings.System.KEYBOARD_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        updateSetting(
                Settings.System.KEYBOARD_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(
                Settings.System.KEYBOARD_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Test
    public void setSliderPosition_updatesIntensitySetting() throws Exception {
        mController.setSliderPosition(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(readSetting(Settings.System.KEYBOARD_VIBRATION_INTENSITY))
                .isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);

        mController.setSliderPosition(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(readSetting(Settings.System.KEYBOARD_VIBRATION_INTENSITY))
                .isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        mController.setSliderPosition(Vibrator.VIBRATION_INTENSITY_MEDIUM);
        assertThat(readSetting(Settings.System.KEYBOARD_VIBRATION_INTENSITY))
                .isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        mController.setSliderPosition(Vibrator.VIBRATION_INTENSITY_HIGH);
        assertThat(readSetting(Settings.System.KEYBOARD_VIBRATION_INTENSITY))
                .isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSetting(String settingKey) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), settingKey);
    }
}
// LINT.ThenChange(KeyboardVibrationIntensitySliderPreferenceTest.kt)
