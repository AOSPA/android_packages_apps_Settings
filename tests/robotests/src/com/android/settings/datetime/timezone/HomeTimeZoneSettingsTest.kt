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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.datetime.timezone.model.TimeZoneData
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeTimeZoneSettingsTest {

    @get:Rule val mMockitoRule = MockitoJUnit.rule()

    private lateinit var mContext: Context
    private lateinit var mHomeTimeZoneSettings: HomeTimeZoneSettings

    @Mock private lateinit var mTimeZoneData: TimeZoneData

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mHomeTimeZoneSettings = spy(HomeTimeZoneSettings())

        doNothing().`when`(mHomeTimeZoneSettings).finish()
        `when`(mHomeTimeZoneSettings.context).thenReturn(mContext)
        `when`(mHomeTimeZoneSettings.requireContext()).thenReturn(mContext)

        mHomeTimeZoneSettings.mTimeZoneData = mTimeZoneData
    }

    @Test
    fun isHomeTimeZoneEnabled_nullTimeZone_shouldReturnTrue() {
        mHomeTimeZoneSettings.mSelectedTimeZoneId = null

        assertThat(mHomeTimeZoneSettings.isHomeTimeZoneEnabled()).isTrue()
    }

    @Test
    fun isHomeTimeZoneEnabled_emptyTimeZone_shouldReturnFalse() {
        mHomeTimeZoneSettings.mSelectedTimeZoneId = ""

        assertThat(mHomeTimeZoneSettings.isHomeTimeZoneEnabled()).isFalse()
    }

    @Test
    fun isHomeTimeZoneEnabled_validTimeZone_shouldReturnTrue() {
        mHomeTimeZoneSettings.mSelectedTimeZoneId = "America/Los_Angeles"

        assertThat(mHomeTimeZoneSettings.isHomeTimeZoneEnabled()).isTrue()
    }
}
