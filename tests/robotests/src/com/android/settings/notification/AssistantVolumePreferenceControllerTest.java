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

package com.android.settings.notification;

import static android.media.audio.Flags.FLAG_STREAM_ASSISTANT_PUBLIC;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
public class AssistantVolumePreferenceControllerTest {
    @Rule
    public SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private AudioHelper mHelper;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private AssistantVolumePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new AssistantVolumePreferenceController(mContext);
        mController.setAudioHelper(mHelper);
    }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    public void isAvailable_singleVolume_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    public void isAvailable_onWatch_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @DisableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    public void isAvailable_flagDisabled_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    public void isAvailable_notSingleVolume_notWatch_shouldReturnTrue() {
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAudioStream_shouldReturnAssistant() {
        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_ASSISTANT);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
// LINT.ThenChange(AssistantVolumePreferenceTest.kt)
