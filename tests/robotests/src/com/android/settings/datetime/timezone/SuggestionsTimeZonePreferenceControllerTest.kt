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

package com.android.settings.datetime.timezone

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SuggestionsTimeZonePreferenceControllerTest {

    @get:Rule val mMockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mTimeZoneSuggestor: HomeTimeZoneSuggestor
    @Mock private lateinit var mPreferenceScreen: PreferenceScreen
    @Mock private lateinit var mPreferenceCategory: PreferenceCategory
    @Mock private lateinit var mHomeTimeZoneSettings: HomeTimeZoneSettings

    private lateinit var mContext: Context
    private lateinit var mController: SuggestionsTimeZonePreferenceController

    @Before
    fun setUp() {
        mContext = RuntimeEnvironment.getApplication()
        mController = SuggestionsTimeZonePreferenceController(mContext, "test_key")
        mController.setParentFragment(mHomeTimeZoneSettings)
        mController.homeTimeZoneSuggestor = mTimeZoneSuggestor

        `when`(mPreferenceScreen.findPreference<PreferenceCategory>(mController.preferenceKey))
            .thenReturn(mPreferenceCategory)
    }

    @Test
    fun getAvailabilityStatus_homeTimeZoneDisabled_isAvailable() {
        `when`(mHomeTimeZoneSettings.isHomeTimeZoneEnabled()).thenReturn(false)
        `when`(mTimeZoneSuggestor.getTimeZoneSuggestions())
            .thenReturn(listOf(mock(TimeZoneInfo::class.java)))

        assertThat(mController.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_withSuggestions_shouldBeAvailable() {
        `when`(mHomeTimeZoneSettings.isHomeTimeZoneEnabled()).thenReturn(true)
        `when`(mTimeZoneSuggestor.getTimeZoneSuggestions())
            .thenReturn(listOf(mock(TimeZoneInfo::class.java)))
        assertThat(mController.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun displayPreference_shouldAddPreferencesToCategory() {
        val tzInfo = mock(TimeZoneInfo::class.java)
        `when`(tzInfo.id).thenReturn("id")
        `when`(tzInfo.genericName).thenReturn("name")
        `when`(tzInfo.exemplarLocation).thenReturn("location")
        `when`(mTimeZoneSuggestor.getTimeZoneSuggestions()).thenReturn(listOf(tzInfo))
        mController.displayPreference(mPreferenceScreen)
        verify(mPreferenceCategory).addPreference(any(Preference::class.java))
    }
}
