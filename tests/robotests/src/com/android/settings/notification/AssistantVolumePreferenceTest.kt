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

package com.android.settings.notification

import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.audio.Flags.FLAG_STREAM_ASSISTANT_PUBLIC
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class AssistantVolumePreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val audioHelper = mock<AudioHelper>()
    private val mockPackageManager = mock<PackageManager>()
    private val assistantVolumePreference = AssistantVolumePreference(audioHelper)
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager(): PackageManager = mockPackageManager
        }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    fun isAvailable_onWatch_shouldReturnFalse() {
        audioHelper.stub { on { isSingleVolume } doReturn false }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_WATCH) } doReturn true
        }

        assertThat(assistantVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    fun isAvailable_singleVolume_shouldReturnFalse() {
        audioHelper.stub { on { isSingleVolume } doReturn true }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_WATCH) } doReturn false
        }

        assertThat(assistantVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    fun isAvailable_flagDisabled_shouldReturnFalse() {
        audioHelper.stub { on { isSingleVolume } doReturn false }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_WATCH) } doReturn false
        }

        assertThat(assistantVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(FLAG_STREAM_ASSISTANT_PUBLIC)
    fun isAvailable_flagEnabledAndNotSingleVolumeAndNotWatch_shouldReturnTrue() {
        audioHelper.stub { on { isSingleVolume } doReturn false }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_WATCH) } doReturn false
        }

        assertThat(assistantVolumePreference.isAvailable(context)).isTrue()
    }
}
// LINT.ThenChange(AssistantVolumePreferenceControllerTest.java)
