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

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE;
import static com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;

/** Tests for {@link NotificationHelper}. */
@RunWith(RobolectricTestRunner.class)
public class NotificationHelperTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(RuntimeEnvironment.getApplication());
    private NotificationHelper mNotificationHelper;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        mNotificationHelper = new NotificationHelper(mContext);
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = shadowOf(notificationManager);
    }

    @Test
    public void constructor_createNotificationChannel() {
        assertThat(mShadowNotificationManager.getNotificationChannels()).isNotEmpty();
        assertThat(mShadowNotificationManager.getNotificationChannels().getFirst().getId())
                .isEqualTo(NotificationHelper.NOTIFICATION_CHANNEL_ID);
    }

    @Test
    public void handleSurveyNotification_withDarkUiSettingsPageId_createAndPostsNotification() {
        mNotificationHelper.handleSurveyNotification(SettingsEnums.DARK_UI_SETTINGS);

        int notificationId = mNotificationHelper.getNotificationId(SettingsEnums.DARK_UI_SETTINGS);
        Notification notification = mShadowNotificationManager.getNotification(notificationId);

        assertThat(notification).isNotNull();
        ShadowNotification shadowNotification = shadowOf(notification);
        assertThat(shadowNotification.getContentTitle().toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_title));
        assertThat(shadowNotification.getContentText().toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_summary));
        assertThat(notification.actions.length).isEqualTo(1);
        assertThat(notification.actions[0].title.toString()).isEqualTo(
                mContext.getString(R.string.dark_theme_survey_notification_action));

        // Verify content intent
        PendingIntent contentPendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowContentPendingIntent = shadowOf(contentPendingIntent);
        Intent contentIntent = shadowContentPendingIntent.getSavedIntent();
        assertThat(contentIntent.getAction()).isEqualTo(Settings.ACTION_DARK_THEME_SETTINGS);
        assertThat(contentIntent.getPackage()).isEqualTo(mContext.getPackageName());
        assertThat(contentIntent.getStringExtra(EXTRA_SOURCE)).isEqualTo(SOURCE_START_SURVEY);
        assertThat(contentIntent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Verify action intent
        PendingIntent actionPendingIntent = notification.actions[0].actionIntent;
        ShadowPendingIntent shadowActionPendingIntent = shadowOf(actionPendingIntent);
        Intent actionIntent = shadowActionPendingIntent.getSavedIntent();
        assertThat(actionIntent.getAction()).isEqualTo(Settings.ACTION_DARK_THEME_SETTINGS);
        assertThat(actionIntent.getPackage()).isEqualTo(mContext.getPackageName());
        assertThat(actionIntent.getStringExtra(EXTRA_SOURCE)).isEqualTo(SOURCE_START_SURVEY);
        assertThat(actionIntent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Test
    public void cancelNotification_withDarkUiSettingsPageId_cancelExpectedNotification() {
        mNotificationHelper.handleSurveyNotification(SettingsEnums.DARK_UI_SETTINGS);
        int notificationId = mNotificationHelper.getNotificationId(SettingsEnums.DARK_UI_SETTINGS);

        mNotificationHelper.cancelNotification(SettingsEnums.DARK_UI_SETTINGS);

        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNull();
    }

    @Test
    public void getNotificationId_returnCorrectId() {
        int pageId = 123;
        // NOTIFICATION_ID_BASE from NotificationHelper is 751131
        int expectedNotificationId = 751131 + pageId;

        assertThat(mNotificationHelper.getNotificationId(pageId)).isEqualTo(expectedNotificationId);
    }
}

