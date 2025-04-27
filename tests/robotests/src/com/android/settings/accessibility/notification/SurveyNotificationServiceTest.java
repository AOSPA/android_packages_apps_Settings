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

package com.android.settings.accessibility.notification;

import static android.app.Service.START_NOT_STICKY;

import static com.android.settings.accessibility.notification.NotificationConstants.EXTRA_DISMISS_NOTIFICATION;
import static com.android.settings.accessibility.notification.SurveyNotificationService.getNotificationId;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;

/** Tests for {@link SurveyNotificationService}. */
@RunWith(RobolectricTestRunner.class)
public class SurveyNotificationServiceTest {

    private final Context mContext = spy(RuntimeEnvironment.getApplication());
    private SurveyNotificationService mService;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        mService = Robolectric.setupService(SurveyNotificationService.class);
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = shadowOf(notificationManager);
    }

    @Test
    public void onCreate_createsNotificationChannel() {
        assertThat(mShadowNotificationManager.getNotificationChannels().size()).isEqualTo(1);
        assertThat(mShadowNotificationManager.getNotificationChannels().getFirst().getId())
                .isEqualTo(SurveyNotificationService.NOTIFICATION_CHANNEL_ID);
    }

    @Test
    public void onStartCommand_withNullIntent_notCreateAndPostNotification() {
        int result = mService.onStartCommand(null, 0, 0);

        assertThat(result).isEqualTo(START_NOT_STICKY);
        assertThat(mShadowNotificationManager.getAllNotifications().size()).isEqualTo(0);
    }

    @Test
    public void onStartCommand_withoutPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent();

        int result = mService.onStartCommand(intent, 0, 0);

        assertThat(result).isEqualTo(START_NOT_STICKY);
        assertThat(mShadowNotificationManager.getAllNotifications().size()).isEqualTo(0);
    }

    @Test
    public void onStartCommand_withUnknownPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent();
        intent.putExtra(NotificationConstants.EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN);

        int result = mService.onStartCommand(intent, 0, 0);

        assertThat(result).isEqualTo(Service.START_NOT_STICKY);
        assertThat(mShadowNotificationManager.getAllNotifications().size()).isEqualTo(0);
    }


    @Test
    public void onStartCommand_withNullNotificationManager_notCreateAndPostNotification() {
        // Create a new service instance without calling Robolectric.setupService() to avoid
        // initializing the NotificationManager.
        final SurveyNotificationService service = new SurveyNotificationService();
        final Intent intent = new Intent();
        intent.putExtra(NotificationConstants.EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        int result = service.onStartCommand(intent, 0, 0);

        assertThat(result).isEqualTo(Service.START_NOT_STICKY);
        assertThat(mShadowNotificationManager.getAllNotifications().size()).isEqualTo(0);
    }

    @Test
    public void onStartCommand_withDarkUiSettingsPageId_createAndPostNotification() {
        final Intent intent = new Intent();
        intent.putExtra(NotificationConstants.EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        int result = mService.onStartCommand(intent, 0, 0);

        assertThat(result).isEqualTo(Service.START_NOT_STICKY);
        Notification notification = mShadowNotificationManager.getNotification(
                getNotificationId(SettingsEnums.DARK_UI_SETTINGS));
        assertThat(notification).isNotNull();
        ShadowNotification shadowNotification = shadowOf(notification);
        assertThat(shadowNotification.getContentTitle().toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_title));
        assertThat(shadowNotification.getContentText().toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_summary));
        assertThat(notification.actions.length).isEqualTo(1);
        assertThat(notification.actions[0].title.toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_action));
        ShadowPendingIntent shadowPendingIntent = shadowOf(notification.actions[0].actionIntent);
        Intent pendingIntent = shadowPendingIntent.getSavedIntent();
        assertThat(pendingIntent.getAction()).isEqualTo(Settings.ACTION_DARK_THEME_SETTINGS);
    }

    @Test
    public void onStartCommand_withDarkUiSettingsPageIdAndDismissExtraTrue_cancelsNotification() {
        // Post a notification first to ensure there's one to cancel
        int notificationId = getNotificationId(SettingsEnums.DARK_UI_SETTINGS);
        final Intent postIntent = new Intent();
        postIntent.putExtra(NotificationConstants.EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);
        mService.onStartCommand(postIntent, 0, 0);
        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNotNull();
        // Now send the dismiss intent
        Intent dismissIntent = new Intent();
        dismissIntent.putExtra(NotificationConstants.EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);
        dismissIntent.putExtra(EXTRA_DISMISS_NOTIFICATION, true);

        int result = mService.onStartCommand(dismissIntent, 0, 0);

        assertThat(result).isEqualTo(START_NOT_STICKY);
        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNull();
    }

    @Test
    public void onBind_returnsNull() {
        final Intent intent = new Intent();
        assertThat(mService.onBind(intent)).isNull();
    }
}
