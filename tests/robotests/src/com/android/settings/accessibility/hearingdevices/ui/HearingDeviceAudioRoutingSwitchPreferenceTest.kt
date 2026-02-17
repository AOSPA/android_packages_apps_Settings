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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.media.AudioAttributes
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBluetoothUtils::class])
class HearingDeviceAudioRoutingSwitchPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val key = "test_key"
    private val settingsKey = "Settings.Secure.HEARING_AID_RINGTONE_ROUTING"
    private val testTitleRes = R.string.accessibility_hearing_device_notification_routing_title
    private val testSummaryOnRes =
        R.string.accessibility_hearing_device_routing_hearing_device_summary
    private val testSummaryOffRes =
        R.string.accessibility_hearing_device_routing_device_speaker_summary
    private val testPurposeRes = R.string.hearing_device_notification_routing_purpose
    private val audioAttributeUsages = intArrayOf(AudioAttributes.USAGE_NOTIFICATION)

    private val preference =
        HearingDeviceAudioRoutingSwitchPreference(
            context,
            key,
            settingsKey,
            testTitleRes,
            testSummaryOnRes,
            testSummaryOffRes,
            testPurposeRes,
            audioAttributeUsages,
        )

    @Test
    fun bind_checkedValue_setSummaryOnRes() {
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        widget.isChecked = true

        preference.bind(widget, preference)

        assertThat(widget.summary).isEqualTo(context.getString(testSummaryOnRes))
    }

    @Test
    fun bind_uncheckedValue_setSummaryOffRes() {
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        widget.isChecked = false

        preference.bind(widget, preference)

        assertThat(widget.summary).isEqualTo(context.getString(testSummaryOffRes))
    }

    @Test
    fun onPreferenceChange_checkedValue_updateSummaryOnRes() {
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        preference.bind(widget, preference)

        widget.callChangeListener(true)

        assertThat(widget.summary).isEqualTo(context.getString(testSummaryOnRes))
    }

    @Test
    fun onPreferenceChange_uncheckedValue_updatesSummaryOffRes() {
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        preference.bind(widget, preference)

        widget.callChangeListener(false)

        assertThat(widget.summary).isEqualTo(context.getString(testSummaryOffRes))
    }
}
