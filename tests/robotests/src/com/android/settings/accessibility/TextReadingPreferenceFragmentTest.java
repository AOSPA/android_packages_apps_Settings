/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.TextReadingPreferenceFragment.EXTRA_LAUNCHED_FROM;
import static com.android.settingslib.metadata.PreferenceScreenBindingKeyProviderKt.EXTRA_BINDING_SCREEN_KEY;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.accessibility.textreading.ui.TextReadingScreen;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenFromNotification;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenInAnythingElse;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenInSuw;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenOnAccessibility;

import com.google.testing.junit.testparameterinjector.TestParameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;

/** Tests for {@link TextReadingPreferenceFragment}. */
@RunWith(RobolectricTestParameterInjector.class)
public class TextReadingPreferenceFragmentTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new TextReadingPreferenceFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS);
    }

    @Test
    public void getPreferenceScreenResId_returnsZero() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(0);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("TextReadingPreferenceFragment");
    }

    @Test
    public void getPreferenceScreenBindingKey_fromCatalystScreen_returnsCorrectKey() {
        final String bindingKey = TextReadingScreen.KEY;
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_BINDING_SCREEN_KEY, bindingKey);
        mFragment.setArguments(bundle);

        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(bindingKey);
    }

    @TestParameters({
            "{entryPoint: " + EntryPoint.UNKNOWN_ENTRY + ", expectedScreenKey: "
                    + TextReadingScreen.KEY + "}",
            "{entryPoint: " + EntryPoint.SUW_VISION_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreenInSuw.KEY + "}",
            "{entryPoint: " + EntryPoint.SUW_ANYTHING_ELSE + ", expectedScreenKey: "
                    + TextReadingScreenInAnythingElse.KEY + "}",
            "{entryPoint: " + EntryPoint.HIGH_CONTRAST_TEXT_NOTIFICATION + ", expectedScreenKey: "
                    + TextReadingScreenFromNotification.KEY + "}",
            "{entryPoint: " + EntryPoint.ACCESSIBILITY_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreenOnAccessibility.KEY + "}",
            "{entryPoint: " + EntryPoint.DISPLAY_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreen.KEY + "}",
    })

    @Test
    public void getPreferenceScreenBindingKey_fromNonCatalystScreen(int entryPoint,
            String expectedScreenKey) {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_LAUNCHED_FROM, entryPoint);
        mFragment.setArguments(bundle);

        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(expectedScreenKey);
    }
}
