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

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.Resources;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend;
import com.google.testing.junit.testparameterinjector.TestParameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;

import java.util.Set;

@RunWith(RobolectricTestParameterInjector.class)
public class DreamUtilsTest {
    enum BatteryConfigTestCase {
        ENABLED(true, R.array.when_to_start_screensaver_entries,
                R.array.when_to_start_screensaver_values),
        DISABLED(false, R.array.when_to_start_screensaver_entries_no_battery,
                R.array.when_to_start_screensaver_values_no_battery);

        final boolean batteryEnabled;
        final int entriesResId;
        final int keysResId;

        BatteryConfigTestCase(boolean batteryEnabled, int entriesResId, int keysResId) {
            this.batteryEnabled = batteryEnabled;
            this.entriesResId = entriesResId;
            this.keysResId = keysResId;
        }
    }

    @Test
    public void getWhenToDreamEntries_returnsCorrectEntries(
            @TestParameter BatteryConfigTestCase testCase) {
        Resources resources = mock(Resources.class);
        when(resources.getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery))
                .thenReturn(testCase.batteryEnabled);
        String[] expected = new String[]{"entry1", "entry2"};
        when(resources.getStringArray(testCase.entriesResId))
                .thenReturn(expected);

        String[] actual = DreamUtils.getWhenToDreamEntries(resources);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getWhenToDreamKeys_returnsCorrectKeys(
            @TestParameter BatteryConfigTestCase testCase) {
        Resources resources = mock(Resources.class);
        when(resources.getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery))
                .thenReturn(testCase.batteryEnabled);
        String[] expected = new String[]{"key1", "key2"};
        when(resources.getStringArray(testCase.keysResId))
                .thenReturn(expected);

        String[] actual = DreamUtils.getWhenToDreamKeys(resources);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getWhenToDreamOptions_returnsMappedOptions(
            @TestParameter BatteryConfigTestCase testCase) {
        Resources resources = mock(Resources.class);
        when(resources.getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery))
                .thenReturn(testCase.batteryEnabled);

        // Using real keys from DreamSettings to ensure mapping works.
        // We use a subset or all depending on what we want to simulate.
        // Here we just check that the keys returned by getWhenToDreamKeys are mapped correctly.
        String[] keys = new String[]{
                DreamSettings.WHILE_CHARGING_ONLY,
                DreamSettings.WHILE_DOCKED_ONLY,
                DreamSettings.NEVER_DREAM
        };

        when(resources.getStringArray(testCase.keysResId))
                .thenReturn(keys);

        int[] actual = DreamUtils.getWhenToDreamOptions(resources);

        assertThat(actual).hasLength(3);
        assertThat(actual[0]).isEqualTo(DreamBackend.WHILE_CHARGING);
        assertThat(actual[1]).isEqualTo(DreamBackend.WHILE_DOCKED);
        assertThat(actual[2]).isEqualTo(DreamBackend.NEVER);
    }

    @Test
    public void getLowLightBehaviors_returnsIntersection() {
        Resources resources = mock(Resources.class);
        String[] supported = new String[]{"behavior1", "behavior2", "behavior3"};
        String[] available = new String[]{"behavior2", "behavior3", "behavior4"};

        when(resources.getStringArray(R.array.low_light_display_behavior_supported_values))
                .thenReturn(supported);
        when(resources.getStringArray(R.array.low_light_display_behavior_values))
                .thenReturn(available);

        Set<String> actual = DreamUtils.getLowLightBehaviors(resources);

        assertThat(actual).containsExactly("behavior2", "behavior3");
    }

    @Test
    public void getLowLightBehaviors_noIntersection_returnsEmpty() {
        Resources resources = mock(Resources.class);
        String[] supported = new String[]{"behavior1", "behavior2"};
        String[] available = new String[]{"behavior3", "behavior4"};

        when(resources.getStringArray(R.array.low_light_display_behavior_supported_values))
                .thenReturn(supported);
        when(resources.getStringArray(R.array.low_light_display_behavior_values))
                .thenReturn(available);

        Set<String> actual = DreamUtils.getLowLightBehaviors(resources);

        assertThat(actual).isEmpty();
    }
}
