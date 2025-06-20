/*
 * Copyright 2025 The Android Open Source Project
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

import static com.android.settings.flags.Flags.FLAG_FIX_TOUCHPAD_AND_MOUSE_SETTINGS_SEARCH_INDEX;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.InputDevice;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.keyboard.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/** Tests for {@link TouchpadAndMouseSettings} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
        ShadowInputDevice.class,
})
public class TouchpadAndMouseSettingsTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        addTouchpad();
    }

    private void addTouchpad() {
        ShadowInputDevice.reset();

        int deviceId = 1;
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_TOUCHPAD);
        ShadowInputDevice.addDevice(deviceId, device);
    }

    @Test
    @DisableFlags({FLAG_FIX_TOUCHPAD_AND_MOUSE_SETTINGS_SEARCH_INDEX,
            Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED})
    public void withoutIndexFix_newPageEnabled_pageIndexIncluded() {
        assertIsPageSearchEnabled(true);
    }

    @Test
    @EnableFlags(FLAG_FIX_TOUCHPAD_AND_MOUSE_SETTINGS_SEARCH_INDEX)
    @DisableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void withIndexFix_newPageDisabled_pageIndexIncluded() {
        assertIsPageSearchEnabled(true);
    }

    @Test
    @EnableFlags({FLAG_FIX_TOUCHPAD_AND_MOUSE_SETTINGS_SEARCH_INDEX,
            Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED})
    public void withIndexFix_newPageEnabled_pageIndexExcluded() {
        assertIsPageSearchEnabled(false);
    }

    private void assertIsPageSearchEnabled(boolean expectedResult) {
        final BaseSearchIndexProvider searchIndexProvider =
                TouchpadAndMouseSettings.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj =
                org.robolectric.util.ReflectionHelpers.callInstanceMethod(
                        searchIndexProvider, /*methodName=*/ "isPageSearchEnabled",
                        ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isEqualTo(expectedResult);
    }
}
