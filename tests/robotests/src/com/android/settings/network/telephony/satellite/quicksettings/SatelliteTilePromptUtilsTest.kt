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
import android.content.Context
import android.content.SharedPreferences
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
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

    private lateinit var context: Context
    @Mock private lateinit var mockSharedPreferences: SharedPreferences
    @Mock private lateinit var mockEditor: SharedPreferences.Editor
    @Mock private lateinit var mockNotificationManager: NotificationManager
    private lateinit var satelliteTilePromptUtils: SatelliteTilePromptUtils

    companion object {
        private const val PROMPT_SHOWN_KEY: String = "prompt_shown"
    }

    @Before
    fun setUp() {
        context = spy(RuntimeEnvironment.getApplication())
        satelliteTilePromptUtils = SatelliteTilePromptUtils()

        // Mock SharedPreferences
        doReturn(mockSharedPreferences).`when`(context).getSharedPreferences(anyString(), anyInt())
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)

        // Mock NotificationManager
        doReturn(mockNotificationManager)
            .`when`(context)
            .getSystemService(NotificationManager::class.java)
    }

    @Test
    fun hasAddTilePromptBeenShown_returnsFalseByDefault() {
        whenever(mockSharedPreferences.getBoolean(eq(PROMPT_SHOWN_KEY), eq(false)))
            .thenReturn(false)

        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isFalse()
    }

    @Test
    fun hasAddTilePromptBeenShown_returnsTrueWhenSet() {
        whenever(mockSharedPreferences.getBoolean(eq(PROMPT_SHOWN_KEY), eq(false))).thenReturn(true)

        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isTrue()
    }

    @Test
    fun setAddTilePromptShown_true_writesToSharedPreferences() {
        satelliteTilePromptUtils.setAddTilePromptShown(context, true)

        verify(mockEditor).putBoolean(eq(PROMPT_SHOWN_KEY), eq(true))
        verify(mockEditor).apply()
    }

    @Test
    fun setAddTilePromptShown_false_writesToSharedPreferences() {
        satelliteTilePromptUtils.setAddTilePromptShown(context, false)

        verify(mockEditor).putBoolean(eq(PROMPT_SHOWN_KEY), eq(false))
        verify(mockEditor).apply()
    }

    @Test
    fun showSatelliteTileAvailableNotification_createsNotificationChannelAndNotifies() {
        satelliteTilePromptUtils.showSatelliteTileAvailableNotification(context)

        verifyNotificationChannelCreated()
        verifyNotificationSent()
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

    private fun verifyNotificationSent() {
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

            assertPendingIntent(contentIntent)
            assertPendingIntent(actions[0].actionIntent)
            assertThat(actions[0].title).isEqualTo(context.getString(R.string.add_tile_action))
        }
    }

    private fun assertPendingIntent(pendingIntent: PendingIntent?) {
        assertThat(pendingIntent).isNotNull()
        val shadowIntent = Shadows.shadowOf(pendingIntent)
        assertThat(shadowIntent.savedIntent.component?.className)
            .isEqualTo(AddSatelliteTileActivity::class.java.name)
        assertThat(shadowIntent.flags)
            .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
