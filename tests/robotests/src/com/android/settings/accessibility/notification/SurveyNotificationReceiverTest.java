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

import static com.android.settings.accessibility.notification.SurveyNotificationReceiver.getNotificationId;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_DISMISSED;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SURVEY_NOTIFICATION_SHOWN;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.server.accessibility.Flags;
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

/** Tests for {@link SurveyNotificationReceiver}. */
@RunWith(RobolectricTestRunner.class)
public class SurveyNotificationReceiverTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(RuntimeEnvironment.getApplication());
    private SurveyNotificationReceiver mReceiver;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        mReceiver = new SurveyNotificationReceiver();
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = shadowOf(notificationManager);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceive_createsNotificationChannel() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getNotificationChannels()).isNotEmpty();
        assertThat(mShadowNotificationManager.getNotificationChannels().getFirst().getId())
                .isEqualTo(SurveyNotificationReceiver.NOTIFICATION_CHANNEL_ID);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceive_disableHaTS_notCreatesNotificationChannel() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartCommand_withoutPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartCommand_withUnknownPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN);

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartCommand_withDarkUiSettingsPageId_createAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        mReceiver.onReceive(mContext, intent);

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
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartCommand_disableHaTS_withDarkUiSettingsPageId_notCreateAndPostNotification() {
        final Intent intent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        mReceiver.onReceive(mContext, intent);

        assertThat(mShadowNotificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartCommand_withDarkUiSettingsPageIdAndDismissExtraTrue_cancelsNotification() {
        // Post a notification first to ensure there's one to cancel
        int notificationId = getNotificationId(SettingsEnums.DARK_UI_SETTINGS);
        final Intent postIntent = new Intent(ACTION_SURVEY_NOTIFICATION_SHOWN)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);
        mReceiver.onReceive(mContext, postIntent);
        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNotNull();
        // Now send the dismiss intent
        Intent dismissIntent = new Intent(ACTION_SURVEY_NOTIFICATION_DISMISSED)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        mReceiver.onReceive(mContext, dismissIntent);

        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNull();
    }

    @Test
    public void getNotificationId_returnsCorrectId() {
        int pageId = 123;
        int expectedNotificationId = 751131 + pageId;

        assertThat(SurveyNotificationReceiver.getNotificationId(pageId))
                .isEqualTo(expectedNotificationId);
    }
}
