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
import android.view.View
import android.view.accessibility.CaptioningManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.widget.LayoutPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptionAppearancePreviewPreferenceTest {

    private val captioningManager: CaptioningManager = mock()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()).stub {
            on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
        }
    private val preference = CaptionAppearancePreviewPreference(context)

    @Test
    fun key_isCorrect() {
        assertThat(preference.key).isEqualTo(CaptionAppearancePreviewPreference.KEY)
    }

    @Test
    fun createWidget_returnsLayoutPreferenceWithCorrectLayout() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(LayoutPreference::class.java)
        // Implicitly assert we're using the correct layout - R.layout.captioning_preview
        // by asserting that view with id - preview_viewport - exists.
        // This is due to the limitation on the LayoutPreference's implementation.
        widget as LayoutPreference
        assertThat(widget.findViewById<View>(R.id.preview_viewport)).isNotNull()
        assertThat(widget.isSelectable).isFalse()
    }

    @Test
    fun onCreate_registersCaptioningChangeListener() {
        val mockLifecycleContext: PreferenceLifecycleContext = mock {
            on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
        }

        preference.onCreate(mockLifecycleContext)

        verify(captioningManager).addCaptioningChangeListener(any())
    }

    @Test
    fun onDestroy_unregistersCaptioningChangeListener() {
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
