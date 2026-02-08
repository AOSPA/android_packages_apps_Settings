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

package com.android.settings.accessibility.captionpreferences.ui

import android.content.Context
import android.view.accessibility.CaptioningManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomCaptionOptionsPreferenceTest {

    private lateinit var context: Context
    private lateinit var captioningManager: CaptioningManager
    private val preference = CustomCaptionOptionsPreference()

    @Before
    fun setUp() {
        captioningManager = mock()
        context =
            spy(ApplicationProvider.getApplicationContext()) {
                on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
            }
    }

    @Test
    fun metadata_isCorrect() {
        assertThat(preference.key).isEqualTo(CustomCaptionOptionsPreference.KEY)
        assertThat(preference.title).isEqualTo(R.string.captioning_custom_options_title)
    }

    @Test
    fun isAvailable_returnsTrueWhenCustomStyle() {
        whenever(captioningManager.rawUserStyle)
            .thenReturn(CaptioningManager.CaptionStyle.PRESET_CUSTOM)
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_returnsFalseWhenNotCustomStyle() {
        whenever(captioningManager.rawUserStyle).thenReturn(0)
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun onCreate_registersListener() {
        val mockLifecycleContext: PreferenceLifecycleContext = mock {
            on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
        }

        preference.onCreate(mockLifecycleContext)

        verify(captioningManager).addCaptioningChangeListener(any())
    }

    @Test
    fun onDestroy_unregistersListener() {
        val mockLifecycleContext: PreferenceLifecycleContext = mock {
            on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
        }
        val listenerCaptor = argumentCaptor<CaptioningManager.CaptioningChangeListener>()

        preference.onCreate(mockLifecycleContext)
        verify(captioningManager).addCaptioningChangeListener(listenerCaptor.capture())

        preference.onDestroy(mockLifecycleContext)
        verify(captioningManager).removeCaptioningChangeListener(listenerCaptor.lastValue)
    }
}
