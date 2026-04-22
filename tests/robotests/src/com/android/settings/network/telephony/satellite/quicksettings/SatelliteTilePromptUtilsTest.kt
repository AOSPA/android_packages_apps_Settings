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

package com.android.settings.network.telephony.satellite.quicksettings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.SharedPreferences
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SatelliteTilePromptUtilsTest {
    @get:Rule val mocks = MockitoJUnit.rule()
    @get:Rule val metricsRule = MetricsRule()

    private lateinit var context: Context
    @Mock private lateinit var mockSharedPreferences: SharedPreferences
    @Mock private lateinit var mockEditor: SharedPreferences.Editor
    @Mock private lateinit var mockNotificationManager: NotificationManager
    private lateinit var satelliteTilePromptUtils: SatelliteTilePromptUtils

    companion object {
        private const val PREF_PROMPT_COUNT = "satellite_tile_prompt_count"
        private const val PREF_LAST_PROMPT_TIMESTAMP = "satellite_tile_prompt_last_timestamp"
        private const val MAX_PROMPTS = 4
    }

    @Before
    fun setUp() {
        context = spy(RuntimeEnvironment.getApplication())
        satelliteTilePromptUtils = SatelliteTilePromptUtils()

        // Mock SharedPreferences
        doReturn(mockSharedPreferences).`when`(context).getSharedPreferences(anyString(), anyInt())
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        whenever(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)

        // Mock NotificationManager
        doReturn(mockNotificationManager)
            .`when`(context)
            .getSystemService(NotificationManager::class.java)
    }

    @Test
    fun showSatelliteTileAvailableNotification_createsNotificationChannelAndNotifies() {
        satelliteTilePromptUtils.showSatelliteTileAvailableNotification(context)

        verifyNotificationChannelCreated()
        verifyNotificationSentWithIntents()
    }

    @Test
    fun showSatelliteTileAvailableNotification_logsImpression() {
        satelliteTilePromptUtils.showSatelliteTileAvailableNotification(context)

        verify(metricsRule.metricsFeatureProvider)
            .action(context, SettingsEnums.ACTION_SATELLITE_NOTIFICATION_SHOWN)
    }

    @Test
    fun areAllPromptsShown_lessThanMax_returnsFalse() {
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(MAX_PROMPTS - 1)

        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isFalse()
    }

    @Test
    fun areAllPromptsShown_equalToMax_returnsTrue() {
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(MAX_PROMPTS)

        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun areAllPromptsShown_moreThanMax_returnsTrue() {
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(MAX_PROMPTS + 1)

        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun markAllPromptsShown_setsCountToMax() {
        satelliteTilePromptUtils.markAllPromptsShown(context)

        verify(mockEditor).putInt(PREF_PROMPT_COUNT, MAX_PROMPTS)
        verify(mockEditor).apply()
    }

    @Test
    fun recordPromptShown_incrementsCountAndTimestamp() {
        val initialCount = 1
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(initialCount)
        val beforeTimestamp = System.currentTimeMillis()

        satelliteTilePromptUtils.recordPromptShown(context)

        val afterTimestamp = System.currentTimeMillis()
        val timestampCaptor = ArgumentCaptor.forClass(Long::class.java)
        verify(mockEditor).putInt(PREF_PROMPT_COUNT, initialCount + 1)
        verify(mockEditor).putLong(eq(PREF_LAST_PROMPT_TIMESTAMP), timestampCaptor.capture())
        verify(mockEditor).apply()

        assertThat(timestampCaptor.value).isAtLeast(beforeTimestamp)
        assertThat(timestampCaptor.value).isAtMost(afterTimestamp)
    }

    @Test
    fun shouldShowSatelliteTilePrompt_count0_returnsTrue() {
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(0)

        assertThat(satelliteTilePromptUtils.shouldShowSatelliteTilePrompt(context)).isTrue()
    }

    @Test
    fun shouldShowSatelliteTilePrompt_countMax_returnsFalse() {
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(MAX_PROMPTS)

        assertThat(satelliteTilePromptUtils.shouldShowSatelliteTilePrompt(context)).isFalse()
    }

    @Test
    fun shouldShowSatelliteTilePrompt_intervalNotPassed_returnsFalse() {
        val now = System.currentTimeMillis()
        val interval = TimeUnit.HOURS.toMillis(24)
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(1)
        whenever(mockSharedPreferences.getLong(PREF_LAST_PROMPT_TIMESTAMP, 0L))
            .thenReturn(now - interval + 1000) // 1 second less than required

        assertThat(satelliteTilePromptUtils.shouldShowSatelliteTilePrompt(context)).isFalse()
    }

    @Test
    fun shouldShowSatelliteTilePrompt_intervalPassed_returnsTrue() {
        val now = System.currentTimeMillis()
        val interval = TimeUnit.HOURS.toMillis(24)
        whenever(mockSharedPreferences.getInt(PREF_PROMPT_COUNT, 0)).thenReturn(1)
        whenever(mockSharedPreferences.getLong(PREF_LAST_PROMPT_TIMESTAMP, 0L))
            .thenReturn(now - interval)

        assertThat(satelliteTilePromptUtils.shouldShowSatelliteTilePrompt(context)).isTrue()
    }

    private fun verifyNotificationChannelCreated() {
        val channelCaptor = ArgumentCaptor.forClass(NotificationChannel::class.java)
        verify(mockNotificationManager).createNotificationChannel(channelCaptor.capture())
        val channel = channelCaptor.value
        assertThat(channel.id).isEqualTo("satellite_tile_prompt_channel")
        assertThat(channel.name)
            .isEqualTo(context.getString(R.string.satellite_tile_prompt_channel_title))
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    }

    private fun verifyNotificationSentWithIntents() {
        val notificationCaptor = ArgumentCaptor.forClass(Notification::class.java)
        val notificationId = R.id.satellite_prompt_notification_id
        verify(mockNotificationManager).notify(eq(notificationId), notificationCaptor.capture())
        val notification = notificationCaptor.value

        with(notification) {
            assertThat(extras.getString(Notification.EXTRA_TITLE))
                .isEqualTo(context.getString(R.string.satellite_tile_prompt_notification_title))
            assertThat(extras.getString(Notification.EXTRA_TEXT))
                .isEqualTo(context.getString(R.string.satellite_tile_prompt_notification_summary))
            assertThat(smallIcon.resId).isEqualTo(R.drawable.ic_satellite_tile)
            assertThat(flags and Notification.FLAG_AUTO_CANCEL)
                .isEqualTo(Notification.FLAG_AUTO_CANCEL)
            assertThat(flags and Notification.FLAG_LOCAL_ONLY)
                .isEqualTo(Notification.FLAG_LOCAL_ONLY)

            assertPendingIntent(contentIntent)
            assertPendingIntent(actions[0].actionIntent)
            assertThat(actions[0].title).isEqualTo(context.getString(R.string.add_tile_action))
            assertThat(deleteIntent).isNull()
        }
    }

    private fun assertPendingIntent(pendingIntent: PendingIntent?) {
        assertThat(pendingIntent).isNotNull()
        val shadowIntent = Shadows.shadowOf(pendingIntent)
        assertThat(shadowIntent.savedIntent.component?.className)
            .isEqualTo(AddSatelliteTileActivity::class.java.name)
        assertThat(
                shadowIntent.savedIntent.getIntExtra(
                    AddSatelliteTileActivity.EXTRA_NOTIFICATION_ID,
                    0,
                )
            )
            .isEqualTo(R.id.satellite_prompt_notification_id)
        assertThat(shadowIntent.flags)
            .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
