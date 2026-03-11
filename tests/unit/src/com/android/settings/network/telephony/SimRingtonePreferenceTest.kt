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

package com.android.settings.network.telephony

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SimRingtonePreferenceTest {

    private val mockTelecomManager = mock<TelecomManager>()
    private val mockPreference = mock<Preference>()
    private val mockLifecycleContext = mock<PreferenceLifecycleContext>()
    private val mockLauncher = mock<ActivityResultLauncher<Intent>>()
    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.TELECOM_SERVICE -> mockTelecomManager
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = mockResources
        }

    private val subId = 1
    private lateinit var preference: SimRingtonePreference

    @Before
    fun setUp() {
        preference = SimRingtonePreference(context, subId)

        mockPreference.stub {
            on { context } doReturn context
            on { key } doReturn SimRingtonePreference.KEY
        }

        mockLifecycleContext.stub {
            on { requirePreference<Preference>(SimRingtonePreference.KEY) } doReturn mockPreference
            on {
                registerForActivityResult(
                    any<ActivityResultContracts.StartActivityForResult>(),
                    any(),
                )
            } doReturn mockLauncher
        }
    }

    @Test
    fun key_isCorrect() {
        assertThat(preference.key).isEqualTo(KEY)
    }

    @Test
    fun title_isCorrect() {
        assertThat(context.getString(preference.title))
            .isEqualTo(context.getString(R.string.sim_ringtone_title))
    }

    @Test
    fun isAvailable_whenFlagDisabled_returnsFalse() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_specific_ringtone) } doReturn false
        }
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun onCreate_setsPreferenceClickListener() {
        preference.onCreate(mockLifecycleContext)
        verify(mockPreference).onPreferenceClickListener = any()
    }

    companion object {
        const val KEY = "sim_ringtone_info"
    }
}
