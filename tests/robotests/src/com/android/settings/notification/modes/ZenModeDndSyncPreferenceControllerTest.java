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
package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ZenModeDndSyncPreferenceControllerTest {
    @Rule public final MockitoRule mRule = MockitoJUnit.rule();

    @Mock private ZenModesBackend mBackend;
    @Mock private SwitchPreferenceCompat mPreference;
    private ZenModeDndSyncPreferenceController mController;
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();

        mController = new ZenModeDndSyncPreferenceController(mContext, "something", mBackend);
    }

    @Test
    public void modeSyncUnsupported_notAvailable() {
        when(mBackend.isModeSyncSupported()).thenReturn(false);
        ZenMode dnd = TestModeBuilder.MANUAL_DND;

        assertThat(mController.isAvailable(dnd)).isFalse();
    }

    @Test
    public void modeSyncSupported_notDnd_notAvailable() {
        when(mBackend.isModeSyncSupported()).thenReturn(true);
        ZenMode mode = TestModeBuilder.EXAMPLE;

        assertThat(mController.isAvailable(mode)).isFalse();
    }

    @Test
    public void modeSyncSupported_dnd_available() {
        when(mBackend.isModeSyncSupported()).thenReturn(true);
        ZenMode dnd = TestModeBuilder.MANUAL_DND;

        assertThat(mController.isAvailable(dnd)).isTrue();
    }

    @Test
    public void updateState_syncEnabled_checked() {
        when(mBackend.isModeSyncEnabled()).thenReturn(true);
        ZenMode dnd = TestModeBuilder.MANUAL_DND;

        mController.updateState(mPreference, dnd);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_syncDisabled_unchecked() {
        when(mBackend.isModeSyncEnabled()).thenReturn(false);
        ZenMode dnd = TestModeBuilder.MANUAL_DND;

        mController.updateState(mPreference, dnd);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChecked_enableModeSync() {
        mController.onPreferenceChange(mPreference, true);

        verify(mBackend).setModeSyncEnabled(true);
    }

    @Test
    public void onPreferenceUnchecked_disableModeSync() {
        mController.onPreferenceChange(mPreference, false);

        verify(mBackend).setModeSyncEnabled(false);
    }
}
