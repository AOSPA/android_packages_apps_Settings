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

package com.android.settings.accessibility;

import static android.view.accessibility.Flags.FLAG_FORCE_INVERT_COLOR;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ForceInvertPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ForceInvertPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ForceInvertPreferenceController mController;
    private SelectorWithWidgetPreference mStandardDarkThemePreference;
    private SelectorWithWidgetPreference mExpandedDarkThemePreference;

    @Before
    public void setUp() {
        mController = new ForceInvertPreferenceController(mContext, "dark_theme_group");
        mStandardDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mStandardDarkThemePreference
                .setKey(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY);
        mExpandedDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mExpandedDarkThemePreference
                .setKey(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY))
                .thenReturn(mStandardDarkThemePreference);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY))
                .thenReturn(mExpandedDarkThemePreference);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOff_shouldReturnUnsupported() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOn_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void settingOff_reflectsCorrectValue() {
        setEnabled(false);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
    }

    @Test
    public void settingOn_reflectsCorrectValue() {
        setEnabled(true);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
    }

    @Test
    public void onRadioButtonClicked_standardDarkTheme_settingChanges() {
        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(mStandardDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def=*/ -1) == ON;
        assertThat(isForceInvertEnabled).isFalse();
    }

    @Test
    public void onRadioButtonClicked_expandedDarkTheme_settingChanges() {
        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def=*/ -1) == ON;
        assertThat(isForceInvertEnabled).isTrue();
    }

    private void setEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, enabled ? ON : OFF);
    }
}
