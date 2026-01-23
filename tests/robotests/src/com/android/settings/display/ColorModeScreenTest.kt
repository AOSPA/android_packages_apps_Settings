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

package com.android.settings.display

import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.ColorDisplayManager
import android.provider.Settings.Secure
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings.ColorModeActivity
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ColorModeScreenTest {
    private val preferenceScreenCreator = ColorModeScreen()
    private val mockColorDisplayManager = mock<ColorDisplayManager>()
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? = mockColorDisplayManager
        }

    @Before
    fun setUp() {
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0)
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0)
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ColorModeScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ColorModeActivity::class.java.getName())
    }

    @Test
    fun isAvailable_deviceNotColorManaged_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_accessibilityInversionEnabled_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 1)

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_accessibilityDaltonizerEnabled_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1)

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_deviceColorManagedAccessibilityFeaturesDisabled_returnsTrue() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(true)
    }
}
