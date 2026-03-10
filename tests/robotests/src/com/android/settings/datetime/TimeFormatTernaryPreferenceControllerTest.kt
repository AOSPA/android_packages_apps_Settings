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

package com.android.settings.datetime

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TimeFormatTernaryPreferenceControllerTest {

  @Mock private lateinit var mCallback: UpdateTimeAndDateCallback

  private lateinit var mContext: Context
  private lateinit var mController: TimeFormatTernaryPreferenceController
  private lateinit var mPreference: Preference

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    mContext = RuntimeEnvironment.application
    mController = TimeFormatTernaryPreferenceController(mContext, "test_key")
    mController.setTimeAndDateCallback(mCallback)
    mController.setFromSUW(false)
    mPreference = Preference(mContext)
    mPreference.key = "test_key"
  }

  @Test
  fun isCalledFromSUW_notAvailable() {
    mController.setFromSUW(true)
    assertThat(mController.availabilityStatus)
      .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING)
  }

  @Test
  fun notCalledFromSUW_shouldBeAvailable() {
    assertThat(mController.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
  }

  @Test
  fun updateState_shouldEnablePreference() {
    mController.updateState(mPreference)
    assertThat(mPreference.isEnabled).isTrue()
  }

  @Test
  fun getSummary_auto_shouldReturnAutoSummary() {
    Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, null)
    assertThat(mController.summary).isEqualTo(mContext.getString(R.string.time_format_automatic))
  }

  @Test
  fun getSummary_12Hour_shouldReturn12HourSummary() {
    Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, "12")
    assertThat(mController.summary).isEqualTo(mContext.getString(R.string.time_format_12_hour))
  }

  @Test
  fun getSummary_24Hour_shouldReturn24HourSummary() {
    Settings.System.putString(mContext.contentResolver, Settings.System.TIME_12_24, "24")
    assertThat(mController.summary).isEqualTo(mContext.getString(R.string.time_format_24_hour))
  }

  @Test
  fun update24HourFormat_auto_shouldSendIntent() {
    TimeFormatTernaryPreferenceController.update24HourFormat(mContext, null)
    val intents =
      shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
        .broadcastIntents
    assertThat(intents.size).isEqualTo(1)
    val intent = intents[0]
    assertThat(intent.action).isEqualTo(Intent.ACTION_TIME_CHANGED)
    assertThat(intent.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
      .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT)
  }

  @Test
  fun update24HourFormat_12Hour_shouldSendIntent() {
    TimeFormatTernaryPreferenceController.update24HourFormat(mContext, "12")
    val intents =
      shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
        .broadcastIntents
    assertThat(intents.size).isEqualTo(1)
    val intent = intents[0]
    assertThat(intent.action).isEqualTo(Intent.ACTION_TIME_CHANGED)
    assertThat(intent.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
      .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR)
  }

  @Test
  fun update24HourFormat_24Hour_shouldSendIntent() {
    TimeFormatTernaryPreferenceController.update24HourFormat(mContext, "24")
    val intents =
      shadowOf(ApplicationProvider.getApplicationContext() as android.app.Application)
        .broadcastIntents
    assertThat(intents.size).isEqualTo(1)
    val intent = intents[0]
    assertThat(intent.action).isEqualTo(Intent.ACTION_TIME_CHANGED)
    assertThat(intent.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
      .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR)
  }
}
